

import java.nio.ByteBuffer;
import org.apache.cassandra.db.commitlog.CommitLog;
import org.apache.cassandra.db.commitlog.CommitLogSegment;
import org.apache.cassandra.db.commitlog.FileDirectSegment;
import org.apache.cassandra.io.FSWriteError;
import org.apache.cassandra.io.compress.ICompressor;


public class CompressedSegment extends FileDirectSegment {
	final ICompressor compressor;

	ByteBuffer createBuffer(CommitLog commitLog) {
	}

	void write(int startMarker, int nextMarker) {
		try {
		} catch (Exception e) {
			throw new FSWriteError(e, getPath());
		}
	}

	@Override
	public long onDiskSize() {
	}
}

