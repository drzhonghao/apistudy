

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.TermVectorsReader;
import org.apache.lucene.codecs.TermVectorsWriter;
import org.apache.lucene.codecs.compressing.CompressingStoredFieldsIndexWriter;
import org.apache.lucene.codecs.compressing.CompressingTermVectorsReader;
import org.apache.lucene.codecs.compressing.CompressionMode;
import org.apache.lucene.codecs.compressing.Compressor;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.MergeState;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.GrowableByteArrayDataOutput;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.StringHelper;
import org.apache.lucene.util.packed.BlockPackedWriter;
import org.apache.lucene.util.packed.PackedInts;

import static org.apache.lucene.util.packed.PackedInts.Format.PACKED;


public final class CompressingTermVectorsWriter extends TermVectorsWriter {
	static final int MAX_DOCUMENTS_PER_CHUNK = 128;

	static final String VECTORS_EXTENSION = "tvd";

	static final String VECTORS_INDEX_EXTENSION = "tvx";

	static final String CODEC_SFX_IDX = "Index";

	static final String CODEC_SFX_DAT = "Data";

	static final int VERSION_START = 1;

	static final int VERSION_CURRENT = CompressingTermVectorsWriter.VERSION_START;

	static final int PACKED_BLOCK_SIZE = 64;

	static final int POSITIONS = 1;

	static final int OFFSETS = 2;

	static final int PAYLOADS = 4;

	static final int FLAGS_BITS = PackedInts.bitsRequired((((CompressingTermVectorsWriter.POSITIONS) | (CompressingTermVectorsWriter.OFFSETS)) | (CompressingTermVectorsWriter.PAYLOADS)));

	private final String segment;

	private CompressingStoredFieldsIndexWriter indexWriter;

	private IndexOutput vectorsStream;

	private final CompressionMode compressionMode;

	private final Compressor compressor;

	private final int chunkSize;

	private long numChunks;

	private long numDirtyChunks;

	private class DocData {
		final int numFields;

		final Deque<CompressingTermVectorsWriter.FieldData> fields;

		final int posStart;

		final int offStart;

		final int payStart;

		DocData(int numFields, int posStart, int offStart, int payStart) {
			this.numFields = numFields;
			this.fields = new ArrayDeque<>(numFields);
			this.posStart = posStart;
			this.offStart = offStart;
			this.payStart = payStart;
		}

		CompressingTermVectorsWriter.FieldData addField(int fieldNum, int numTerms, boolean positions, boolean offsets, boolean payloads) {
			final CompressingTermVectorsWriter.FieldData field;
			if (fields.isEmpty()) {
				field = new CompressingTermVectorsWriter.FieldData(fieldNum, numTerms, positions, offsets, payloads, posStart, offStart, payStart);
			}else {
				final CompressingTermVectorsWriter.FieldData last = fields.getLast();
				final int posStart = (last.posStart) + (last.hasPositions ? last.totalPositions : 0);
				final int offStart = (last.offStart) + (last.hasOffsets ? last.totalPositions : 0);
				final int payStart = (last.payStart) + (last.hasPayloads ? last.totalPositions : 0);
				field = new CompressingTermVectorsWriter.FieldData(fieldNum, numTerms, positions, offsets, payloads, posStart, offStart, payStart);
			}
			fields.add(field);
			return field;
		}
	}

	private CompressingTermVectorsWriter.DocData addDocData(int numVectorFields) {
		CompressingTermVectorsWriter.FieldData last = null;
		for (Iterator<CompressingTermVectorsWriter.DocData> it = pendingDocs.descendingIterator(); it.hasNext();) {
			final CompressingTermVectorsWriter.DocData doc = it.next();
			if (!(doc.fields.isEmpty())) {
				last = doc.fields.getLast();
				break;
			}
		}
		final CompressingTermVectorsWriter.DocData doc;
		if (last == null) {
			doc = new CompressingTermVectorsWriter.DocData(numVectorFields, 0, 0, 0);
		}else {
			final int posStart = (last.posStart) + (last.hasPositions ? last.totalPositions : 0);
			final int offStart = (last.offStart) + (last.hasOffsets ? last.totalPositions : 0);
			final int payStart = (last.payStart) + (last.hasPayloads ? last.totalPositions : 0);
			doc = new CompressingTermVectorsWriter.DocData(numVectorFields, posStart, offStart, payStart);
		}
		pendingDocs.add(doc);
		return doc;
	}

