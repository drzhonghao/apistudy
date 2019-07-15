

import com.codahale.metrics.Timer;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.zip.CRC32;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.commitlog.AbstractCommitLogSegmentManager;
import org.apache.cassandra.db.commitlog.CommitLog;
import org.apache.cassandra.db.commitlog.CommitLogDescriptor;
import org.apache.cassandra.db.commitlog.CommitLogPosition;
import org.apache.cassandra.db.partitions.AbstractBTreePartition;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.io.FSWriteError;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.IntegerInterval;
import org.apache.cassandra.utils.IntegerInterval.Set;
import org.apache.cassandra.utils.NativeLibrary;
import org.apache.cassandra.utils.concurrent.OpOrder;
import org.apache.cassandra.utils.concurrent.WaitQueue;
import org.cliffc.high_scale_lib.NonBlockingHashMap;


public abstract class CommitLogSegment {
	private static final long idBase;

	private CommitLogSegment.CDCState cdcState = CommitLogSegment.CDCState.PERMITTED;

	public enum CDCState {

		PERMITTED,
		FORBIDDEN,
		CONTAINS;}

	Object cdcStateLock = new Object();

	private static final AtomicInteger nextId = new AtomicInteger(1);

	private static long replayLimitId;

	static {
		long maxId = Long.MIN_VALUE;
		for (File file : new File(DatabaseDescriptor.getCommitLogLocation()).listFiles()) {
			if (CommitLogDescriptor.isValid(file.getName()))
				maxId = Math.max(CommitLogDescriptor.fromFileName(file.getName()).id, maxId);

		}
		CommitLogSegment.replayLimitId = idBase = Math.max(System.currentTimeMillis(), (maxId + 1));
	}

	public static final int ENTRY_OVERHEAD_SIZE = (4 + 4) + 4;

	static final int SYNC_MARKER_SIZE = 4 + 4;

	private final OpOrder appendOrder = new OpOrder();

	private final AtomicInteger allocatePosition = new AtomicInteger();

	private volatile int lastSyncedOffset;

	private volatile int lastMarkerOffset;

	private int endOfBuffer;

	private final WaitQueue syncComplete = new WaitQueue();

	private final NonBlockingHashMap<UUID, IntegerInterval> cfDirty = new NonBlockingHashMap<>(1024);

	private final ConcurrentHashMap<UUID, IntegerInterval.Set> cfClean = new ConcurrentHashMap<>();

	public final long id;

	final File logFile = null;

	final FileChannel channel;

	final int fd;

	protected final AbstractCommitLogSegmentManager manager;

	ByteBuffer buffer;

	private volatile boolean headerWritten;

	public final CommitLogDescriptor descriptor;

	static CommitLogSegment createSegment(CommitLog commitLog, AbstractCommitLogSegmentManager manager) {
		return null;
	}

	static boolean usesBufferPool(CommitLog commitLog) {
		return false;
	}

	static long getNextId() {
		return (CommitLogSegment.idBase) + (CommitLogSegment.nextId.getAndIncrement());
	}

	CommitLogSegment(CommitLog commitLog, AbstractCommitLogSegmentManager manager) {
		this.manager = manager;
		id = CommitLogSegment.getNextId();
		try {
			channel = FileChannel.open(logFile.toPath(), StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.CREATE);
			fd = NativeLibrary.getfd(channel);
		} catch (IOException e) {
			throw new FSWriteError(e, logFile);
		}
		buffer = createBuffer(commitLog);
		descriptor = null;
	}

	void writeLogHeader() {
		CommitLogDescriptor.writeHeader(buffer, descriptor, additionalHeaderParameters());
		endOfBuffer = buffer.capacity();
		lastSyncedOffset = lastMarkerOffset = buffer.position();
		allocatePosition.set(((lastSyncedOffset) + (CommitLogSegment.SYNC_MARKER_SIZE)));
		headerWritten = true;
	}

	protected Map<String, String> additionalHeaderParameters() {
		return Collections.<String, String>emptyMap();
	}

	abstract ByteBuffer createBuffer(CommitLog commitLog);

	@SuppressWarnings("resource")
	CommitLogSegment.Allocation allocate(Mutation mutation, int size) {
		final OpOrder.Group opGroup = appendOrder.start();
		try {
			int position = allocate(size);
			if (position < 0) {
				opGroup.close();
				return null;
			}
			markDirty(mutation, position);
			return new CommitLogSegment.Allocation(this, opGroup, position, ((ByteBuffer) (buffer.duplicate().position(position).limit((position + size)))));
		} catch (Throwable t) {
			opGroup.close();
			throw t;
		}
	}

	static boolean shouldReplay(String name) {
		return (CommitLogDescriptor.fromFileName(name).id) < (CommitLogSegment.replayLimitId);
	}

	static void resetReplayLimit() {
		CommitLogSegment.replayLimitId = CommitLogSegment.getNextId();
	}

