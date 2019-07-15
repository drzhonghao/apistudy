

import com.google.common.base.Throwables;
import io.netty.util.concurrent.FastThreadLocalThread;
import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.PartitionColumns;
import org.apache.cassandra.db.SerializationHeader;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.db.rows.EncodingStats;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.utils.JVMStabilityInspector;


class SSTableSimpleUnsortedWriter {
	private static final SSTableSimpleUnsortedWriter.Buffer SENTINEL = new SSTableSimpleUnsortedWriter.Buffer();

	private SSTableSimpleUnsortedWriter.Buffer buffer = new SSTableSimpleUnsortedWriter.Buffer();

	private final long bufferSize;

	private long currentSize;

	private final SerializationHeader header;

	private final BlockingQueue<SSTableSimpleUnsortedWriter.Buffer> writeQueue = new SynchronousQueue<SSTableSimpleUnsortedWriter.Buffer>();

	private final SSTableSimpleUnsortedWriter.DiskWriter diskWriter = new SSTableSimpleUnsortedWriter.DiskWriter();

	SSTableSimpleUnsortedWriter(File directory, CFMetaData metadata, PartitionColumns columns, long bufferSizeInMB) {
		this.bufferSize = (bufferSizeInMB * 1024L) * 1024L;
		this.header = new SerializationHeader(true, metadata, columns, EncodingStats.NO_STATS);
		diskWriter.start();
	}

	PartitionUpdate getUpdateFor(DecoratedKey key) {
		assert key != null;
		PartitionUpdate previous = buffer.get(key);
		if (previous == null) {
			previous = createPartitionUpdate(key);
			previous.allowNewUpdates();
			buffer.put(key, previous);
		}
		return previous;
	}

	private void countRow(Row row) {
	}

	private void maybeSync() throws SSTableSimpleUnsortedWriter.SyncException {
		try {
			if ((currentSize) > (bufferSize))
				sync();

		} catch (IOException e) {
			throw new SSTableSimpleUnsortedWriter.SyncException(e);
		}
	}

	private PartitionUpdate createPartitionUpdate(DecoratedKey key) {
		return null;
	}

	public void close() throws IOException {
		sync();
		put(SSTableSimpleUnsortedWriter.SENTINEL);
		try {
			diskWriter.join();
			checkForWriterException();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		checkForWriterException();
	}

	protected void sync() throws IOException {
		if (buffer.isEmpty())
			return;

		put(buffer);
		buffer = new SSTableSimpleUnsortedWriter.Buffer();
		currentSize = 0;
	}

	private void put(SSTableSimpleUnsortedWriter.Buffer buffer) throws IOException {
		while (true) {
			checkForWriterException();
			try {
				if (writeQueue.offer(buffer, 1, TimeUnit.SECONDS))
					break;

			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		} 
	}

	private void checkForWriterException() throws IOException {
		if ((diskWriter.exception) != null) {
			if ((diskWriter.exception) instanceof IOException)
				throw ((IOException) (diskWriter.exception));
			else
				throw Throwables.propagate(diskWriter.exception);

		}
	}

	static class SyncException extends RuntimeException {
		SyncException(IOException ioe) {
			super(ioe);
		}
	}

	static class Buffer extends TreeMap<DecoratedKey, PartitionUpdate> {}

	private class DiskWriter extends FastThreadLocalThread {
		volatile Throwable exception = null;

		public void run() {
			while (true) {
				try {
					SSTableSimpleUnsortedWriter.Buffer b = writeQueue.take();
					if (b == (SSTableSimpleUnsortedWriter.SENTINEL))
						return;

				} catch (Throwable e) {
					JVMStabilityInspector.inspectThrowable(e);
					if ((exception) == null)
						exception = e;

				}
			} 
		}
	}
}