	private class FieldData {
		final boolean hasPositions;

		final boolean hasOffsets;

		final boolean hasPayloads;

		final int fieldNum;

		final int flags;

		final int numTerms;

		final int[] freqs;

		final int[] prefixLengths;

		final int[] suffixLengths;

		final int posStart;

		final int offStart;

		final int payStart;

		int totalPositions;

		int ord;

		FieldData(int fieldNum, int numTerms, boolean positions, boolean offsets, boolean payloads, int posStart, int offStart, int payStart) {
			this.fieldNum = fieldNum;
			this.numTerms = numTerms;
			this.hasPositions = positions;
			this.hasOffsets = offsets;
			this.hasPayloads = payloads;
			this.flags = ((positions ? CompressingTermVectorsWriter.POSITIONS : 0) | (offsets ? CompressingTermVectorsWriter.OFFSETS : 0)) | (payloads ? CompressingTermVectorsWriter.PAYLOADS : 0);
			this.freqs = new int[numTerms];
			this.prefixLengths = new int[numTerms];
			this.suffixLengths = new int[numTerms];
			this.posStart = posStart;
			this.offStart = offStart;
			this.payStart = payStart;
			totalPositions = 0;
			ord = 0;
		}

		void addTerm(int freq, int prefixLength, int suffixLength) {
			freqs[ord] = freq;
			prefixLengths[ord] = prefixLength;
			suffixLengths[ord] = suffixLength;
			++(ord);
		}

		void addPosition(int position, int startOffset, int length, int payloadLength) {
			if (hasPositions) {
				if (((posStart) + (totalPositions)) == (positionsBuf.length)) {
					positionsBuf = ArrayUtil.grow(positionsBuf);
				}
				positionsBuf[((posStart) + (totalPositions))] = position;
			}
			if (hasOffsets) {
				if (((offStart) + (totalPositions)) == (startOffsetsBuf.length)) {
					final int newLength = ArrayUtil.oversize(((offStart) + (totalPositions)), 4);
					startOffsetsBuf = Arrays.copyOf(startOffsetsBuf, newLength);
					lengthsBuf = Arrays.copyOf(lengthsBuf, newLength);
				}
				startOffsetsBuf[((offStart) + (totalPositions))] = startOffset;
				lengthsBuf[((offStart) + (totalPositions))] = length;
			}
			if (hasPayloads) {
				if (((payStart) + (totalPositions)) == (payloadLengthsBuf.length)) {
					payloadLengthsBuf = ArrayUtil.grow(payloadLengthsBuf);
				}
				payloadLengthsBuf[((payStart) + (totalPositions))] = payloadLength;
			}
			++(totalPositions);
		}
	}

	private int numDocs;

	private final Deque<CompressingTermVectorsWriter.DocData> pendingDocs;

	private CompressingTermVectorsWriter.DocData curDoc;

	private CompressingTermVectorsWriter.FieldData curField;

	private final BytesRef lastTerm;

	private int[] positionsBuf;

	private int[] startOffsetsBuf;

	private int[] lengthsBuf;

	private int[] payloadLengthsBuf;

	private final GrowableByteArrayDataOutput termSuffixes;

	private final GrowableByteArrayDataOutput payloadBytes;

	private final BlockPackedWriter writer;