	private int allocate(int size) {
		while (true) {
			int prev = allocatePosition.get();
			int next = prev + size;
			if (next >= (endOfBuffer))
				return -1;

			if (allocatePosition.compareAndSet(prev, next)) {
				assert (buffer) != null;
				return prev;
			}
		} 
	}

	void discardUnusedTail() {
		try (OpOrder.Group group = appendOrder.start()) {
			while (true) {
				int prev = allocatePosition.get();
				int next = (endOfBuffer) + 1;
				if (prev >= next) {
					assert ((buffer) == null) || (prev == ((buffer.capacity()) + 1));
					return;
				}
				if (allocatePosition.compareAndSet(prev, next)) {
					endOfBuffer = prev;
					assert ((buffer) != null) && (next == ((buffer.capacity()) + 1));
					return;
				}
			} 
		}
	}

	void waitForModifications() {
		appendOrder.awaitNewBarrier();
	}

	synchronized void sync(boolean flush) {
		if (!(headerWritten))
			throw new IllegalStateException("commit log header has not been written");

		assert (lastMarkerOffset) >= (lastSyncedOffset) : String.format("commit log segment positions are incorrect: last marked = %d, last synced = %d", lastMarkerOffset, lastSyncedOffset);
		final boolean needToMarkData = (allocatePosition.get()) > ((lastMarkerOffset) + (CommitLogSegment.SYNC_MARKER_SIZE));
		final boolean hasDataToFlush = (lastSyncedOffset) != (lastMarkerOffset);
		if (!(needToMarkData || hasDataToFlush))
			return;

		assert (buffer) != null;
		boolean close = false;
		int startMarker = lastMarkerOffset;
		int nextMarker;
		int sectionEnd;
		if (needToMarkData) {
			nextMarker = allocate(CommitLogSegment.SYNC_MARKER_SIZE);
			if (nextMarker < 0) {
				discardUnusedTail();
				close = true;
				nextMarker = buffer.capacity();
			}
			waitForModifications();
			sectionEnd = (close) ? endOfBuffer : nextMarker;
			write(startMarker, sectionEnd);
			lastMarkerOffset = sectionEnd;
		}else {
			nextMarker = lastMarkerOffset;
			sectionEnd = nextMarker;
		}
		if (flush || close) {
			flush(startMarker, sectionEnd);
			lastSyncedOffset = lastMarkerOffset = nextMarker;
		}
		if (close)
			internalClose();

		syncComplete.signalAll();
	}

	protected static void writeSyncMarker(long id, ByteBuffer buffer, int offset, int filePos, int nextMarker) {
		if (filePos > nextMarker)
			throw new IllegalArgumentException(String.format("commit log sync marker's current file position %d is greater than next file position %d", filePos, nextMarker));

		CRC32 crc = new CRC32();
		FBUtilities.updateChecksumInt(crc, ((int) (id & 4294967295L)));
		FBUtilities.updateChecksumInt(crc, ((int) (id >>> 32)));
		FBUtilities.updateChecksumInt(crc, filePos);
		buffer.putInt(offset, nextMarker);
		buffer.putInt((offset + 4), ((int) (crc.getValue())));
	}

	abstract void write(int lastSyncedOffset, int nextMarker);

	abstract void flush(int startMarker, int nextMarker);

	public boolean isStillAllocating() {
		return (allocatePosition.get()) < (endOfBuffer);
	}

	void discard(boolean deleteFile) {
		close();
		if (deleteFile)
			FileUtils.deleteWithConfirm(logFile);

	}

	public CommitLogPosition getCurrentCommitLogPosition() {
		return new CommitLogPosition(id, allocatePosition.get());
	}

	public String getPath() {
		return logFile.getPath();
	}

	public String getName() {
		return logFile.getName();
	}

	void waitForFinalSync() {
		while (true) {
			WaitQueue.Signal signal = syncComplete.register();
			if ((lastSyncedOffset) < (endOfBuffer)) {
				signal.awaitUninterruptibly();
			}else {
				signal.cancel();
				break;
			}
		} 
	}

	void waitForSync(int position, Timer waitingOnCommit) {
		while ((lastSyncedOffset) < position) {
			WaitQueue.Signal signal = (waitingOnCommit != null) ? syncComplete.register(waitingOnCommit.time()) : syncComplete.register();
			if ((lastSyncedOffset) < position)
				signal.awaitUninterruptibly();
			else
				signal.cancel();

		} 
	}

	synchronized void close() {
		discardUnusedTail();
		sync(true);
		assert (buffer) == null;
	}

	protected void internalClose() {
		try {
			channel.close();
			buffer = null;
		} catch (IOException e) {
			throw new FSWriteError(e, getPath());
		}
	}

	public static <K> void coverInMap(ConcurrentMap<K, IntegerInterval> map, K key, int value) {
		IntegerInterval i = map.get(key);
		if (i == null) {
			i = map.putIfAbsent(key, new IntegerInterval(value, value));
			if (i == null)
				return;

		}
		i.expandToCover(value);
	}

