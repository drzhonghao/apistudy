

import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.StoredFieldsReader;
import org.apache.lucene.codecs.compressing.CompressingStoredFieldsIndexReader;
import org.apache.lucene.codecs.compressing.CompressingStoredFieldsWriter;
import org.apache.lucene.codecs.compressing.CompressionMode;
import org.apache.lucene.codecs.compressing.Decompressor;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.StoredFieldVisitor;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.store.ChecksumIndexInput;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.Accountables;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BitUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.packed.PackedInts;

import static org.apache.lucene.util.packed.PackedInts.Format.PACKED;


public final class CompressingStoredFieldsReader extends StoredFieldsReader {
	private final int version;

	private final FieldInfos fieldInfos;

	private final CompressingStoredFieldsIndexReader indexReader;

	private final long maxPointer;

	private final IndexInput fieldsStream;

	private final int chunkSize;

	private final int packedIntsVersion;

	private final CompressionMode compressionMode;

	private final Decompressor decompressor;

	private final int numDocs;

	private final boolean merging;

	private final CompressingStoredFieldsReader.BlockState state;

	private final long numChunks;

	private final long numDirtyChunks;

	private boolean closed;

	private CompressingStoredFieldsReader(CompressingStoredFieldsReader reader, boolean merging) {
		this.version = reader.version;
		this.fieldInfos = reader.fieldInfos;
		this.fieldsStream = reader.fieldsStream.clone();
		this.indexReader = reader.indexReader.clone();
		this.maxPointer = reader.maxPointer;
		this.chunkSize = reader.chunkSize;
		this.packedIntsVersion = reader.packedIntsVersion;
		this.compressionMode = reader.compressionMode;
		this.decompressor = reader.decompressor.clone();
		this.numDocs = reader.numDocs;
		this.numChunks = reader.numChunks;
		this.numDirtyChunks = reader.numDirtyChunks;
		this.merging = merging;
		this.state = new CompressingStoredFieldsReader.BlockState();
		this.closed = false;
	}

	public CompressingStoredFieldsReader(Directory d, SegmentInfo si, String segmentSuffix, FieldInfos fn, IOContext context, String formatName, CompressionMode compressionMode) throws IOException {
		this.compressionMode = compressionMode;
		final String segment = si.name;
		boolean success = false;
		fieldInfos = fn;
		numDocs = si.maxDoc();
		int version = -1;
		long maxPointer = -1;
		CompressingStoredFieldsIndexReader indexReader = null;
		final String indexName = IndexFileNames.segmentFileName(segment, segmentSuffix, CompressingStoredFieldsWriter.FIELDS_INDEX_EXTENSION);
		try (ChecksumIndexInput indexStream = d.openChecksumInput(indexName, context)) {
			Throwable priorE = null;
			try {
				maxPointer = indexStream.readVLong();
			} catch (Throwable exception) {
				priorE = exception;
			} finally {
				CodecUtil.checkFooter(indexStream, priorE);
			}
		}
		this.version = version;
		this.maxPointer = maxPointer;
		this.indexReader = indexReader;
		final String fieldsStreamFN = IndexFileNames.segmentFileName(segment, segmentSuffix, CompressingStoredFieldsWriter.FIELDS_EXTENSION);
		try {
			fieldsStream = d.openInput(fieldsStreamFN, context);
			chunkSize = fieldsStream.readVInt();
			packedIntsVersion = fieldsStream.readVInt();
			decompressor = compressionMode.newDecompressor();
			this.merging = false;
			this.state = new CompressingStoredFieldsReader.BlockState();
			fieldsStream.seek(maxPointer);
			numChunks = fieldsStream.readVLong();
			numDirtyChunks = fieldsStream.readVLong();
			if ((numDirtyChunks) > (numChunks)) {
				throw new CorruptIndexException(((("invalid chunk counts: dirty=" + (numDirtyChunks)) + ", total=") + (numChunks)), fieldsStream);
			}
			CodecUtil.retrieveChecksum(fieldsStream);
			success = true;
		} finally {
			if (!success) {
				IOUtils.closeWhileHandlingException(this);
			}
		}
	}

	private void ensureOpen() throws AlreadyClosedException {
		if (closed) {
			throw new AlreadyClosedException("this FieldsReader is closed");
		}
	}

	@Override
	public void close() throws IOException {
		if (!(closed)) {
			IOUtils.close(fieldsStream);
			closed = true;
		}
	}

	private static void readField(DataInput in, StoredFieldVisitor visitor, FieldInfo info, int bits) throws IOException {
	}

	private static void skipField(DataInput in, int bits) throws IOException {
	}

	static float readZFloat(DataInput in) throws IOException {
		int b = (in.readByte()) & 255;
		if (b == 255) {
			return Float.intBitsToFloat(in.readInt());
		}else
			if ((b & 128) != 0) {
				return (b & 127) - 1;
			}else {
				int bits = ((b << 24) | (((in.readShort()) & 65535) << 8)) | ((in.readByte()) & 255);
				return Float.intBitsToFloat(bits);
			}

	}

