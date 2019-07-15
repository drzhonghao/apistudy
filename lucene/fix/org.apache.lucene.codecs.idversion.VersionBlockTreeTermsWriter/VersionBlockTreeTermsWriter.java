

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.FieldsConsumer;
import org.apache.lucene.codecs.PostingsWriterBase;
import org.apache.lucene.codecs.blocktree.BlockTreeTermsWriter;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.RAMOutputStream;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.BytesRefIterator;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.StringHelper;
import org.apache.lucene.util.fst.Builder;
import org.apache.lucene.util.fst.ByteSequenceOutputs;
import org.apache.lucene.util.fst.BytesRefFSTEnum;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.PairOutputs;
import org.apache.lucene.util.fst.PairOutputs.Pair;
import org.apache.lucene.util.fst.PositiveIntOutputs;
import org.apache.lucene.util.fst.Util;

import static org.apache.lucene.util.fst.FST.INPUT_TYPE.BYTE1;


public final class VersionBlockTreeTermsWriter extends FieldsConsumer {
	static final PairOutputs<BytesRef, Long> FST_OUTPUTS = new PairOutputs<>(ByteSequenceOutputs.getSingleton(), PositiveIntOutputs.getSingleton());

	static final PairOutputs.Pair<BytesRef, Long> NO_OUTPUT = VersionBlockTreeTermsWriter.FST_OUTPUTS.getNoOutput();

	public static final int DEFAULT_MIN_BLOCK_SIZE = 25;

	public static final int DEFAULT_MAX_BLOCK_SIZE = 48;

	static final int OUTPUT_FLAGS_NUM_BITS = 2;

	static final int OUTPUT_FLAGS_MASK = 3;

	static final int OUTPUT_FLAG_IS_FLOOR = 1;

	static final int OUTPUT_FLAG_HAS_TERMS = 2;

	static final String TERMS_EXTENSION = "tiv";

	static final String TERMS_CODEC_NAME = "VersionBlockTreeTermsDict";

	public static final int VERSION_START = 1;

	public static final int VERSION_CURRENT = VersionBlockTreeTermsWriter.VERSION_START;

	static final String TERMS_INDEX_EXTENSION = "tipv";

	static final String TERMS_INDEX_CODEC_NAME = "VersionBlockTreeTermsIndex";

	private final IndexOutput out;

	private final IndexOutput indexOut;

	final int maxDoc;

	final int minItemsInBlock;

	final int maxItemsInBlock;

	final PostingsWriterBase postingsWriter;

	final FieldInfos fieldInfos;

	private static class FieldMetaData {
		public final FieldInfo fieldInfo;

		public final PairOutputs.Pair<BytesRef, Long> rootCode;

		public final long numTerms;

		public final long indexStartFP;

		private final int longsSize;

		public final BytesRef minTerm;

		public final BytesRef maxTerm;

		public FieldMetaData(FieldInfo fieldInfo, PairOutputs.Pair<BytesRef, Long> rootCode, long numTerms, long indexStartFP, int longsSize, BytesRef minTerm, BytesRef maxTerm) {
			assert numTerms > 0;
			this.fieldInfo = fieldInfo;
			assert rootCode != null : (("field=" + (fieldInfo.name)) + " numTerms=") + numTerms;
			this.rootCode = rootCode;
			this.indexStartFP = indexStartFP;
			this.numTerms = numTerms;
			this.longsSize = longsSize;
			this.minTerm = minTerm;
			this.maxTerm = maxTerm;
		}
	}

	private final List<VersionBlockTreeTermsWriter.FieldMetaData> fields = new ArrayList<>();

	private final String segment;