	public CompressingTermVectorsWriter(Directory directory, SegmentInfo si, String segmentSuffix, IOContext context, String formatName, CompressionMode compressionMode, int chunkSize, int blockSize) throws IOException {
		assert directory != null;
		this.segment = si.name;
		this.compressionMode = compressionMode;
		this.compressor = compressionMode.newCompressor();
		this.chunkSize = chunkSize;
		numDocs = 0;
		pendingDocs = new ArrayDeque<>();
		termSuffixes = new GrowableByteArrayDataOutput(ArrayUtil.oversize(chunkSize, 1));
		payloadBytes = new GrowableByteArrayDataOutput(ArrayUtil.oversize(1, 1));
		lastTerm = new BytesRef(ArrayUtil.oversize(30, 1));
		boolean success = false;
		IndexOutput indexStream = directory.createOutput(IndexFileNames.segmentFileName(segment, segmentSuffix, CompressingTermVectorsWriter.VECTORS_INDEX_EXTENSION), context);
		try {
			vectorsStream = directory.createOutput(IndexFileNames.segmentFileName(segment, segmentSuffix, CompressingTermVectorsWriter.VECTORS_EXTENSION), context);
			final String codecNameIdx = formatName + (CompressingTermVectorsWriter.CODEC_SFX_IDX);
			final String codecNameDat = formatName + (CompressingTermVectorsWriter.CODEC_SFX_DAT);
			CodecUtil.writeIndexHeader(indexStream, codecNameIdx, CompressingTermVectorsWriter.VERSION_CURRENT, si.getId(), segmentSuffix);
			CodecUtil.writeIndexHeader(vectorsStream, codecNameDat, CompressingTermVectorsWriter.VERSION_CURRENT, si.getId(), segmentSuffix);
			assert (CodecUtil.indexHeaderLength(codecNameDat, segmentSuffix)) == (vectorsStream.getFilePointer());
			assert (CodecUtil.indexHeaderLength(codecNameIdx, segmentSuffix)) == (indexStream.getFilePointer());
			indexStream = null;
			vectorsStream.writeVInt(PackedInts.VERSION_CURRENT);
			vectorsStream.writeVInt(chunkSize);
			writer = new BlockPackedWriter(vectorsStream, CompressingTermVectorsWriter.PACKED_BLOCK_SIZE);
			positionsBuf = new int[1024];
			startOffsetsBuf = new int[1024];
			lengthsBuf = new int[1024];
			payloadLengthsBuf = new int[1024];
			success = true;
		} finally {
			if (!success) {
				IOUtils.closeWhileHandlingException(vectorsStream, indexStream, indexWriter);
			}
		}
	}

	@Override
	public void close() throws IOException {
		try {
			IOUtils.close(vectorsStream, indexWriter);
		} finally {
			vectorsStream = null;
			indexWriter = null;
		}
	}

	@Override
	public void startDocument(int numVectorFields) throws IOException {
		curDoc = addDocData(numVectorFields);
	}

	@Override
	public void finishDocument() throws IOException {
		termSuffixes.writeBytes(payloadBytes.getBytes(), payloadBytes.getPosition());
		payloadBytes.reset();
		++(numDocs);
		if (triggerFlush()) {
			flush();
		}
		curDoc = null;
	}

	@Override
	public void startField(FieldInfo info, int numTerms, boolean positions, boolean offsets, boolean payloads) throws IOException {
		curField = curDoc.addField(info.number, numTerms, positions, offsets, payloads);
		lastTerm.length = 0;
	}

	@Override
	public void finishField() throws IOException {
		curField = null;
	}

	@Override
	public void startTerm(BytesRef term, int freq) throws IOException {
		assert freq >= 1;
		final int prefix;
		if ((lastTerm.length) == 0) {
			prefix = 0;
		}else {
			prefix = StringHelper.bytesDifference(lastTerm, term);
		}
		curField.addTerm(freq, prefix, ((term.length) - prefix));
		termSuffixes.writeBytes(term.bytes, ((term.offset) + prefix), ((term.length) - prefix));
		if ((lastTerm.bytes.length) < (term.length)) {
			lastTerm.bytes = new byte[ArrayUtil.oversize(term.length, 1)];
		}
		lastTerm.offset = 0;
		lastTerm.length = term.length;
		System.arraycopy(term.bytes, term.offset, lastTerm.bytes, 0, term.length);
	}

	@Override
	public void addPosition(int position, int startOffset, int endOffset, BytesRef payload) throws IOException {
		assert (curField.flags) != 0;
		curField.addPosition(position, startOffset, (endOffset - startOffset), (payload == null ? 0 : payload.length));
		if ((curField.hasPayloads) && (payload != null)) {
			payloadBytes.writeBytes(payload.bytes, payload.offset, payload.length);
		}
	}

