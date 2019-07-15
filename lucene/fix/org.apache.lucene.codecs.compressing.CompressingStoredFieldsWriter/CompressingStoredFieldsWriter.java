

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.StoredFieldsReader;
import org.apache.lucene.codecs.StoredFieldsWriter;
import org.apache.lucene.codecs.compressing.CompressingStoredFieldsIndexWriter;
import org.apache.lucene.codecs.compressing.CompressingStoredFieldsReader;
import org.apache.lucene.codecs.compressing.CompressionMode;
import org.apache.lucene.codecs.compressing.Compressor;
import org.apache.lucene.index.DocIDMerger;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MergeState;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.GrowableByteArrayDataOutput;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BitUtil;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.packed.PackedInts;

import static org.apache.lucene.util.packed.PackedInts.Format.PACKED;


public final class CompressingStoredFieldsWriter extends StoredFieldsWriter {
	public static final String FIELDS_EXTENSION = "fdt";

	public static final String FIELDS_INDEX_EXTENSION = "fdx";

	static final int STRING = 0;

	static final int BYTE_ARR = 1;

	static final int NUMERIC_INT = 2;

	static final int NUMERIC_FLOAT = 3;

	static final int NUMERIC_LONG = 4;

	static final int NUMERIC_DOUBLE = 5;

	static final int TYPE_BITS = PackedInts.bitsRequired(CompressingStoredFieldsWriter.NUMERIC_DOUBLE);

	static final int TYPE_MASK = ((int) (PackedInts.maxValue(CompressingStoredFieldsWriter.TYPE_BITS)));

	static final String CODEC_SFX_IDX = "Index";

	static final String CODEC_SFX_DAT = "Data";

	static final int VERSION_START = 1;

	static final int VERSION_CURRENT = CompressingStoredFieldsWriter.VERSION_START;

	private final String segment;

	private CompressingStoredFieldsIndexWriter indexWriter;

	private IndexOutput fieldsStream;

	private Compressor compressor;

	private final CompressionMode compressionMode;

	private final int chunkSize;

	private final int maxDocsPerChunk;

	private final GrowableByteArrayDataOutput bufferedDocs;

	private int[] numStoredFields;

	private int[] endOffsets;

	private int docBase;

	private int numBufferedDocs;

	private long numChunks;

	private long numDirtyChunks;

	public CompressingStoredFieldsWriter(Directory directory, SegmentInfo si, String segmentSuffix, IOContext context, String formatName, CompressionMode compressionMode, int chunkSize, int maxDocsPerChunk, int blockSize) throws IOException {
		assert directory != null;
		this.segment = si.name;
		this.compressionMode = compressionMode;
		this.compressor = compressionMode.newCompressor();
		this.chunkSize = chunkSize;
		this.maxDocsPerChunk = maxDocsPerChunk;
		this.docBase = 0;
		this.bufferedDocs = new GrowableByteArrayDataOutput(chunkSize);
		this.numStoredFields = new int[16];
		this.endOffsets = new int[16];
		this.numBufferedDocs = 0;
		boolean success = false;
		IndexOutput indexStream = directory.createOutput(IndexFileNames.segmentFileName(segment, segmentSuffix, CompressingStoredFieldsWriter.FIELDS_INDEX_EXTENSION), context);
		try {
			fieldsStream = directory.createOutput(IndexFileNames.segmentFileName(segment, segmentSuffix, CompressingStoredFieldsWriter.FIELDS_EXTENSION), context);
			final String codecNameIdx = formatName + (CompressingStoredFieldsWriter.CODEC_SFX_IDX);
			final String codecNameDat = formatName + (CompressingStoredFieldsWriter.CODEC_SFX_DAT);
			CodecUtil.writeIndexHeader(indexStream, codecNameIdx, CompressingStoredFieldsWriter.VERSION_CURRENT, si.getId(), segmentSuffix);
			CodecUtil.writeIndexHeader(fieldsStream, codecNameDat, CompressingStoredFieldsWriter.VERSION_CURRENT, si.getId(), segmentSuffix);
			assert (CodecUtil.indexHeaderLength(codecNameDat, segmentSuffix)) == (fieldsStream.getFilePointer());
			assert (CodecUtil.indexHeaderLength(codecNameIdx, segmentSuffix)) == (indexStream.getFilePointer());
			indexStream = null;
			fieldsStream.writeVInt(chunkSize);
			fieldsStream.writeVInt(PackedInts.VERSION_CURRENT);
			success = true;
		} finally {
			if (!success) {
				IOUtils.closeWhileHandlingException(fieldsStream, indexStream, indexWriter);
			}
		}
	}

