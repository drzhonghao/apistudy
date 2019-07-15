

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.File;
import java.io.IOException;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import org.apache.cassandra.concurrent.NamedThreadFactory;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.commitlog.CommitLog;
import org.apache.cassandra.db.commitlog.CommitLogArchiver;
import org.apache.cassandra.db.commitlog.CommitLogPosition;
import org.apache.cassandra.db.commitlog.CommitLogSegment;
import org.apache.cassandra.db.commitlog.SimpleCachedBufferPool;
import org.apache.cassandra.utils.JVMStabilityInspector;
import org.apache.cassandra.utils.Pair;
import org.apache.cassandra.utils.WrappedRunnable;
import org.apache.cassandra.utils.concurrent.OpOrder;
import org.apache.cassandra.utils.concurrent.WaitQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractCommitLogSegmentManager {
	static final Logger logger = LoggerFactory.getLogger(AbstractCommitLogSegmentManager.class);

	private volatile CommitLogSegment availableSegment = null;

	private final WaitQueue segmentPrepared = new WaitQueue();

	private final ConcurrentLinkedQueue<CommitLogSegment> activeSegments = new ConcurrentLinkedQueue<>();

	private volatile CommitLogSegment allocatingFrom = null;

	final String storageDirectory;

	private final AtomicLong size = new AtomicLong();

	private Thread managerThread;

	protected final CommitLog commitLog;

	private volatile boolean shutdown;

	private final BooleanSupplier managerThreadWaitCondition = () -> (((availableSegment) == null) && (!(atSegmentBufferLimit()))) || (shutdown);

	private final WaitQueue managerThreadWaitQueue = new WaitQueue();

	private static final SimpleCachedBufferPool bufferPool = new SimpleCachedBufferPool(DatabaseDescriptor.getCommitLogMaxCompressionBuffersInPool(), DatabaseDescriptor.getCommitLogSegmentSize());

	AbstractCommitLogSegmentManager(final CommitLog commitLog, String storageDirectory) {
		this.commitLog = commitLog;
		this.storageDirectory = storageDirectory;
	}

	void start() {
		Runnable runnable = new WrappedRunnable() {
			public void runMayThrow() throws Exception {
				while (!(shutdown)) {
					try {
						assert (availableSegment) == null;
						AbstractCommitLogSegmentManager.logger.debug("No segments in reserve; creating a fresh one");
						availableSegment = createSegment();
						if (shutdown) {
							discardAvailableSegment();
							return;
						}
						segmentPrepared.signalAll();
						Thread.yield();
						if (((availableSegment) == null) && (!(atSegmentBufferLimit())))
							continue;

						maybeFlushToReclaim();
					} catch (Throwable t) {
						JVMStabilityInspector.inspectThrowable(t);
						if (!(CommitLog.handleCommitError("Failed managing commit log segments", t)))
							return;

						Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
					}
					WaitQueue.waitOnCondition(managerThreadWaitCondition, managerThreadWaitQueue);
				} 
			}
		};
		shutdown = false;
		managerThread = NamedThreadFactory.createThread(runnable, "COMMIT-LOG-ALLOCATOR");
		managerThread.start();
		advanceAllocatingFrom(null);
	}

	private boolean atSegmentBufferLimit() {
		return false;
	}

	private void maybeFlushToReclaim() {
		long unused = unusedCapacity();
		if (unused < 0) {
			long flushingSize = 0;
			List<CommitLogSegment> segmentsToRecycle = new ArrayList<>();
			for (CommitLogSegment segment : activeSegments) {
				if (segment == (allocatingFrom))
					break;

				flushingSize += segment.onDiskSize();
				segmentsToRecycle.add(segment);
				if ((flushingSize + unused) >= 0)
					break;

			}
			flushDataFrom(segmentsToRecycle, false);
		}
	}

	abstract void handleReplayedSegment(final File file);

	abstract CommitLogSegment createSegment();

	abstract void discard(CommitLogSegment segment, boolean delete);

	@net.nicoulaj.compilecommand.annotations.DontInline
	void advanceAllocatingFrom(CommitLogSegment old) {
		while (true) {
			synchronized(this) {
				if ((allocatingFrom) != old)
					return;

				if ((availableSegment) != null) {
					activeSegments.add((allocatingFrom = availableSegment));
					availableSegment = null;
					break;
				}
			}
			awaitAvailableSegment(old);
		} 
		wakeManager();
		if (old != null) {
			commitLog.archiver.maybeArchive(old);
		}
		commitLog.requestExtraSync();
	}

	void awaitAvailableSegment(CommitLogSegment currentAllocatingFrom) {
		do {
			if (((availableSegment) == null) && ((allocatingFrom) == currentAllocatingFrom)) {
			}else {
			}
		} while (((availableSegment) == null) && ((allocatingFrom) == currentAllocatingFrom) );
	}

	void forceRecycleAll(Iterable<UUID> droppedCfs) {
		List<CommitLogSegment> segmentsToRecycle = new ArrayList<>(activeSegments);
		CommitLogSegment last = segmentsToRecycle.get(((segmentsToRecycle.size()) - 1));
		advanceAllocatingFrom(last);
		Keyspace.writeOrder.awaitNewBarrier();
		Future<?> future = flushDataFrom(segmentsToRecycle, true);
		try {
			future.get();
			for (CommitLogSegment segment : activeSegments)
				for (UUID cfId : droppedCfs)
					segment.markClean(cfId, CommitLogPosition.NONE, segment.getCurrentCommitLogPosition());


			for (CommitLogSegment segment : activeSegments) {
				if (segment.isUnused())
					archiveAndDiscard(segment);

			}
			CommitLogSegment first;
			if (((first = activeSegments.peek()) != null) && ((first.id) <= (last.id)))
				AbstractCommitLogSegmentManager.logger.error("Failed to force-recycle all segments; at least one segment is still in use with dirty CFs.");

		} catch (Throwable t) {
			AbstractCommitLogSegmentManager.logger.error("Failed waiting for a forced recycle of in-use commit log segments", t);
		}
	}

	void archiveAndDiscard(final CommitLogSegment segment) {
		boolean archiveSuccess = commitLog.archiver.maybeWaitForArchiving(segment.getName());
		if (!(activeSegments.remove(segment)))
			return;

		AbstractCommitLogSegmentManager.logger.debug("Segment {} is no longer active and will be deleted {}", segment, (archiveSuccess ? "now" : "by the archive script"));
		discard(segment, archiveSuccess);
	}

	void addSize(long addedSize) {
		size.addAndGet(addedSize);
	}

	public long onDiskSize() {
		return size.get();
	}

	private long unusedCapacity() {
		long total = ((DatabaseDescriptor.getTotalCommitlogSpaceInMB()) * 1024) * 1024;
		long currentSize = size.get();
		AbstractCommitLogSegmentManager.logger.trace("Total active commitlog segment space used is {} out of {}", currentSize, total);
		return total - currentSize;
	}

	private Future<?> flushDataFrom(List<CommitLogSegment> segments, boolean force) {
		if (segments.isEmpty())
			return Futures.immediateFuture(null);

		final CommitLogPosition maxCommitLogPosition = segments.get(((segments.size()) - 1)).getCurrentCommitLogPosition();
		final Map<UUID, ListenableFuture<?>> flushes = new LinkedHashMap<>();
		for (CommitLogSegment segment : segments) {
			for (UUID dirtyCFId : segment.getDirtyCFIDs()) {
				Pair<String, String> pair = Schema.instance.getCF(dirtyCFId);
				if (pair == null) {
					AbstractCommitLogSegmentManager.logger.trace("Marking clean CF {} that doesn't exist anymore", dirtyCFId);
					segment.markClean(dirtyCFId, CommitLogPosition.NONE, segment.getCurrentCommitLogPosition());
				}else
					if (!(flushes.containsKey(dirtyCFId))) {
						String keyspace = pair.left;
						final ColumnFamilyStore cfs = Keyspace.open(keyspace).getColumnFamilyStore(dirtyCFId);
						flushes.put(dirtyCFId, (force ? cfs.forceFlush() : cfs.forceFlush(maxCommitLogPosition)));
					}

			}
		}
		return Futures.allAsList(flushes.values());
	}

	public void stopUnsafe(boolean deleteSegments) {
		AbstractCommitLogSegmentManager.logger.debug("CLSM closing and clearing existing commit log segments...");
		shutdown();
		try {
			awaitTermination();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		for (CommitLogSegment segment : activeSegments)
			closeAndDeleteSegmentUnsafe(segment, deleteSegments);

		activeSegments.clear();
		size.set(0L);
		AbstractCommitLogSegmentManager.logger.trace("CLSM done with closing and clearing existing commit log segments.");
	}

	void awaitManagementTasksCompletion() {
		if (((availableSegment) == null) && (!(atSegmentBufferLimit()))) {
			awaitAvailableSegment(allocatingFrom);
		}
	}

	private void closeAndDeleteSegmentUnsafe(CommitLogSegment segment, boolean delete) {
		try {
			discard(segment, delete);
		} catch (AssertionError ignored) {
		}
	}

	public void shutdown() {
		assert !(shutdown);
		shutdown = true;
		discardAvailableSegment();
		wakeManager();
	}

	private void discardAvailableSegment() {
		CommitLogSegment next = null;
		synchronized(this) {
			next = availableSegment;
			availableSegment = null;
		}
	}

	public void awaitTermination() throws InterruptedException {
		managerThread.join();
		managerThread = null;
		for (CommitLogSegment segment : activeSegments) {
		}
		AbstractCommitLogSegmentManager.bufferPool.shutdown();
	}

	@com.google.common.annotations.VisibleForTesting
	public Collection<CommitLogSegment> getActiveSegments() {
		return Collections.unmodifiableCollection(activeSegments);
	}

	CommitLogPosition getCurrentPosition() {
		return allocatingFrom.getCurrentCommitLogPosition();
	}

	public void sync(boolean flush) throws IOException {
		CommitLogSegment current = allocatingFrom;
		for (CommitLogSegment segment : getActiveSegments()) {
			if ((segment.id) > (current.id))
				return;

		}
	}

	SimpleCachedBufferPool getBufferPool() {
		return AbstractCommitLogSegmentManager.bufferPool;
	}

	void wakeManager() {
		managerThreadWaitQueue.signalAll();
	}

	void notifyBufferFreed() {
		wakeManager();
	}

	CommitLogSegment allocatingFrom() {
		return allocatingFrom;
	}
}