	void markDirty(Mutation mutation, int allocatedPosition) {
		for (PartitionUpdate update : mutation.getPartitionUpdates())
			CommitLogSegment.coverInMap(cfDirty, update.metadata().cfId, allocatedPosition);

	}

	public synchronized void markClean(UUID cfId, CommitLogPosition startPosition, CommitLogPosition endPosition) {
		if (((startPosition.segmentId) > (id)) || ((endPosition.segmentId) < (id)))
			return;

		if (!(cfDirty.containsKey(cfId)))
			return;

		int start = ((startPosition.segmentId) == (id)) ? startPosition.position : 0;
		int end = ((endPosition.segmentId) == (id)) ? endPosition.position : Integer.MAX_VALUE;
		cfClean.computeIfAbsent(cfId, ( k) -> new IntegerInterval.Set()).add(start, end);
		removeCleanFromDirty();
	}

	private void removeCleanFromDirty() {
		if (isStillAllocating())
			return;

		Iterator<Map.Entry<UUID, IntegerInterval.Set>> iter = cfClean.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<UUID, IntegerInterval.Set> clean = iter.next();
			UUID cfId = clean.getKey();
			IntegerInterval.Set cleanSet = clean.getValue();
			IntegerInterval dirtyInterval = cfDirty.get(cfId);
			if ((dirtyInterval != null) && (cleanSet.covers(dirtyInterval))) {
				cfDirty.remove(cfId);
				iter.remove();
			}
		} 
	}

	public synchronized Collection<UUID> getDirtyCFIDs() {
		if ((cfClean.isEmpty()) || (cfDirty.isEmpty()))
			return cfDirty.keySet();

		List<UUID> r = new ArrayList<>(cfDirty.size());
		for (Map.Entry<UUID, IntegerInterval> dirty : cfDirty.entrySet()) {
			UUID cfId = dirty.getKey();
			IntegerInterval dirtyInterval = dirty.getValue();
			IntegerInterval.Set cleanSet = cfClean.get(cfId);
			if ((cleanSet == null) || (!(cleanSet.covers(dirtyInterval))))
				r.add(dirty.getKey());

		}
		return r;
	}

	public synchronized boolean isUnused() {
		if (isStillAllocating())
			return false;

		removeCleanFromDirty();
		return cfDirty.isEmpty();
	}

	public boolean contains(CommitLogPosition context) {
		return (context.segmentId) == (id);
	}

	public String dirtyString() {
		StringBuilder sb = new StringBuilder();
		for (UUID cfId : getDirtyCFIDs()) {
			CFMetaData m = Schema.instance.getCFMetaData(cfId);
			sb.append((m == null ? "<deleted>" : m.cfName)).append(" (").append(cfId).append(", dirty: ").append(cfDirty.get(cfId)).append(", clean: ").append(cfClean.get(cfId)).append("), ");
		}
		return sb.toString();
	}

	public abstract long onDiskSize();

	public long contentSize() {
		return lastSyncedOffset;
	}

	@Override
	public String toString() {
		return ("CommitLogSegment(" + (getPath())) + ')';
	}

	public static class CommitLogSegmentFileComparator implements Comparator<File> {
		public int compare(File f, File f2) {
			CommitLogDescriptor desc = CommitLogDescriptor.fromFileName(f.getName());
			CommitLogDescriptor desc2 = CommitLogDescriptor.fromFileName(f2.getName());
			return Long.compare(desc.id, desc2.id);
		}
	}

	public CommitLogSegment.CDCState getCDCState() {
		return cdcState;
	}

	public void setCDCState(CommitLogSegment.CDCState newState) {
		if (newState == (cdcState))
			return;

		synchronized(cdcStateLock) {
			if (((cdcState) == (CommitLogSegment.CDCState.CONTAINS)) && (newState != (CommitLogSegment.CDCState.CONTAINS)))
				throw new IllegalArgumentException("Cannot transition from CONTAINS to any other state.");

			if (((cdcState) == (CommitLogSegment.CDCState.FORBIDDEN)) && (newState != (CommitLogSegment.CDCState.PERMITTED)))
				throw new IllegalArgumentException("Only transition from FORBIDDEN to PERMITTED is allowed.");

			cdcState = newState;
		}
	}

	protected static class Allocation {
		private final CommitLogSegment segment;

		private final OpOrder.Group appendOp;

		private final int position;

		private final ByteBuffer buffer;

		Allocation(CommitLogSegment segment, OpOrder.Group appendOp, int position, ByteBuffer buffer) {
			this.segment = segment;
			this.appendOp = appendOp;
			this.position = position;
			this.buffer = buffer;
		}

		CommitLogSegment getSegment() {
			return segment;
		}

		ByteBuffer getBuffer() {
			return buffer;
		}

		void markWritten() {
			appendOp.close();
		}

		void awaitDiskSync(Timer waitingOnCommit) {
			segment.waitForSync(position, waitingOnCommit);
		}

		public CommitLogPosition getCommitLogPosition() {
			return new CommitLogPosition(segment.id, buffer.limit());
		}
	}
}