	@Override
	public void close() throws IOException {
		try {
			IOUtils.close(fieldsStream, indexWriter, compressor);
		} finally {
			fieldsStream = null;
			indexWriter = null;
			compressor = null;
		}
	}

	private int numStoredFieldsInDoc;

	@Override
	public void startDocument() throws IOException {
	}

	@Override
	public void finishDocument() throws IOException {
		if ((numBufferedDocs) == (this.numStoredFields.length)) {
			final int newLength = ArrayUtil.oversize(((numBufferedDocs) + 1), 4);
			this.numStoredFields = Arrays.copyOf(this.numStoredFields, newLength);
			endOffsets = Arrays.copyOf(endOffsets, newLength);
		}
		this.numStoredFields[numBufferedDocs] = numStoredFieldsInDoc;
		numStoredFieldsInDoc = 0;
		endOffsets[numBufferedDocs] = bufferedDocs.getPosition();
		++(numBufferedDocs);
		if (triggerFlush()) {
			flush();
		}
	}

	private static void saveInts(int[] values, int length, DataOutput out) throws IOException {
		assert length > 0;
		if (length == 1) {
			out.writeVInt(values[0]);
		}else {
			boolean allEqual = true;
			for (int i = 1; i < length; ++i) {
				if ((values[i]) != (values[0])) {
					allEqual = false;
					break;
				}
			}
			if (allEqual) {
				out.writeVInt(0);
				out.writeVInt(values[0]);
			}else {
				long max = 0;
				for (int i = 0; i < length; ++i) {
					max |= values[i];
				}
				final int bitsRequired = PackedInts.bitsRequired(max);
				out.writeVInt(bitsRequired);
				final PackedInts.Writer w = PackedInts.getWriterNoHeader(out, PACKED, length, bitsRequired, 1);
				for (int i = 0; i < length; ++i) {
					w.add(values[i]);
				}
				w.finish();
			}
		}
	}

	private void writeHeader(int docBase, int numBufferedDocs, int[] numStoredFields, int[] lengths, boolean sliced) throws IOException {
		final int slicedBit = (sliced) ? 1 : 0;
		fieldsStream.writeVInt(docBase);
		fieldsStream.writeVInt(((numBufferedDocs << 1) | slicedBit));
		CompressingStoredFieldsWriter.saveInts(numStoredFields, numBufferedDocs, fieldsStream);
		CompressingStoredFieldsWriter.saveInts(lengths, numBufferedDocs, fieldsStream);
	}

	private boolean triggerFlush() {
		return ((bufferedDocs.getPosition()) >= (chunkSize)) || ((numBufferedDocs) >= (maxDocsPerChunk));
	}

	private void flush() throws IOException {
		final int[] lengths = endOffsets;
		for (int i = (numBufferedDocs) - 1; i > 0; --i) {
			lengths[i] = (endOffsets[i]) - (endOffsets[(i - 1)]);
			assert (lengths[i]) >= 0;
		}
		final boolean sliced = (bufferedDocs.getPosition()) >= (2 * (chunkSize));
		writeHeader(docBase, numBufferedDocs, numStoredFields, lengths, sliced);
		if (sliced) {
			for (int compressed = 0; compressed < (bufferedDocs.getPosition()); compressed += chunkSize) {
				compressor.compress(bufferedDocs.getBytes(), compressed, Math.min(chunkSize, ((bufferedDocs.getPosition()) - compressed)), fieldsStream);
			}
		}else {
			compressor.compress(bufferedDocs.getBytes(), 0, bufferedDocs.getPosition(), fieldsStream);
		}
		docBase += numBufferedDocs;
		numBufferedDocs = 0;
		bufferedDocs.reset();
		(numChunks)++;
	}

