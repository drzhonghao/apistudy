

import java.io.File;
import java.io.IOException;
import org.apache.cassandra.io.util.ChannelProxy;
import org.apache.cassandra.io.util.DataIntegrityMetadata;
import org.apache.cassandra.io.util.RandomAccessReader;
import org.apache.cassandra.utils.ChecksumType;
import org.apache.cassandra.utils.concurrent.SharedCloseableImpl;


public final class ChecksummedRandomAccessReader {
	@SuppressWarnings("resource")
	public static RandomAccessReader open(File file, File crcFile) throws IOException {
		ChannelProxy channel = new ChannelProxy(file);
		try {
			DataIntegrityMetadata.ChecksumValidator validator = new DataIntegrityMetadata.ChecksumValidator(ChecksumType.CRC32, RandomAccessReader.open(crcFile), file.getPath());
		} catch (Throwable t) {
			channel.close();
			throw t;
		}
		return null;
	}
}