	private boolean triggerFlush() {
		return ((termSuffixes.getPosition()) >= (chunkSize)) || ((pendingDocs.size()) >= (CompressingTermVectorsWriter.MAX_DOCUMENTS_PER_CHUNK));
	}

	private void flush() throws IOException {
		final int chunkDocs = pendingDocs.size();
		assert chunkDocs > 0 : chunkDocs;
		final int docBase = (numDocs) - chunkDocs;
		vectorsStream.writeVInt(docBase);
		vectorsStream.writeVInt(chunkDocs);
		final int totalFields = flushNumFields(chunkDocs);
		if (totalFields > 0) {
			final int[] fieldNums = flushFieldNums();
			flushFields(totalFields, fieldNums);
			flushFlags(totalFields, fieldNums);
			flushNumTerms(totalFields);
			flushTermLengths();
			flushTermFreqs();
			flushPositions();
			flushOffsets(fieldNums);
			flushPayloadLengths();
			compressor.compress(termSuffixes.getBytes(), 0, termSuffixes.getPosition(), vectorsStream);
		}
		pendingDocs.clear();
		curDoc = null;
		curField = null;
		termSuffixes.reset();
		(numChunks)++;
	}

	private int flushNumFields(int chunkDocs) throws IOException {
		if (chunkDocs == 1) {
			final int numFields = pendingDocs.getFirst().numFields;
			vectorsStream.writeVInt(numFields);
			return numFields;
		}else {
			writer.reset(vectorsStream);
			int totalFields = 0;
			for (CompressingTermVectorsWriter.DocData dd : pendingDocs) {
				writer.add(dd.numFields);
				totalFields += dd.numFields;
			}
			writer.finish();
			return totalFields;
		}
	}

	private int[] flushFieldNums() throws IOException {
		SortedSet<Integer> fieldNums = new TreeSet<>();
		for (CompressingTermVectorsWriter.DocData dd : pendingDocs) {
			for (CompressingTermVectorsWriter.FieldData fd : dd.fields) {
				fieldNums.add(fd.fieldNum);
			}
		}
		final int numDistinctFields = fieldNums.size();
		assert numDistinctFields > 0;
		final int bitsRequired = PackedInts.bitsRequired(fieldNums.last());
		final int token = ((Math.min((numDistinctFields - 1), 7)) << 5) | bitsRequired;
		vectorsStream.writeByte(((byte) (token)));
		if ((numDistinctFields - 1) >= 7) {
			vectorsStream.writeVInt(((numDistinctFields - 1) - 7));
		}
		final PackedInts.Writer writer = PackedInts.getWriterNoHeader(vectorsStream, PACKED, fieldNums.size(), bitsRequired, 1);
		for (Integer fieldNum : fieldNums) {
			writer.add(fieldNum);
		}
		writer.finish();
		int[] fns = new int[fieldNums.size()];
		int i = 0;
		for (Integer key : fieldNums) {
			fns[(i++)] = key;
		}
		return fns;
	}

	private void flushFields(int totalFields, int[] fieldNums) throws IOException {
		final PackedInts.Writer writer = PackedInts.getWriterNoHeader(vectorsStream, PACKED, totalFields, PackedInts.bitsRequired(((fieldNums.length) - 1)), 1);
		for (CompressingTermVectorsWriter.DocData dd : pendingDocs) {
			for (CompressingTermVectorsWriter.FieldData fd : dd.fields) {
				final int fieldNumIndex = Arrays.binarySearch(fieldNums, fd.fieldNum);
				assert fieldNumIndex >= 0;
				writer.add(fieldNumIndex);
			}
		}
		writer.finish();
	}