	@Override
	public void writeField(FieldInfo info, IndexableField field) throws IOException {
		++(numStoredFieldsInDoc);
		int bits = 0;
		final BytesRef bytes;
		final String string;
		Number number = field.numericValue();
		if (number != null) {
			if (((number instanceof Byte) || (number instanceof Short)) || (number instanceof Integer)) {
				bits = CompressingStoredFieldsWriter.NUMERIC_INT;
			}else
				if (number instanceof Long) {
					bits = CompressingStoredFieldsWriter.NUMERIC_LONG;
				}else
					if (number instanceof Float) {
						bits = CompressingStoredFieldsWriter.NUMERIC_FLOAT;
					}else
						if (number instanceof Double) {
							bits = CompressingStoredFieldsWriter.NUMERIC_DOUBLE;
						}else {
							throw new IllegalArgumentException(("cannot store numeric type " + (number.getClass())));
						}



			string = null;
			bytes = null;
		}else {
			bytes = field.binaryValue();
			if (bytes != null) {
				bits = CompressingStoredFieldsWriter.BYTE_ARR;
				string = null;
			}else {
				bits = CompressingStoredFieldsWriter.STRING;
				string = field.stringValue();
				if (string == null) {
					throw new IllegalArgumentException((("field " + (field.name())) + " is stored but does not have binaryValue, stringValue nor numericValue"));
				}
			}
		}
		final long infoAndBits = (((long) (info.number)) << (CompressingStoredFieldsWriter.TYPE_BITS)) | bits;
		bufferedDocs.writeVLong(infoAndBits);
		if (bytes != null) {
			bufferedDocs.writeVInt(bytes.length);
			bufferedDocs.writeBytes(bytes.bytes, bytes.offset, bytes.length);
		}else
			if (string != null) {
				bufferedDocs.writeString(string);
			}else {
				if (((number instanceof Byte) || (number instanceof Short)) || (number instanceof Integer)) {
					bufferedDocs.writeZInt(number.intValue());
				}else
					if (number instanceof Long) {
						CompressingStoredFieldsWriter.writeTLong(bufferedDocs, number.longValue());
					}else
						if (number instanceof Float) {
							CompressingStoredFieldsWriter.writeZFloat(bufferedDocs, number.floatValue());
						}else
							if (number instanceof Double) {
								CompressingStoredFieldsWriter.writeZDouble(bufferedDocs, number.doubleValue());
							}else {
								throw new AssertionError("Cannot get here");
							}



			}

	}

	static final int NEGATIVE_ZERO_FLOAT = Float.floatToIntBits((-0.0F));

	static final long NEGATIVE_ZERO_DOUBLE = Double.doubleToLongBits((-0.0));

	static final long SECOND = 1000L;

	static final long HOUR = (60 * 60) * (CompressingStoredFieldsWriter.SECOND);

	static final long DAY = 24 * (CompressingStoredFieldsWriter.HOUR);

	static final int SECOND_ENCODING = 64;

	static final int HOUR_ENCODING = 128;

	static final int DAY_ENCODING = 192;

	static void writeZFloat(DataOutput out, float f) throws IOException {
		int intVal = ((int) (f));
		final int floatBits = Float.floatToIntBits(f);
		if ((((f == intVal) && (intVal >= (-1))) && (intVal <= 125)) && (floatBits != (CompressingStoredFieldsWriter.NEGATIVE_ZERO_FLOAT))) {
			out.writeByte(((byte) (128 | (1 + intVal))));
		}else
			if ((floatBits >>> 31) == 0) {
				out.writeInt(floatBits);
			}else {
				out.writeByte(((byte) (255)));
				out.writeInt(floatBits);
			}

	}

	static void writeZDouble(DataOutput out, double d) throws IOException {
		int intVal = ((int) (d));
		final long doubleBits = Double.doubleToLongBits(d);
		if ((((d == intVal) && (intVal >= (-1))) && (intVal <= 124)) && (doubleBits != (CompressingStoredFieldsWriter.NEGATIVE_ZERO_DOUBLE))) {
			out.writeByte(((byte) (128 | (intVal + 1))));
			return;
		}else
			if (d == ((float) (d))) {
				out.writeByte(((byte) (254)));
				out.writeInt(Float.floatToIntBits(((float) (d))));
			}else
				if ((doubleBits >>> 63) == 0) {
					out.writeLong(doubleBits);
				}else {
					out.writeByte(((byte) (255)));
					out.writeLong(doubleBits);
				}


	}

