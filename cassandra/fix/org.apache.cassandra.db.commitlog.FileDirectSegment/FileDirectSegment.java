

import org.apache.cassandra.db.commitlog.CommitLogSegment;
import org.apache.cassandra.io.FSWriteError;


public abstract class FileDirectSegment extends CommitLogSegment {
	volatile long lastWrittenPos = 0;

	void writeLogHeader() {
	}

	@Override
	protected void internalClose() {
		try {
			super.internalClose();
		} finally {
		}
	}

	protected void flush(int startMarker, int nextMarker) {
		try {
		} catch (Exception e) {
			throw new FSWriteError(e, getPath());
		}
	}
}

