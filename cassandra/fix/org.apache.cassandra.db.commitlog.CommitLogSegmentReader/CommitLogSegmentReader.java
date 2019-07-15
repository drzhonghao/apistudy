

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.AbstractIterator;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.zip.CRC32;
import javax.crypto.Cipher;
import org.apache.cassandra.config.ParameterizedClass;
import org.apache.cassandra.db.commitlog.CommitLogDescriptor;
import org.apache.cassandra.db.commitlog.CommitLogReadHandler;
import org.apache.cassandra.db.commitlog.EncryptedFileSegmentInputStream;
import org.apache.cassandra.io.FSReadError;
import org.apache.cassandra.io.compress.ICompressor;
import org.apache.cassandra.io.util.FileDataInput;
import org.apache.cassandra.io.util.FileSegmentInputStream;
import org.apache.cassandra.io.util.RandomAccessReader;
import org.apache.cassandra.io.util.RebufferingInputStream;
import org.apache.cassandra.schema.CompressionParams;
import org.apache.cassandra.security.EncryptionContext;
import org.apache.cassandra.security.EncryptionUtils;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.FBUtilities;


public class CommitLogSegmentReader implements Iterable<CommitLogSegmentReader.SyncSegment> {
	private final CommitLogReadHandler handler;

	private final CommitLogDescriptor descriptor;

	private final RandomAccessReader reader;

	private final CommitLogSegmentReader.Segmenter segmenter;

	private final boolean tolerateTruncation;

	protected int end;

	protected CommitLogSegmentReader(CommitLogReadHandler handler, CommitLogDescriptor descriptor, RandomAccessReader reader, boolean tolerateTruncation) {
		this.handler = handler;
		this.descriptor = descriptor;
		this.reader = reader;
		this.tolerateTruncation = tolerateTruncation;
		end = ((int) (reader.getFilePointer()));
		if (descriptor.getEncryptionContext().isEnabled())
			segmenter = new CommitLogSegmentReader.EncryptedSegmenter(descriptor, reader);
		else
			if ((descriptor.compression) != null)
				segmenter = new CommitLogSegmentReader.CompressedSegmenter(descriptor, reader);
			else
				segmenter = new CommitLogSegmentReader.NoOpSegmenter(reader);


	}

	public Iterator<CommitLogSegmentReader.SyncSegment> iterator() {
		return new CommitLogSegmentReader.SegmentIterator();
	}

	protected class SegmentIterator extends AbstractIterator<CommitLogSegmentReader.SyncSegment> {
		protected CommitLogSegmentReader.SyncSegment computeNext() {
			while (true) {
				try {
					final int currentStart = end;
					end = readSyncMarker(descriptor, currentStart, reader);
					if ((end) == (-1)) {
						return endOfData();
					}
					if ((end) > (reader.length())) {
						end = ((int) (reader.length()));
					}
				} catch (CommitLogSegmentReader.SegmentReadException e) {
				} catch (IOException e) {
					boolean tolerateErrorsInSection = (tolerateTruncation) & (segmenter.tolerateSegmentErrors(end, reader.length()));
				}
			} 
		}
	}

	private int readSyncMarker(CommitLogDescriptor descriptor, int offset, RandomAccessReader reader) throws IOException {
		reader.seek(offset);
		CRC32 crc = new CRC32();
		FBUtilities.updateChecksumInt(crc, ((int) ((descriptor.id) & 4294967295L)));
		FBUtilities.updateChecksumInt(crc, ((int) ((descriptor.id) >>> 32)));
		FBUtilities.updateChecksumInt(crc, ((int) (reader.getPosition())));
		final int end = reader.readInt();
		long filecrc = (reader.readInt()) & 4294967295L;
		if ((crc.getValue()) != filecrc) {
			if ((end != 0) || (filecrc != 0)) {
				String msg = String.format(("Encountered bad header at position %d of commit log %s, with invalid CRC. " + "The end of segment marker should be zero."), offset, reader.getPath());
				throw new CommitLogSegmentReader.SegmentReadException(msg, true);
			}
			return -1;
		}else
			if ((end < offset) || (end > (reader.length()))) {
				String msg = String.format("Encountered bad header at position %d of commit log %s, with bad position but valid CRC", offset, reader.getPath());
				throw new CommitLogSegmentReader.SegmentReadException(msg, false);
			}

		return end;
	}

	public static class SegmentReadException extends IOException {
		public final boolean invalidCrc;

		public SegmentReadException(String msg, boolean invalidCrc) {
			super(msg);
			this.invalidCrc = invalidCrc;
		}
	}

	public static class SyncSegment {
		public final FileDataInput input;

		public final int fileStartPosition;

		public final int fileEndPosition;

		public final int endPosition;

		public final boolean toleratesErrorsInSection;

		public SyncSegment(FileDataInput input, int fileStartPosition, int fileEndPosition, int endPosition, boolean toleratesErrorsInSection) {
			this.input = input;
			this.fileStartPosition = fileStartPosition;
			this.fileEndPosition = fileEndPosition;
			this.endPosition = endPosition;
			this.toleratesErrorsInSection = toleratesErrorsInSection;
		}
	}

