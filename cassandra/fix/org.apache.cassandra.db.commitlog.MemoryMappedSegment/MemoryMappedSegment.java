

import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.commitlog.CommitLog;
import org.apache.cassandra.db.commitlog.CommitLogSegment;
import org.apache.cassandra.io.FSWriteError;
import org.apache.cassandra.io.util.FileUtils;


public class MemoryMappedSegment extends CommitLogSegment {
	ByteBuffer createBuffer(CommitLog commitLog) {
		try {
		} catch (IOException e) {
		}
	}

	void write(int startMarker, int nextMarker) {
	}

	protected void flush(int startMarker, int nextMarker) {
		try {
		} catch (Exception e) {
			throw new FSWriteError(e, getPath());
		}
	}

	@Override
	public long onDiskSize() {
		return DatabaseDescriptor.getCommitLogSegmentSize();
	}

	@Override
	protected void internalClose() {
		if (FileUtils.isCleanerAvailable) {
		}
		super.internalClose();
	}
}