	public VersionBlockTreeTermsWriter(SegmentWriteState state, PostingsWriterBase postingsWriter, int minItemsInBlock, int maxItemsInBlock) throws IOException {
		BlockTreeTermsWriter.validateSettings(minItemsInBlock, maxItemsInBlock);
		segment = state.segmentInfo.name;
		maxDoc = state.segmentInfo.maxDoc();
		final String termsFileName = IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, VersionBlockTreeTermsWriter.TERMS_EXTENSION);
		out = state.directory.createOutput(termsFileName, state.context);
		boolean success = false;
		IndexOutput indexOut = null;
		try {
			fieldInfos = state.fieldInfos;
			this.minItemsInBlock = minItemsInBlock;
			this.maxItemsInBlock = maxItemsInBlock;
			CodecUtil.writeIndexHeader(out, VersionBlockTreeTermsWriter.TERMS_CODEC_NAME, VersionBlockTreeTermsWriter.VERSION_CURRENT, state.segmentInfo.getId(), state.segmentSuffix);
			final String termsIndexFileName = IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, VersionBlockTreeTermsWriter.TERMS_INDEX_EXTENSION);
			indexOut = state.directory.createOutput(termsIndexFileName, state.context);
			CodecUtil.writeIndexHeader(indexOut, VersionBlockTreeTermsWriter.TERMS_INDEX_CODEC_NAME, VersionBlockTreeTermsWriter.VERSION_CURRENT, state.segmentInfo.getId(), state.segmentSuffix);
			this.postingsWriter = postingsWriter;
			postingsWriter.init(out, state);
			success = true;
		} finally {
			if (!success) {
				IOUtils.closeWhileHandlingException(out, indexOut);
			}
		}
		this.indexOut = indexOut;
	}

	private void writeTrailer(IndexOutput out, long dirStart) throws IOException {
		out.writeLong(dirStart);
	}

	private void writeIndexTrailer(IndexOutput indexOut, long dirStart) throws IOException {
		indexOut.writeLong(dirStart);
	}

	@Override
	public void write(Fields fields) throws IOException {
		String lastField = null;
		for (String field : fields) {
			assert (lastField == null) || ((lastField.compareTo(field)) < 0);
			lastField = field;
			Terms terms = fields.terms(field);
			if (terms == null) {
				continue;
			}
			TermsEnum termsEnum = terms.iterator();
			VersionBlockTreeTermsWriter.TermsWriter termsWriter = new VersionBlockTreeTermsWriter.TermsWriter(fieldInfos.fieldInfo(field));
			while (true) {
				BytesRef term = termsEnum.next();
				if (term == null) {
					break;
				}
				termsWriter.write(term, termsEnum);
			} 
			termsWriter.finish();
		}
	}

	static long encodeOutput(long fp, boolean hasTerms, boolean isFloor) {
		assert fp < (1L << 62);
		return ((fp << 2) | (hasTerms ? VersionBlockTreeTermsWriter.OUTPUT_FLAG_HAS_TERMS : 0)) | (isFloor ? VersionBlockTreeTermsWriter.OUTPUT_FLAG_IS_FLOOR : 0);
	}

	private static class PendingEntry {
		public final boolean isTerm;

		protected PendingEntry(boolean isTerm) {
			this.isTerm = isTerm;
		}
	}

	private static final class PendingTerm extends VersionBlockTreeTermsWriter.PendingEntry {
		public final byte[] termBytes;

		public final BlockTermState state;

		public PendingTerm(BytesRef term, BlockTermState state) {
			super(true);
			this.termBytes = new byte[term.length];
			System.arraycopy(term.bytes, term.offset, termBytes, 0, term.length);
			this.state = state;
		}

		@Override
		public String toString() {
			return VersionBlockTreeTermsWriter.brToString(termBytes);
		}
	}

	@SuppressWarnings("unused")
	static String brToString(BytesRef b) {
		try {
			return ((b.utf8ToString()) + " ") + b;
		} catch (Throwable t) {
			return b.toString();
		}
	}

	@SuppressWarnings("unused")
	static String brToString(byte[] b) {
		return VersionBlockTreeTermsWriter.brToString(new BytesRef(b));
	}

	private static final class PendingBlock extends VersionBlockTreeTermsWriter.PendingEntry {
		public final BytesRef prefix;

		public final long fp;

		public FST<PairOutputs.Pair<BytesRef, Long>> index;

		public List<FST<PairOutputs.Pair<BytesRef, Long>>> subIndices;

		public final boolean hasTerms;

		public final boolean isFloor;

		public final int floorLeadByte;

		private final long maxVersion;

		public PendingBlock(BytesRef prefix, long maxVersion, long fp, boolean hasTerms, boolean isFloor, int floorLeadByte, List<FST<PairOutputs.Pair<BytesRef, Long>>> subIndices) {
			super(false);
			this.prefix = prefix;
			this.maxVersion = maxVersion;
			this.fp = fp;
			this.hasTerms = hasTerms;
			this.isFloor = isFloor;
			this.floorLeadByte = floorLeadByte;
			this.subIndices = subIndices;
		}

		@Override
		public String toString() {
			return "BLOCK: " + (VersionBlockTreeTermsWriter.brToString(prefix));
		}

		public void compileIndex(List<VersionBlockTreeTermsWriter.PendingBlock> blocks, RAMOutputStream scratchBytes, IntsRefBuilder scratchIntsRef) throws IOException {
			assert ((isFloor) && ((blocks.size()) > 1)) || (((isFloor) == false) && ((blocks.size()) == 1)) : (("isFloor=" + (isFloor)) + " blocks=") + blocks;
			assert (this) == (blocks.get(0));
			assert (scratchBytes.getFilePointer()) == 0;
			long maxVersionIndex = maxVersion;
			scratchBytes.writeVLong(VersionBlockTreeTermsWriter.encodeOutput(fp, hasTerms, isFloor));
			if (isFloor) {
				scratchBytes.writeVInt(((blocks.size()) - 1));
				for (int i = 1; i < (blocks.size()); i++) {
					VersionBlockTreeTermsWriter.PendingBlock sub = blocks.get(i);
					maxVersionIndex = Math.max(maxVersionIndex, sub.maxVersion);
					scratchBytes.writeByte(((byte) (sub.floorLeadByte)));
					assert (sub.fp) > (fp);
					scratchBytes.writeVLong(((((sub.fp) - (fp)) << 1) | (sub.hasTerms ? 1 : 0)));
				}
			}
			final Builder<PairOutputs.Pair<BytesRef, Long>> indexBuilder = new Builder<>(BYTE1, 0, 0, true, false, Integer.MAX_VALUE, VersionBlockTreeTermsWriter.FST_OUTPUTS, true, 15);
			final byte[] bytes = new byte[((int) (scratchBytes.getFilePointer()))];
			assert (bytes.length) > 0;
			scratchBytes.writeTo(bytes, 0);
			indexBuilder.add(Util.toIntsRef(prefix, scratchIntsRef), VersionBlockTreeTermsWriter.FST_OUTPUTS.newPair(new BytesRef(bytes, 0, bytes.length), ((Long.MAX_VALUE) - maxVersionIndex)));
			scratchBytes.reset();
			for (VersionBlockTreeTermsWriter.PendingBlock block : blocks) {
				if ((block.subIndices) != null) {
					for (FST<PairOutputs.Pair<BytesRef, Long>> subIndex : block.subIndices) {
						append(indexBuilder, subIndex, scratchIntsRef);
					}
					block.subIndices = null;
				}
			}
			index = indexBuilder.finish();
			assert (subIndices) == null;
		}

		private void append(Builder<PairOutputs.Pair<BytesRef, Long>> builder, FST<PairOutputs.Pair<BytesRef, Long>> subIndex, IntsRefBuilder scratchIntsRef) throws IOException {
			final BytesRefFSTEnum<PairOutputs.Pair<BytesRef, Long>> subIndexEnum = new BytesRefFSTEnum<>(subIndex);
			BytesRefFSTEnum.InputOutput<PairOutputs.Pair<BytesRef, Long>> indexEnt;
			while ((indexEnt = subIndexEnum.next()) != null) {
				builder.add(Util.toIntsRef(indexEnt.input, scratchIntsRef), indexEnt.output);
			} 
		}
	}

	private final RAMOutputStream scratchBytes = new RAMOutputStream();

	private final IntsRefBuilder scratchIntsRef = new IntsRefBuilder();

	class TermsWriter {
		private final FieldInfo fieldInfo;

		private final int longsSize;

		private long numTerms;

		final FixedBitSet docsSeen;

		long indexStartFP;

		private final BytesRefBuilder lastTerm = new BytesRefBuilder();

		private int[] prefixStarts = new int[8];

		private final long[] longs;

		private final List<VersionBlockTreeTermsWriter.PendingEntry> pending = new ArrayList<>();

		private final List<VersionBlockTreeTermsWriter.PendingBlock> newBlocks = new ArrayList<>();

		private VersionBlockTreeTermsWriter.PendingTerm firstPendingTerm;

		private VersionBlockTreeTermsWriter.PendingTerm lastPendingTerm;

		void writeBlocks(int prefixLength, int count) throws IOException {
			assert count > 0;
			assert (prefixLength > 0) || (count == (pending.size()));
			int lastSuffixLeadLabel = -1;
			boolean hasTerms = false;
			boolean hasSubBlocks = false;
			int start = (pending.size()) - count;
			int end = pending.size();
			int nextBlockStart = start;
			int nextFloorLeadLabel = -1;
			for (int i = start; i < end; i++) {
				VersionBlockTreeTermsWriter.PendingEntry ent = pending.get(i);
				int suffixLeadLabel;
				if (ent.isTerm) {
					VersionBlockTreeTermsWriter.PendingTerm term = ((VersionBlockTreeTermsWriter.PendingTerm) (ent));
					if ((term.termBytes.length) == prefixLength) {
						assert lastSuffixLeadLabel == (-1);
						suffixLeadLabel = -1;
					}else {
						suffixLeadLabel = (term.termBytes[prefixLength]) & 255;
					}
				}else {
					VersionBlockTreeTermsWriter.PendingBlock block = ((VersionBlockTreeTermsWriter.PendingBlock) (ent));
					assert (block.prefix.length) > prefixLength;
					suffixLeadLabel = (block.prefix.bytes[((block.prefix.offset) + prefixLength)]) & 255;
				}
				if (suffixLeadLabel != lastSuffixLeadLabel) {
					int itemsInBlock = i - nextBlockStart;
					if ((itemsInBlock >= (minItemsInBlock)) && ((end - nextBlockStart) > (maxItemsInBlock))) {
						boolean isFloor = itemsInBlock < count;
						newBlocks.add(writeBlock(prefixLength, isFloor, nextFloorLeadLabel, nextBlockStart, i, hasTerms, hasSubBlocks));
						hasTerms = false;
						hasSubBlocks = false;
						nextFloorLeadLabel = suffixLeadLabel;
						nextBlockStart = i;
					}
					lastSuffixLeadLabel = suffixLeadLabel;
				}
				if (ent.isTerm) {
					hasTerms = true;
				}else {
					hasSubBlocks = true;
				}
			}
			if (nextBlockStart < end) {
				int itemsInBlock = end - nextBlockStart;
				boolean isFloor = itemsInBlock < count;
				newBlocks.add(writeBlock(prefixLength, isFloor, nextFloorLeadLabel, nextBlockStart, end, hasTerms, hasSubBlocks));
			}
			assert (newBlocks.isEmpty()) == false;
			VersionBlockTreeTermsWriter.PendingBlock firstBlock = newBlocks.get(0);
			assert (firstBlock.isFloor) || ((newBlocks.size()) == 1);
			firstBlock.compileIndex(newBlocks, scratchBytes, scratchIntsRef);
			pending.subList(((pending.size()) - count), pending.size()).clear();
			pending.add(firstBlock);
			newBlocks.clear();
		}

		private VersionBlockTreeTermsWriter.PendingBlock writeBlock(int prefixLength, boolean isFloor, int floorLeadLabel, int start, int end, boolean hasTerms, boolean hasSubBlocks) throws IOException {
			assert end > start;
			long startFP = out.getFilePointer();
			boolean hasFloorLeadLabel = isFloor && (floorLeadLabel != (-1));
			final BytesRef prefix = new BytesRef((prefixLength + (hasFloorLeadLabel ? 1 : 0)));
			System.arraycopy(lastTerm.bytes(), 0, prefix.bytes, 0, prefixLength);
			prefix.length = prefixLength;
			int numEntries = end - start;
			int code = numEntries << 1;
			if (end == (pending.size())) {
				code |= 1;
			}
			out.writeVInt(code);
			boolean isLeafBlock = hasSubBlocks == false;
			final List<FST<PairOutputs.Pair<BytesRef, Long>>> subIndices;
			boolean absolute = true;
			long maxVersionInBlock = -1;
			if (isLeafBlock) {
				subIndices = null;
				for (int i = start; i < end; i++) {
					VersionBlockTreeTermsWriter.PendingEntry ent = pending.get(i);
					assert ent.isTerm : "i=" + i;
					VersionBlockTreeTermsWriter.PendingTerm term = ((VersionBlockTreeTermsWriter.PendingTerm) (ent));
					assert StringHelper.startsWith(term.termBytes, prefix) : (("term.term=" + (term.termBytes)) + " prefix=") + prefix;
					BlockTermState state = term.state;
					final int suffix = (term.termBytes.length) - prefixLength;
					suffixWriter.writeVInt(suffix);
					suffixWriter.writeBytes(term.termBytes, prefixLength, suffix);
					assert (floorLeadLabel == (-1)) || (((term.termBytes[prefixLength]) & 255) >= floorLeadLabel);
					postingsWriter.encodeTerm(longs, bytesWriter, fieldInfo, state, absolute);
					for (int pos = 0; pos < (longsSize); pos++) {
						assert (longs[pos]) >= 0;
						metaWriter.writeVLong(longs[pos]);
					}
					bytesWriter.writeTo(metaWriter);
					bytesWriter.reset();
					absolute = false;
				}
			}else {
				subIndices = new ArrayList<>();
				for (int i = start; i < end; i++) {
					VersionBlockTreeTermsWriter.PendingEntry ent = pending.get(i);
					if (ent.isTerm) {
						VersionBlockTreeTermsWriter.PendingTerm term = ((VersionBlockTreeTermsWriter.PendingTerm) (ent));
						assert StringHelper.startsWith(term.termBytes, prefix) : (("term.term=" + (term.termBytes)) + " prefix=") + prefix;
						BlockTermState state = term.state;
						final int suffix = (term.termBytes.length) - prefixLength;
						suffixWriter.writeVInt((suffix << 1));
						suffixWriter.writeBytes(term.termBytes, prefixLength, suffix);
						assert (floorLeadLabel == (-1)) || (((term.termBytes[prefixLength]) & 255) >= floorLeadLabel);
						postingsWriter.encodeTerm(longs, bytesWriter, fieldInfo, state, absolute);
						for (int pos = 0; pos < (longsSize); pos++) {
							assert (longs[pos]) >= 0;
							metaWriter.writeVLong(longs[pos]);
						}
						bytesWriter.writeTo(metaWriter);
						bytesWriter.reset();
						absolute = false;
					}else {
						VersionBlockTreeTermsWriter.PendingBlock block = ((VersionBlockTreeTermsWriter.PendingBlock) (ent));
						maxVersionInBlock = Math.max(maxVersionInBlock, block.maxVersion);
						assert StringHelper.startsWith(block.prefix, prefix);
						final int suffix = (block.prefix.length) - prefixLength;
						assert suffix > 0;
						suffixWriter.writeVInt(((suffix << 1) | 1));
						suffixWriter.writeBytes(block.prefix.bytes, prefixLength, suffix);
						assert (floorLeadLabel == (-1)) || (((block.prefix.bytes[prefixLength]) & 255) >= floorLeadLabel);
						assert (block.fp) < startFP;
						suffixWriter.writeVLong((startFP - (block.fp)));
						subIndices.add(block.index);
					}
				}
				assert (subIndices.size()) != 0;
			}
			out.writeVInt((((int) ((suffixWriter.getFilePointer()) << 1)) | (isLeafBlock ? 1 : 0)));
			suffixWriter.writeTo(out);
			suffixWriter.reset();
			out.writeVInt(((int) (metaWriter.getFilePointer())));
			metaWriter.writeTo(out);
			metaWriter.reset();
			if (hasFloorLeadLabel) {
				prefix.bytes[((prefix.length)++)] = ((byte) (floorLeadLabel));
			}
			return new VersionBlockTreeTermsWriter.PendingBlock(prefix, maxVersionInBlock, startFP, hasTerms, isFloor, floorLeadLabel, subIndices);
		}

		TermsWriter(FieldInfo fieldInfo) {
			this.fieldInfo = fieldInfo;
			docsSeen = new FixedBitSet(maxDoc);
			this.longsSize = postingsWriter.setField(fieldInfo);
			this.longs = new long[longsSize];
		}

		public void write(BytesRef text, TermsEnum termsEnum) throws IOException {
			BlockTermState state = postingsWriter.writeTerm(text, termsEnum, docsSeen);
		}

		private void pushTerm(BytesRef text) throws IOException {
			int limit = Math.min(lastTerm.length(), text.length);
			int pos = 0;
			while ((pos < limit) && ((lastTerm.byteAt(pos)) == (text.bytes[((text.offset) + pos)]))) {
				pos++;
			} 
			for (int i = (lastTerm.length()) - 1; i >= pos; i--) {
				int prefixTopSize = (pending.size()) - (prefixStarts[i]);
				if (prefixTopSize >= (minItemsInBlock)) {
					writeBlocks((i + 1), prefixTopSize);
					prefixStarts[i] -= prefixTopSize - 1;
				}
			}
			if ((prefixStarts.length) < (text.length)) {
				prefixStarts = ArrayUtil.grow(prefixStarts, text.length);
			}
			for (int i = pos; i < (text.length); i++) {
				prefixStarts[i] = pending.size();
			}
			lastTerm.copyBytes(text);
		}

		public void finish() throws IOException {
			if ((numTerms) > 0) {
				writeBlocks(0, pending.size());
				assert ((pending.size()) == 1) && (!(pending.get(0).isTerm)) : (("pending.size()=" + (pending.size())) + " pending=") + (pending);
				final VersionBlockTreeTermsWriter.PendingBlock root = ((VersionBlockTreeTermsWriter.PendingBlock) (pending.get(0)));
				assert (root.prefix.length) == 0;
				assert (root.index.getEmptyOutput()) != null;
				indexStartFP = indexOut.getFilePointer();
				root.index.save(indexOut);
				assert (firstPendingTerm) != null;
				BytesRef minTerm = new BytesRef(firstPendingTerm.termBytes);
				assert (lastPendingTerm) != null;
				BytesRef maxTerm = new BytesRef(lastPendingTerm.termBytes);
				fields.add(new VersionBlockTreeTermsWriter.FieldMetaData(fieldInfo, ((VersionBlockTreeTermsWriter.PendingBlock) (pending.get(0))).index.getEmptyOutput(), numTerms, indexStartFP, longsSize, minTerm, maxTerm));
			}else {
			}
		}

		private final RAMOutputStream suffixWriter = new RAMOutputStream();

		private final RAMOutputStream metaWriter = new RAMOutputStream();

		private final RAMOutputStream bytesWriter = new RAMOutputStream();
	}

	private boolean closed;

	@Override
	public void close() throws IOException {
		if (closed) {
			return;
		}
		closed = true;
		boolean success = false;
		try {
			final long dirStart = out.getFilePointer();
			final long indexDirStart = indexOut.getFilePointer();
			out.writeVInt(fields.size());
			for (VersionBlockTreeTermsWriter.FieldMetaData field : fields) {
				out.writeVInt(field.fieldInfo.number);
				assert (field.numTerms) > 0;
				out.writeVLong(field.numTerms);
				out.writeVInt(field.rootCode.output1.length);
				out.writeBytes(field.rootCode.output1.bytes, field.rootCode.output1.offset, field.rootCode.output1.length);
				out.writeVLong(field.rootCode.output2);
				out.writeVInt(field.longsSize);
				indexOut.writeVLong(field.indexStartFP);
				VersionBlockTreeTermsWriter.writeBytesRef(out, field.minTerm);
				VersionBlockTreeTermsWriter.writeBytesRef(out, field.maxTerm);
			}
			writeTrailer(out, dirStart);
			CodecUtil.writeFooter(out);
			writeIndexTrailer(indexOut, indexDirStart);
			CodecUtil.writeFooter(indexOut);
			success = true;
		} finally {
			if (success) {
				IOUtils.close(out, indexOut, postingsWriter);
			}else {
				IOUtils.closeWhileHandlingException(out, indexOut, postingsWriter);
			}
		}
	}

	private static void writeBytesRef(IndexOutput out, BytesRef bytes) throws IOException {
		out.writeVInt(bytes.length);
		out.writeBytes(bytes.bytes, bytes.offset, bytes.length);
	}
}