	private void flushFlags(int totalFields, int[] fieldNums) throws IOException {
		boolean nonChangingFlags = true;
		int[] fieldFlags = new int[fieldNums.length];
		Arrays.fill(fieldFlags, (-1));
		outer : for (CompressingTermVectorsWriter.DocData dd : pendingDocs) {
			for (CompressingTermVectorsWriter.FieldData fd : dd.fields) {
				final int fieldNumOff = Arrays.binarySearch(fieldNums, fd.fieldNum);
				assert fieldNumOff >= 0;
				if ((fieldFlags[fieldNumOff]) == (-1)) {
					fieldFlags[fieldNumOff] = fd.flags;
				}else
					if ((fieldFlags[fieldNumOff]) != (fd.flags)) {
						nonChangingFlags = false;
						break outer;
					}

			}
		}
		if (nonChangingFlags) {
			vectorsStream.writeVInt(0);
			final PackedInts.Writer writer = PackedInts.getWriterNoHeader(vectorsStream, PACKED, fieldFlags.length, CompressingTermVectorsWriter.FLAGS_BITS, 1);
			for (int flags : fieldFlags) {
				assert flags >= 0;
				writer.add(flags);
			}
			assert (writer.ord()) == ((fieldFlags.length) - 1);
			writer.finish();
		}else {
			vectorsStream.writeVInt(1);
			final PackedInts.Writer writer = PackedInts.getWriterNoHeader(vectorsStream, PACKED, totalFields, CompressingTermVectorsWriter.FLAGS_BITS, 1);
			for (CompressingTermVectorsWriter.DocData dd : pendingDocs) {
				for (CompressingTermVectorsWriter.FieldData fd : dd.fields) {
					writer.add(fd.flags);
				}
			}
			assert (writer.ord()) == (totalFields - 1);
			writer.finish();
		}
	}

	private void flushNumTerms(int totalFields) throws IOException {
		int maxNumTerms = 0;
		for (CompressingTermVectorsWriter.DocData dd : pendingDocs) {
			for (CompressingTermVectorsWriter.FieldData fd : dd.fields) {
				maxNumTerms |= fd.numTerms;
			}
		}
		final int bitsRequired = PackedInts.bitsRequired(maxNumTerms);
		vectorsStream.writeVInt(bitsRequired);
		final PackedInts.Writer writer = PackedInts.getWriterNoHeader(vectorsStream, PACKED, totalFields, bitsRequired, 1);
		for (CompressingTermVectorsWriter.DocData dd : pendingDocs) {
			for (CompressingTermVectorsWriter.FieldData fd : dd.fields) {
				writer.add(fd.numTerms);
			}
		}
		assert (writer.ord()) == (totalFields - 1);
		writer.finish();
	}

	private void flushTermLengths() throws IOException {
		writer.reset(vectorsStream);
		for (CompressingTermVectorsWriter.DocData dd : pendingDocs) {
			for (CompressingTermVectorsWriter.FieldData fd : dd.fields) {
				for (int i = 0; i < (fd.numTerms); ++i) {
					writer.add(fd.prefixLengths[i]);
				}
			}
		}
		writer.finish();
		writer.reset(vectorsStream);
		for (CompressingTermVectorsWriter.DocData dd : pendingDocs) {
			for (CompressingTermVectorsWriter.FieldData fd : dd.fields) {
				for (int i = 0; i < (fd.numTerms); ++i) {
					writer.add(fd.suffixLengths[i]);
				}
			}
		}
		writer.finish();
	}

	private void flushTermFreqs() throws IOException {
		writer.reset(vectorsStream);
		for (CompressingTermVectorsWriter.DocData dd : pendingDocs) {
			for (CompressingTermVectorsWriter.FieldData fd : dd.fields) {
				for (int i = 0; i < (fd.numTerms); ++i) {
					writer.add(((fd.freqs[i]) - 1));
				}
			}
		}
		writer.finish();
	}

	private void flushPositions() throws IOException {
		writer.reset(vectorsStream);
		for (CompressingTermVectorsWriter.DocData dd : pendingDocs) {
			for (CompressingTermVectorsWriter.FieldData fd : dd.fields) {
				if (fd.hasPositions) {
					int pos = 0;
					for (int i = 0; i < (fd.numTerms); ++i) {
						int previousPosition = 0;
						for (int j = 0; j < (fd.freqs[i]); ++j) {
							final int position = positionsBuf[((fd.posStart) + (pos++))];
							writer.add((position - previousPosition));
							previousPosition = position;
						}
					}
					assert pos == (fd.totalPositions);
				}
			}
		}
		writer.finish();
	}