	static void writeTLong(DataOutput out, long l) throws IOException {
		int header;
		if ((l % (CompressingStoredFieldsWriter.SECOND)) != 0) {
			header = 0;
		}else
			if ((l % (CompressingStoredFieldsWriter.DAY)) == 0) {
				header = CompressingStoredFieldsWriter.DAY_ENCODING;
				l /= CompressingStoredFieldsWriter.DAY;
			}else
				if ((l % (CompressingStoredFieldsWriter.HOUR)) == 0) {
					header = CompressingStoredFieldsWriter.HOUR_ENCODING;
					l /= CompressingStoredFieldsWriter.HOUR;
				}else {
					header = CompressingStoredFieldsWriter.SECOND_ENCODING;
					l /= CompressingStoredFieldsWriter.SECOND;
				}


		final long zigZagL = BitUtil.zigZagEncode(l);
		header |= zigZagL & 31;
		final long upperBits = zigZagL >>> 5;
		if (upperBits != 0) {
			header |= 32;
		}
		out.writeByte(((byte) (header)));
		if (upperBits != 0) {
			out.writeVLong(upperBits);
		}
	}

	@Override
	public void finish(FieldInfos fis, int numDocs) throws IOException {
		if ((numBufferedDocs) > 0) {
			flush();
			(numDirtyChunks)++;
		}else {
			assert (bufferedDocs.getPosition()) == 0;
		}
		if ((docBase) != numDocs) {
			throw new RuntimeException(((("Wrote " + (docBase)) + " docs, finish called with numDocs=") + numDocs));
		}
		fieldsStream.writeVLong(numChunks);
		fieldsStream.writeVLong(numDirtyChunks);
		CodecUtil.writeFooter(fieldsStream);
		assert (bufferedDocs.getPosition()) == 0;
	}

	static final String BULK_MERGE_ENABLED_SYSPROP = (CompressingStoredFieldsWriter.class.getName()) + ".enableBulkMerge";

	static final boolean BULK_MERGE_ENABLED;

	static {
		boolean v = true;
		try {
			v = Boolean.parseBoolean(System.getProperty(CompressingStoredFieldsWriter.BULK_MERGE_ENABLED_SYSPROP, "true"));
		} catch (SecurityException ignored) {
		}
		BULK_MERGE_ENABLED = v;
	}

	@Override
	public int merge(MergeState mergeState) throws IOException {
		int docCount = 0;
		int numReaders = mergeState.maxDocs.length;
		if (mergeState.needsIndexSort) {
			List<CompressingStoredFieldsWriter.CompressingStoredFieldsMergeSub> subs = new ArrayList<>();
			for (int i = 0; i < (mergeState.storedFieldsReaders.length); i++) {
			}
			final DocIDMerger<CompressingStoredFieldsWriter.CompressingStoredFieldsMergeSub> docIDMerger = DocIDMerger.of(subs, true);
			while (true) {
				CompressingStoredFieldsWriter.CompressingStoredFieldsMergeSub sub = docIDMerger.next();
				if (sub == null) {
					break;
				}
				assert (sub.mappedDocID) == docCount;
				startDocument();
				finishDocument();
				++docCount;
			} 
			finish(mergeState.mergeFieldInfos, docCount);
			return docCount;
		}
		for (int readerIndex = 0; readerIndex < numReaders; readerIndex++) {
			StoredFieldsWriter.MergeVisitor visitor = new StoredFieldsWriter.MergeVisitor(mergeState, readerIndex);
			CompressingStoredFieldsReader matchingFieldsReader = null;
			final int maxDoc = mergeState.maxDocs[readerIndex];
			final Bits liveDocs = mergeState.liveDocs[readerIndex];
		}
		finish(mergeState.mergeFieldInfos, docCount);
		return docCount;
	}

	boolean tooDirty(CompressingStoredFieldsReader candidate) {
		return false;
	}

	private static class CompressingStoredFieldsMergeSub extends DocIDMerger.Sub {
		private final CompressingStoredFieldsReader reader;

		private final int maxDoc;

		int docID = -1;

		public CompressingStoredFieldsMergeSub(CompressingStoredFieldsReader reader, MergeState.DocMap docMap, int maxDoc) {
			super(docMap);
			this.maxDoc = maxDoc;
			this.reader = reader;
		}

		@Override
		public int nextDoc() {
			(docID)++;
			if ((docID) == (maxDoc)) {
				return DocIdSetIterator.NO_MORE_DOCS;
			}else {
				return docID;
			}
		}
	}
}