	interface Segmenter {
		CommitLogSegmentReader.SyncSegment nextSegment(int startPosition, int nextSectionStartPosition) throws IOException;

		default boolean tolerateSegmentErrors(int segmentEndPosition, long fileLength) {
			return (segmentEndPosition >= fileLength) || (segmentEndPosition < 0);
		}
	}

	static class NoOpSegmenter implements CommitLogSegmentReader.Segmenter {
		private final RandomAccessReader reader;

		public NoOpSegmenter(RandomAccessReader reader) {
			this.reader = reader;
		}

		public CommitLogSegmentReader.SyncSegment nextSegment(int startPosition, int nextSectionStartPosition) {
			reader.seek(startPosition);
			return new CommitLogSegmentReader.SyncSegment(reader, startPosition, nextSectionStartPosition, nextSectionStartPosition, true);
		}

		public boolean tolerateSegmentErrors(int end, long length) {
			return true;
		}
	}

	static class CompressedSegmenter implements CommitLogSegmentReader.Segmenter {
		private final ICompressor compressor;

		private final RandomAccessReader reader;

		private byte[] compressedBuffer;

		private byte[] uncompressedBuffer;

		private long nextLogicalStart;

		public CompressedSegmenter(CommitLogDescriptor desc, RandomAccessReader reader) {
			this(CompressionParams.createCompressor(desc.compression), reader);
		}

		public CompressedSegmenter(ICompressor compressor, RandomAccessReader reader) {
			this.compressor = compressor;
			this.reader = reader;
			compressedBuffer = new byte[0];
			uncompressedBuffer = new byte[0];
			nextLogicalStart = reader.getFilePointer();
		}

		@SuppressWarnings("resource")
		public CommitLogSegmentReader.SyncSegment nextSegment(final int startPosition, final int nextSectionStartPosition) throws IOException {
			reader.seek(startPosition);
			int uncompressedLength = reader.readInt();
			int compressedLength = nextSectionStartPosition - ((int) (reader.getPosition()));
			if (compressedLength > (compressedBuffer.length))
				compressedBuffer = new byte[((int) (1.2 * compressedLength))];

			reader.readFully(compressedBuffer, 0, compressedLength);
			if (uncompressedLength > (uncompressedBuffer.length))
				uncompressedBuffer = new byte[((int) (1.2 * uncompressedLength))];

			int count = compressor.uncompress(compressedBuffer, 0, compressedLength, uncompressedBuffer, 0);
			FileDataInput input = new FileSegmentInputStream(ByteBuffer.wrap(uncompressedBuffer, 0, count), reader.getPath(), nextLogicalStart);
			nextLogicalStart += uncompressedLength;
			return new CommitLogSegmentReader.SyncSegment(input, startPosition, nextSectionStartPosition, ((int) (nextLogicalStart)), tolerateSegmentErrors(nextSectionStartPosition, reader.length()));
		}
	}

	static class EncryptedSegmenter implements CommitLogSegmentReader.Segmenter {
		private final RandomAccessReader reader;

		private final ICompressor compressor;

		private final Cipher cipher;

		private ByteBuffer decryptedBuffer;

		private ByteBuffer uncompressedBuffer;

		private final EncryptedFileSegmentInputStream.ChunkProvider chunkProvider;

		private long currentSegmentEndPosition;

		private long nextLogicalStart;

		public EncryptedSegmenter(CommitLogDescriptor descriptor, RandomAccessReader reader) {
			this(reader, descriptor.getEncryptionContext());
		}

		@VisibleForTesting
		EncryptedSegmenter(final RandomAccessReader reader, EncryptionContext encryptionContext) {
			this.reader = reader;
			decryptedBuffer = ByteBuffer.allocate(0);
			compressor = encryptionContext.getCompressor();
			nextLogicalStart = reader.getFilePointer();
			try {
				cipher = encryptionContext.getDecryptor();
			} catch (IOException ioe) {
				throw new FSReadError(ioe, reader.getPath());
			}
			chunkProvider = () -> {
				if ((reader.getFilePointer()) >= (currentSegmentEndPosition))
					return ByteBufferUtil.EMPTY_BYTE_BUFFER;

				try {
					decryptedBuffer = EncryptionUtils.decrypt(reader, decryptedBuffer, true, cipher);
					uncompressedBuffer = EncryptionUtils.uncompress(decryptedBuffer, uncompressedBuffer, true, compressor);
					return uncompressedBuffer;
				} catch (IOException e) {
					throw new FSReadError(e, reader.getPath());
				}
			};
		}

		@SuppressWarnings("resource")
		public CommitLogSegmentReader.SyncSegment nextSegment(int startPosition, int nextSectionStartPosition) throws IOException {
			int totalPlainTextLength = reader.readInt();
			currentSegmentEndPosition = nextSectionStartPosition - 1;
			FileDataInput input = new EncryptedFileSegmentInputStream(reader.getPath(), nextLogicalStart, 0, totalPlainTextLength, chunkProvider);
			nextLogicalStart += totalPlainTextLength;
			return new CommitLogSegmentReader.SyncSegment(input, startPosition, nextSectionStartPosition, ((int) (nextLogicalStart)), tolerateSegmentErrors(nextSectionStartPosition, reader.length()));
		}
	}
}