	private void flushOffsets(int[] fieldNums) throws IOException {
		boolean hasOffsets = false;
		long[] sumPos = new long[fieldNums.length];
		long[] sumOffsets = new long[fieldNums.length];
		for (CompressingTermVectorsWriter.DocData dd : pendingDocs) {
			for (CompressingTermVectorsWriter.FieldData fd : dd.fields) {
				hasOffsets |= fd.hasOffsets;
				if ((fd.hasOffsets) && (fd.hasPositions)) {
					final int fieldNumOff = Arrays.binarySearch(fieldNums, fd.fieldNum);
					int pos = 0;
					for (int i = 0; i < (fd.numTerms); ++i) {
						int previousPos = 0;
						int previousOff = 0;
						for (int j = 0; j < (fd.freqs[i]); ++j) {
							final int position = positionsBuf[((fd.posStart) + pos)];
							final int startOffset = startOffsetsBuf[((fd.offStart) + pos)];
							sumPos[fieldNumOff] += position - previousPos;
							sumOffsets[fieldNumOff] += startOffset - previousOff;
							previousPos = position;
							previousOff = startOffset;
							++pos;
						}
					}
					assert pos == (fd.totalPositions);
				}
			}
		}
		if (!hasOffsets) {
			return;
		}
		final float[] charsPerTerm = new float[fieldNums.length];
		for (int i = 0; i < (fieldNums.length); ++i) {
			charsPerTerm[i] = (((sumPos[i]) <= 0) || ((sumOffsets[i]) <= 0)) ? 0 : ((float) (((double) (sumOffsets[i])) / (sumPos[i])));
		}
		for (int i = 0; i < (fieldNums.length); ++i) {
			vectorsStream.writeInt(Float.floatToRawIntBits(charsPerTerm[i]));
		}
		writer.reset(vectorsStream);
		for (CompressingTermVectorsWriter.DocData dd : pendingDocs) {
			for (CompressingTermVectorsWriter.FieldData fd : dd.fields) {
				if (((fd.flags) & (CompressingTermVectorsWriter.OFFSETS)) != 0) {
					final int fieldNumOff = Arrays.binarySearch(fieldNums, fd.fieldNum);
					final float cpt = charsPerTerm[fieldNumOff];
					int pos = 0;
					for (int i = 0; i < (fd.numTerms); ++i) {
						int previousPos = 0;
						int previousOff = 0;
						for (int j = 0; j < (fd.freqs[i]); ++j) {
							final int position = (fd.hasPositions) ? positionsBuf[((fd.posStart) + pos)] : 0;
							final int startOffset = startOffsetsBuf[((fd.offStart) + pos)];
							writer.add(((startOffset - previousOff) - ((int) (cpt * (position - previousPos)))));
							previousPos = position;
							previousOff = startOffset;
							++pos;
						}
					}
				}
			}
		}
		writer.finish();
		writer.reset(vectorsStream);
		for (CompressingTermVectorsWriter.DocData dd : pendingDocs) {
			for (CompressingTermVectorsWriter.FieldData fd : dd.fields) {
				if (((fd.flags) & (CompressingTermVectorsWriter.OFFSETS)) != 0) {
					int pos = 0;
					for (int i = 0; i < (fd.numTerms); ++i) {
						for (int j = 0; j < (fd.freqs[i]); ++j) {
							writer.add((((lengthsBuf[((fd.offStart) + (pos++))]) - (fd.prefixLengths[i])) - (fd.suffixLengths[i])));
						}
					}
					assert pos == (fd.totalPositions);
				}
			}
		}
		writer.finish();
	}

	private void flushPayloadLengths() throws IOException {
		writer.reset(vectorsStream);
		for (CompressingTermVectorsWriter.DocData dd : pendingDocs) {
			for (CompressingTermVectorsWriter.FieldData fd : dd.fields) {
				if (fd.hasPayloads) {
					for (int i = 0; i < (fd.totalPositions); ++i) {
						writer.add(payloadLengthsBuf[((fd.payStart) + i)]);
					}
				}
			}
		}
		writer.finish();
	}