	static double readZDouble(DataInput in) throws IOException {
		int b = (in.readByte()) & 255;
		if (b == 255) {
			return Double.longBitsToDouble(in.readLong());
		}else
			if (b == 254) {
				return Float.intBitsToFloat(in.readInt());
			}else
				if ((b & 128) != 0) {
					return (b & 127) - 1;
				}else {
					long bits = (((((long) (b)) << 56) | (((in.readInt()) & 4294967295L) << 24)) | (((in.readShort()) & 65535L) << 8)) | ((in.readByte()) & 255L);
					return Double.longBitsToDouble(bits);
				}


	}

	static long readTLong(DataInput in) throws IOException {
		int header = (in.readByte()) & 255;
		long bits = header & 31;
		if ((header & 32) != 0) {
			bits |= (in.readVLong()) << 5;
		}
		long l = BitUtil.zigZagDecode(bits);
		return l;
	}

	static class SerializedDocument {
		final DataInput in;

		final int length;

		final int numStoredFields;

		private SerializedDocument(DataInput in, int length, int numStoredFields) {
			this.in = in;
			this.length = length;
			this.numStoredFields = numStoredFields;
		}
	}

	private class BlockState {
		private int docBase;

		private int chunkDocs;

		private boolean sliced;

		private int[] offsets = IntsRef.EMPTY_INTS;

		private int[] numStoredFields = IntsRef.EMPTY_INTS;

		private long startPointer;

		private final BytesRef spare = new BytesRef();

		private final BytesRef bytes = new BytesRef();

		boolean contains(int docID) {
			return (docID >= (docBase)) && (docID < ((docBase) + (chunkDocs)));
		}

		void reset(int docID) throws IOException {
			boolean success = false;
			try {
				doReset(docID);
				success = true;
			} finally {
				if (success == false) {
					chunkDocs = 0;
				}
			}
		}

		private void doReset(int docID) throws IOException {
			docBase = fieldsStream.readVInt();
			final int token = fieldsStream.readVInt();
			chunkDocs = token >>> 1;
			if (((contains(docID)) == false) || (((docBase) + (chunkDocs)) > (numDocs))) {
				throw new CorruptIndexException(((((((("Corrupted: docID=" + docID) + ", docBase=") + (docBase)) + ", chunkDocs=") + (chunkDocs)) + ", numDocs=") + (numDocs)), fieldsStream);
			}
			sliced = (token & 1) != 0;
			offsets = ArrayUtil.grow(offsets, ((chunkDocs) + 1));
			numStoredFields = ArrayUtil.grow(numStoredFields, chunkDocs);
			if ((chunkDocs) == 1) {
				numStoredFields[0] = fieldsStream.readVInt();
				offsets[1] = fieldsStream.readVInt();
			}else {
				final int bitsPerStoredFields = fieldsStream.readVInt();
				if (bitsPerStoredFields == 0) {
					Arrays.fill(numStoredFields, 0, chunkDocs, fieldsStream.readVInt());
				}else
					if (bitsPerStoredFields > 31) {
						throw new CorruptIndexException(("bitsPerStoredFields=" + bitsPerStoredFields), fieldsStream);
					}else {
						final PackedInts.ReaderIterator it = PackedInts.getReaderIteratorNoHeader(fieldsStream, PACKED, packedIntsVersion, chunkDocs, bitsPerStoredFields, 1);
						for (int i = 0; i < (chunkDocs); ++i) {
							numStoredFields[i] = ((int) (it.next()));
						}
					}

				final int bitsPerLength = fieldsStream.readVInt();
				if (bitsPerLength == 0) {
					final int length = fieldsStream.readVInt();
					for (int i = 0; i < (chunkDocs); ++i) {
						offsets[(1 + i)] = (1 + i) * length;
					}
				}else
					if (bitsPerStoredFields > 31) {
						throw new CorruptIndexException(("bitsPerLength=" + bitsPerLength), fieldsStream);
					}else {
						final PackedInts.ReaderIterator it = PackedInts.getReaderIteratorNoHeader(fieldsStream, PACKED, packedIntsVersion, chunkDocs, bitsPerLength, 1);
						for (int i = 0; i < (chunkDocs); ++i) {
							offsets[(i + 1)] = ((int) (it.next()));
						}
						for (int i = 0; i < (chunkDocs); ++i) {
							offsets[(i + 1)] += offsets[i];
						}
					}

				for (int i = 0; i < (chunkDocs); ++i) {
					final int len = (offsets[(i + 1)]) - (offsets[i]);
					final int storedFields = numStoredFields[i];
					if ((len == 0) != (storedFields == 0)) {
						throw new CorruptIndexException(((("length=" + len) + ", numStoredFields=") + storedFields), fieldsStream);
					}
				}
			}
			startPointer = fieldsStream.getFilePointer();
			if (merging) {
				final int totalLength = offsets[chunkDocs];
				if (sliced) {
					bytes.offset = bytes.length = 0;
					for (int decompressed = 0; decompressed < totalLength;) {
						final int toDecompress = Math.min((totalLength - decompressed), chunkSize);
						decompressor.decompress(fieldsStream, toDecompress, 0, toDecompress, spare);
						bytes.bytes = ArrayUtil.grow(bytes.bytes, ((bytes.length) + (spare.length)));
						System.arraycopy(spare.bytes, spare.offset, bytes.bytes, bytes.length, spare.length);
						bytes.length += spare.length;
						decompressed += toDecompress;
					}
				}else {
					decompressor.decompress(fieldsStream, totalLength, 0, totalLength, bytes);
				}
				if ((bytes.length) != totalLength) {
					throw new CorruptIndexException(((("Corrupted: expected chunk size = " + totalLength) + ", got ") + (bytes.length)), fieldsStream);
				}
			}
		}

