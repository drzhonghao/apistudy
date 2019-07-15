

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.cassandra.concurrent.NamedThreadFactory;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.io.sstable.SSTable;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.streaming.StreamHook;
import org.apache.cassandra.streaming.StreamSession;
import org.apache.cassandra.streaming.StreamTask;
import org.apache.cassandra.streaming.messages.FileMessageHeader;
import org.apache.cassandra.streaming.messages.OutgoingFileMessage;
import org.apache.cassandra.utils.Pair;
import org.apache.cassandra.utils.concurrent.Ref;


public class StreamTransferTask extends StreamTask {
	private static final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("StreamingTransferTaskTimeouts"));

	private final AtomicInteger sequenceNumber = new AtomicInteger(0);

	private boolean aborted = false;

	@VisibleForTesting
	protected final Map<Integer, OutgoingFileMessage> files = new HashMap<>();

	private final Map<Integer, ScheduledFuture> timeoutTasks = new HashMap<>();

	private long totalSize;

	public StreamTransferTask(StreamSession session, UUID cfId) {
		super(session, cfId);
	}

	public synchronized void addTransferFile(Ref<SSTableReader> ref, long estimatedKeys, List<Pair<Long, Long>> sections, long repairedAt) {
		assert ((ref.get()) != null) && (cfId.equals(ref.get().metadata.cfId));
		OutgoingFileMessage message = new OutgoingFileMessage(ref, sequenceNumber.getAndIncrement(), estimatedKeys, sections, repairedAt, session.keepSSTableLevel());
		message = StreamHook.instance.reportOutgoingFile(session, ref.get(), message);
		files.put(message.header.sequenceNumber, message);
		totalSize += message.header.size();
	}

	public void complete(int sequenceNumber) {
		boolean signalComplete;
		synchronized(this) {
			ScheduledFuture timeout = timeoutTasks.remove(sequenceNumber);
			if (timeout != null)
				timeout.cancel(false);

			OutgoingFileMessage file = files.remove(sequenceNumber);
			if (file != null)
				file.complete();

			signalComplete = files.isEmpty();
		}
	}

	public synchronized void abort() {
		if (aborted)
			return;

		aborted = true;
		for (ScheduledFuture future : timeoutTasks.values())
			future.cancel(false);

		timeoutTasks.clear();
		Throwable fail = null;
		for (OutgoingFileMessage file : files.values()) {
			try {
				file.complete();
			} catch (Throwable t) {
				if (fail == null)
					fail = t;
				else
					fail.addSuppressed(t);

			}
		}
		files.clear();
		if (fail != null)
			Throwables.propagate(fail);

	}

	public synchronized int getTotalNumberOfFiles() {
		return files.size();
	}

	public long getTotalSize() {
		return totalSize;
	}

	public synchronized Collection<OutgoingFileMessage> getFileMessages() {
		return new ArrayList<>(files.values());
	}

	public synchronized OutgoingFileMessage createMessageForRetry(int sequenceNumber) {
		ScheduledFuture future = timeoutTasks.remove(sequenceNumber);
		if (future != null)
			future.cancel(false);

		return files.get(sequenceNumber);
	}

	public synchronized ScheduledFuture scheduleTimeout(final int sequenceNumber, long time, TimeUnit unit) {
		if (!(files.containsKey(sequenceNumber)))
			return null;

		ScheduledFuture future = StreamTransferTask.timeoutExecutor.schedule(new Runnable() {
			public void run() {
				synchronized(StreamTransferTask.this) {
					timeoutTasks.remove(sequenceNumber);
					StreamTransferTask.this.complete(sequenceNumber);
				}
			}
		}, time, unit);
		ScheduledFuture prev = timeoutTasks.put(sequenceNumber, future);
		assert prev == null;
		return future;
	}
}