	@Override
	public void finish(FieldInfos fis, int numDocs) throws IOException {
		if (!(pendingDocs.isEmpty())) {
			flush();
			(numDirtyChunks)++;
		}
		if (numDocs != (this.numDocs)) {
			throw new RuntimeException(((("Wrote " + (this.numDocs)) + " docs, finish called with numDocs=") + numDocs));
		}
		vectorsStream.writeVLong(numChunks);
		vectorsStream.writeVLong(numDirtyChunks);
		CodecUtil.writeFooter(vectorsStream);
	}

	@Override
	public void addProx(int numProx, DataInput positions, DataInput offsets) throws IOException {
		assert (curField.hasPositions) == (positions != null);
		assert (curField.hasOffsets) == (offsets != null);
		if (curField.hasPositions) {
			final int posStart = (curField.posStart) + (curField.totalPositions);
			if ((posStart + numProx) > (positionsBuf.length)) {
				positionsBuf = ArrayUtil.grow(positionsBuf, (posStart + numProx));
			}
			int position = 0;
			if (curField.hasPayloads) {
				final int payStart = (curField.payStart) + (curField.totalPositions);
				if ((payStart + numProx) > (payloadLengthsBuf.length)) {
					payloadLengthsBuf = ArrayUtil.grow(payloadLengthsBuf, (payStart + numProx));
				}
				for (int i = 0; i < numProx; ++i) {
					final int code = positions.readVInt();
					if ((code & 1) != 0) {
						final int payloadLength = positions.readVInt();
						payloadLengthsBuf[(payStart + i)] = payloadLength;
						payloadBytes.copyBytes(positions, payloadLength);
					}else {
						payloadLengthsBuf[(payStart + i)] = 0;
					}
					position += code >>> 1;
					positionsBuf[(posStart + i)] = position;
				}
			}else {
				for (int i = 0; i < numProx; ++i) {
					position += (positions.readVInt()) >>> 1;
					positionsBuf[(posStart + i)] = position;
				}
			}
		}
		if (curField.hasOffsets) {
			final int offStart = (curField.offStart) + (curField.totalPositions);
			if ((offStart + numProx) > (startOffsetsBuf.length)) {
				final int newLength = ArrayUtil.oversize((offStart + numProx), 4);
				startOffsetsBuf = Arrays.copyOf(startOffsetsBuf, newLength);
				lengthsBuf = Arrays.copyOf(lengthsBuf, newLength);
			}
			int lastOffset = 0;
			int startOffset;
			int endOffset;
			for (int i = 0; i < numProx; ++i) {
				startOffset = lastOffset + (offsets.readVInt());
				endOffset = startOffset + (offsets.readVInt());
				lastOffset = endOffset;
				startOffsetsBuf[(offStart + i)] = startOffset;
				lengthsBuf[(offStart + i)] = endOffset - startOffset;
			}
		}
		curField.totalPositions += numProx;
	}

	static final String BULK_MERGE_ENABLED_SYSPROP = (CompressingTermVectorsWriter.class.getName()) + ".enableBulkMerge";

	static final boolean BULK_MERGE_ENABLED;

	static {
		boolean v = true;
		try {
			v = Boolean.parseBoolean(System.getProperty(CompressingTermVectorsWriter.BULK_MERGE_ENABLED_SYSPROP, "true"));
		} catch (SecurityException ignored) {
		}
		BULK_MERGE_ENABLED = v;
	}

	@Override
	public int merge(MergeState mergeState) throws IOException {
		if (mergeState.needsIndexSort) {
			return super.merge(mergeState);
		}
		int docCount = 0;
		int numReaders = mergeState.maxDocs.length;
		for (int readerIndex = 0; readerIndex < numReaders; readerIndex++) {
			CompressingTermVectorsReader matchingVectorsReader = null;
			final TermVectorsReader vectorsReader = mergeState.termVectorsReaders[readerIndex];
			final int maxDoc = mergeState.maxDocs[readerIndex];
			final Bits liveDocs = mergeState.liveDocs[readerIndex];
		}
		finish(mergeState.mergeFieldInfos, docCount);
		return docCount;
	}

	boolean tooDirty(CompressingTermVectorsReader candidate) {
		return false;
	}
}

