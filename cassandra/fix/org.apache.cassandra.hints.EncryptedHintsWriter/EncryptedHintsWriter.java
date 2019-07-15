

import java.io.IOException;
import java.nio.ByteBuffer;
import javax.crypto.Cipher;
import org.apache.cassandra.io.compress.ICompressor;
import org.apache.cassandra.security.EncryptionUtils;


public class EncryptedHintsWriter {
	private final Cipher cipher = null;

	private final ICompressor compressor = null;

	private volatile ByteBuffer byteBuffer;

	protected void writeBuffer(ByteBuffer input) throws IOException {
		byteBuffer = EncryptionUtils.compress(input, byteBuffer, true, compressor);
	}

	@com.google.common.annotations.VisibleForTesting
	Cipher getCipher() {
		return cipher;
	}

	@com.google.common.annotations.VisibleForTesting
	ICompressor getCompressor() {
		return compressor;
	}
}

