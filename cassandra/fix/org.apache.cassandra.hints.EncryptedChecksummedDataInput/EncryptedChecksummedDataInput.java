

import io.netty.util.concurrent.FastThreadLocal;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import javax.crypto.Cipher;
import org.apache.cassandra.hints.ChecksummedDataInput;
import org.apache.cassandra.hints.InputPosition;
import org.apache.cassandra.io.FSReadError;
import org.apache.cassandra.io.compress.ICompressor;
import org.apache.cassandra.io.util.ChannelProxy;
import org.apache.cassandra.io.util.RebufferingInputStream;
import org.apache.cassandra.security.EncryptionUtils;


public class EncryptedChecksummedDataInput extends ChecksummedDataInput {
	private static final FastThreadLocal<ByteBuffer> reusableBuffers = new FastThreadLocal<ByteBuffer>() {
		protected ByteBuffer initialValue() {
			return ByteBuffer.allocate(0);
		}
	};

	private final Cipher cipher;

	private final ICompressor compressor;

	private final EncryptionUtils.ChannelProxyReadChannel readChannel;

	private long sourcePosition;

	public boolean isEOF() {
		return ((readChannel.getCurrentPosition()) == (channel.size())) && ((buffer.remaining()) == 0);
	}

	public long getSourcePosition() {
		return sourcePosition;
	}

	static class Position {
		final long bufferStart;

		final int bufferPosition;

		public Position(long sourcePosition, long bufferStart, int bufferPosition) {
			this.bufferStart = bufferStart;
			this.bufferPosition = bufferPosition;
		}

		public long subtract(InputPosition o) {
			EncryptedChecksummedDataInput.Position other = ((EncryptedChecksummedDataInput.Position) (o));
			return (((bufferStart) - (other.bufferStart)) + (bufferPosition)) - (other.bufferPosition);
		}
	}

	public InputPosition getSeekPosition() {
		return null;
	}

	public void seek(InputPosition p) {
		EncryptedChecksummedDataInput.Position pos = ((EncryptedChecksummedDataInput.Position) (p));
		bufferOffset = pos.bufferStart;
		buffer.position(0).limit(0);
		resetCrc();
		reBuffer();
		buffer.position(pos.bufferPosition);
		assert (bufferOffset) == (pos.bufferStart);
		assert (buffer.position()) == (pos.bufferPosition);
	}

	@Override
	protected void readBuffer() {
		this.sourcePosition = readChannel.getCurrentPosition();
		if (isEOF())
			return;

		try {
			ByteBuffer byteBuffer = EncryptedChecksummedDataInput.reusableBuffers.get();
			ByteBuffer decrypted = EncryptionUtils.decrypt(readChannel, byteBuffer, true, cipher);
			buffer = EncryptionUtils.uncompress(decrypted, buffer, true, compressor);
			if ((decrypted.capacity()) > (byteBuffer.capacity()))
				EncryptedChecksummedDataInput.reusableBuffers.set(decrypted);

		} catch (IOException ioe) {
			throw new FSReadError(ioe, getPath());
		}
	}

	@SuppressWarnings("resource")
	public static ChecksummedDataInput upgradeInput(ChecksummedDataInput input, Cipher cipher, ICompressor compressor) {
		input.close();
		return null;
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

