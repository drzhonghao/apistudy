

import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import org.apache.lucene.codecs.compressing.Compressor;
import org.apache.lucene.codecs.compressing.Decompressor;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;


public abstract class CompressionMode {
	public static final CompressionMode FAST = null;

	public static final CompressionMode HIGH_COMPRESSION = null;

	public static final CompressionMode FAST_DECOMPRESSION = null;

	protected CompressionMode() {
	}

	public abstract Compressor newCompressor();

	public abstract Decompressor newDecompressor();

	private static final Decompressor LZ4_DECOMPRESSOR = null;

	private static final class LZ4FastCompressor extends Compressor {
		LZ4FastCompressor() {
		}

		@Override
		public void compress(byte[] bytes, int off, int len, DataOutput out) throws IOException {
		}

		@Override
		public void close() throws IOException {
		}
	}

	private static final class LZ4HighCompressor extends Compressor {
		LZ4HighCompressor() {
		}

		@Override
		public void compress(byte[] bytes, int off, int len, DataOutput out) throws IOException {
		}

		@Override
		public void close() throws IOException {
		}
	}

	private static final class DeflateDecompressor extends Decompressor {
		byte[] compressed;

		DeflateDecompressor() {
			compressed = new byte[0];
		}

		@Override
		public void decompress(DataInput in, int originalLength, int offset, int length, BytesRef bytes) throws IOException {
			assert (offset + length) <= originalLength;
			if (length == 0) {
				bytes.length = 0;
				return;
			}
			final int compressedLength = in.readVInt();
			final int paddedLength = compressedLength + 1;
			compressed = ArrayUtil.grow(compressed, paddedLength);
			in.readBytes(compressed, 0, compressedLength);
			compressed[compressedLength] = 0;
			final Inflater decompressor = new Inflater(true);
			try {
				decompressor.setInput(compressed, 0, paddedLength);
				bytes.offset = bytes.length = 0;
				bytes.bytes = ArrayUtil.grow(bytes.bytes, originalLength);
				try {
					bytes.length = decompressor.inflate(bytes.bytes, bytes.length, originalLength);
				} catch (DataFormatException e) {
					throw new IOException(e);
				}
				if (!(decompressor.finished())) {
					throw new CorruptIndexException(((("Invalid decoder state: needsInput=" + (decompressor.needsInput())) + ", needsDict=") + (decompressor.needsDictionary())), in);
				}
			} finally {
				decompressor.end();
			}
			if ((bytes.length) != originalLength) {
				throw new CorruptIndexException(((("Lengths mismatch: " + (bytes.length)) + " != ") + originalLength), in);
			}
			bytes.offset = offset;
			bytes.length = length;
		}

		@Override
		public Decompressor clone() {
			return new CompressionMode.DeflateDecompressor();
		}
	}

	private static class DeflateCompressor extends Compressor {
		final Deflater compressor;

		byte[] compressed;

		boolean closed;

		DeflateCompressor(int level) {
			compressor = new Deflater(level, true);
			compressed = new byte[64];
		}

		@Override
		public void compress(byte[] bytes, int off, int len, DataOutput out) throws IOException {
			compressor.reset();
			compressor.setInput(bytes, off, len);
			compressor.finish();
			if (compressor.needsInput()) {
				assert len == 0 : len;
				out.writeVInt(0);
				return;
			}
			int totalCount = 0;
			for (; ;) {
				final int count = compressor.deflate(compressed, totalCount, ((compressed.length) - totalCount));
				totalCount += count;
				assert totalCount <= (compressed.length);
				if (compressor.finished()) {
					break;
				}else {
					compressed = ArrayUtil.grow(compressed);
				}
			}
			out.writeVInt(totalCount);
			out.writeBytes(compressed, totalCount);
		}

		@Override
		public void close() throws IOException {
			if ((closed) == false) {
				compressor.end();
				closed = true;
			}
		}
	}
}