		CompressingStoredFieldsReader.SerializedDocument document(int docID) throws IOException {
			if ((contains(docID)) == false) {
				throw new IllegalArgumentException();
			}
			final int index = docID - (docBase);
			final int offset = offsets[index];
			final int length = (offsets[(index + 1)]) - offset;
			final int totalLength = offsets[chunkDocs];
			final int numStoredFields = this.numStoredFields[index];
			final DataInput documentInput;
			if (length == 0) {
				documentInput = new ByteArrayDataInput();
			}else
				if (merging) {
					documentInput = new ByteArrayDataInput(bytes.bytes, ((bytes.offset) + offset), length);
				}else
					if (sliced) {
						fieldsStream.seek(startPointer);
						decompressor.decompress(fieldsStream, chunkSize, offset, Math.min(length, ((chunkSize) - offset)), bytes);
						documentInput = new DataInput() {
							int decompressed = bytes.length;

							void fillBuffer() throws IOException {
								assert (decompressed) <= length;
								if ((decompressed) == length) {
									throw new EOFException();
								}
								final int toDecompress = Math.min((length - (decompressed)), chunkSize);
								decompressor.decompress(fieldsStream, toDecompress, 0, toDecompress, bytes);
								decompressed += toDecompress;
							}

							@Override
							public byte readByte() throws IOException {
								if ((bytes.length) == 0) {
									fillBuffer();
								}
								--(bytes.length);
								return bytes.bytes[((bytes.offset)++)];
							}

							@Override
							public void readBytes(byte[] b, int offset, int len) throws IOException {
								while (len > (bytes.length)) {
									System.arraycopy(bytes.bytes, bytes.offset, b, offset, bytes.length);
									len -= bytes.length;
									offset += bytes.length;
									fillBuffer();
								} 
								System.arraycopy(bytes.bytes, bytes.offset, b, offset, len);
								bytes.offset += len;
								bytes.length -= len;
							}
						};
					}else {
						fieldsStream.seek(startPointer);
						decompressor.decompress(fieldsStream, totalLength, offset, length, bytes);
						assert (bytes.length) == length;
						documentInput = new ByteArrayDataInput(bytes.bytes, bytes.offset, bytes.length);
					}


			return new CompressingStoredFieldsReader.SerializedDocument(documentInput, length, numStoredFields);
		}
	}

	CompressingStoredFieldsReader.SerializedDocument document(int docID) throws IOException {
		if ((state.contains(docID)) == false) {
			state.reset(docID);
		}
		assert state.contains(docID);
		return state.document(docID);
	}

	@Override
	public void visitDocument(int docID, StoredFieldVisitor visitor) throws IOException {
		final CompressingStoredFieldsReader.SerializedDocument doc = document(docID);
		for (int fieldIDX = 0; fieldIDX < (doc.numStoredFields); fieldIDX++) {
			final long infoAndBits = doc.in.readVLong();
		}
	}

	@Override
	public StoredFieldsReader clone() {
		ensureOpen();
		return new CompressingStoredFieldsReader(this, false);
	}

	@Override
	public StoredFieldsReader getMergeInstance() {
		ensureOpen();
		return new CompressingStoredFieldsReader(this, true);
	}

	int getVersion() {
		return version;
	}

	CompressionMode getCompressionMode() {
		return compressionMode;
	}

	CompressingStoredFieldsIndexReader getIndexReader() {
		return indexReader;
	}

	long getMaxPointer() {
		return maxPointer;
	}

	IndexInput getFieldsStream() {
		return fieldsStream;
	}

	int getChunkSize() {
		return chunkSize;
	}

	long getNumChunks() {
		return numChunks;
	}

	long getNumDirtyChunks() {
		return numDirtyChunks;
	}

	int getPackedIntsVersion() {
		return packedIntsVersion;
	}

	@Override
	public long ramBytesUsed() {
		return indexReader.ramBytesUsed();
	}

	@Override
	public Collection<Accountable> getChildResources() {
		return Collections.singleton(Accountables.namedAccountable("stored field index", indexReader));
	}

	@Override
	public void checkIntegrity() throws IOException {
		CodecUtil.checksumEntireFile(fieldsStream);
	}

	@Override
	public String toString() {
		return (((((getClass().getSimpleName()) + "(mode=") + (compressionMode)) + ",chunksize=") + (chunkSize)) + ")";
	}
}

