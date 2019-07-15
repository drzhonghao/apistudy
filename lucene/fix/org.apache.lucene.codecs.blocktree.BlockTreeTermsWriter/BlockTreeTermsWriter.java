

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.FieldsConsumer;
import org.apache.lucene.codecs.PostingsWriterBase;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.DataOutput;
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
import org.apache.lucene.util.fst.Util;

import static org.apache.lucene.util.fst.FST.INPUT_TYPE.BYTE1;


public final class BlockTreeTermsWriter extends FieldsConsumer {
	public static final int DEFAULT_MIN_BLOCK_SIZE = 25;

	public static final int DEFAULT_MAX_BLOCK_SIZE = 48;

	private IndexOutput termsOut = null;

	private final IndexOutput indexOut;

	final int maxDoc;

	final int minItemsInBlock;

	final int maxItemsInBlock;

	final PostingsWriterBase postingsWriter;

	final FieldInfos fieldInfos;

	private static class FieldMetaData {
		public final FieldInfo fieldInfo;

		public final BytesRef rootCode;

		public final long numTerms;

		public final long indexStartFP;

		public final long sumTotalTermFreq;

		public final long sumDocFreq;

		public final int docCount;

		private final int longsSize;

		public final BytesRef minTerm;

		public final BytesRef maxTerm;

		public FieldMetaData(FieldInfo fieldInfo, BytesRef rootCode, long numTerms, long indexStartFP, long sumTotalTermFreq, long sumDocFreq, int docCount, int longsSize, BytesRef minTerm, BytesRef maxTerm) {
			assert numTerms > 0;
			this.fieldInfo = fieldInfo;
			assert rootCode != null : (("field=" + (fieldInfo.name)) + " numTerms=") + numTerms;
			this.rootCode = rootCode;
			this.indexStartFP = indexStartFP;
			this.numTerms = numTerms;
			this.sumTotalTermFreq = sumTotalTermFreq;
			this.sumDocFreq = sumDocFreq;
			this.docCount = docCount;
			this.longsSize = longsSize;
			this.minTerm = minTerm;
			this.maxTerm = maxTerm;
		}
	}

	private final List<BlockTreeTermsWriter.FieldMetaData> fields = new ArrayList<>();

	public BlockTreeTermsWriter(SegmentWriteState state, PostingsWriterBase postingsWriter, int minItemsInBlock, int maxItemsInBlock) throws IOException {
		BlockTreeTermsWriter.validateSettings(minItemsInBlock, maxItemsInBlock);
		this.minItemsInBlock = minItemsInBlock;
		this.maxItemsInBlock = maxItemsInBlock;
		this.maxDoc = state.segmentInfo.maxDoc();
		this.fieldInfos = state.fieldInfos;
		this.postingsWriter = postingsWriter;
		boolean success = false;
		IndexOutput indexOut = null;
		try {
			postingsWriter.init(termsOut, state);
			this.indexOut = indexOut;
			success = true;
		} finally {
			if (!success) {
				IOUtils.closeWhileHandlingException(termsOut, indexOut);
			}
		}
		termsOut = null;
	}

	private void writeTrailer(IndexOutput out, long dirStart) throws IOException {
		out.writeLong(dirStart);
	}

	private void writeIndexTrailer(IndexOutput indexOut, long dirStart) throws IOException {
		indexOut.writeLong(dirStart);
	}

	public static void validateSettings(int minItemsInBlock, int maxItemsInBlock) {
		if (minItemsInBlock <= 1) {
			throw new IllegalArgumentException(("minItemsInBlock must be >= 2; got " + minItemsInBlock));
		}
		if (minItemsInBlock > maxItemsInBlock) {
			throw new IllegalArgumentException(((("maxItemsInBlock must be >= minItemsInBlock; got maxItemsInBlock=" + maxItemsInBlock) + " minItemsInBlock=") + minItemsInBlock));
		}
		if ((2 * (minItemsInBlock - 1)) > maxItemsInBlock) {
			throw new IllegalArgumentException(((("maxItemsInBlock must be at least 2*(minItemsInBlock-1); got maxItemsInBlock=" + maxItemsInBlock) + " minItemsInBlock=") + minItemsInBlock));
		}
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
			BlockTreeTermsWriter.TermsWriter termsWriter = new BlockTreeTermsWriter.TermsWriter(fieldInfos.fieldInfo(field));
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
		return 0L;
	}

	private static class PendingEntry {
		public final boolean isTerm;

		protected PendingEntry(boolean isTerm) {
			this.isTerm = isTerm;
		}
	}

	private static final class PendingTerm extends BlockTreeTermsWriter.PendingEntry {
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
			return "TERM: " + (BlockTreeTermsWriter.brToString(termBytes));
		}
	}

	@SuppressWarnings("unused")
	static String brToString(BytesRef b) {
		if (b == null) {
			return "(null)";
		}else {
			try {
				return ((b.utf8ToString()) + " ") + b;
			} catch (Throwable t) {
				return b.toString();
			}
		}
	}

	@SuppressWarnings("unused")
	static String brToString(byte[] b) {
		return BlockTreeTermsWriter.brToString(new BytesRef(b));
	}

	private static final class PendingBlock extends BlockTreeTermsWriter.PendingEntry {
		public final BytesRef prefix;

		public final long fp;

		public FST<BytesRef> index;

		public List<FST<BytesRef>> subIndices;

		public final boolean hasTerms;

		public final boolean isFloor;

		public final int floorLeadByte;

		public PendingBlock(BytesRef prefix, long fp, boolean hasTerms, boolean isFloor, int floorLeadByte, List<FST<BytesRef>> subIndices) {
			super(false);
			this.prefix = prefix;
			this.fp = fp;
			this.hasTerms = hasTerms;
			this.isFloor = isFloor;
			this.floorLeadByte = floorLeadByte;
			this.subIndices = subIndices;
		}

		@Override
		public String toString() {
			return "BLOCK: prefix=" + (BlockTreeTermsWriter.brToString(prefix));
		}

		public void compileIndex(List<BlockTreeTermsWriter.PendingBlock> blocks, RAMOutputStream scratchBytes, IntsRefBuilder scratchIntsRef) throws IOException {
			assert ((isFloor) && ((blocks.size()) > 1)) || (((isFloor) == false) && ((blocks.size()) == 1)) : (("isFloor=" + (isFloor)) + " blocks=") + blocks;
			assert (this) == (blocks.get(0));
			assert (scratchBytes.getFilePointer()) == 0;
			scratchBytes.writeVLong(BlockTreeTermsWriter.encodeOutput(fp, hasTerms, isFloor));
			if (isFloor) {
				scratchBytes.writeVInt(((blocks.size()) - 1));
				for (int i = 1; i < (blocks.size()); i++) {
					BlockTreeTermsWriter.PendingBlock sub = blocks.get(i);
					assert (sub.floorLeadByte) != (-1);
					scratchBytes.writeByte(((byte) (sub.floorLeadByte)));
					assert (sub.fp) > (fp);
					scratchBytes.writeVLong(((((sub.fp) - (fp)) << 1) | (sub.hasTerms ? 1 : 0)));
				}
			}
			final ByteSequenceOutputs outputs = ByteSequenceOutputs.getSingleton();
			final Builder<BytesRef> indexBuilder = new Builder<>(BYTE1, 0, 0, true, false, Integer.MAX_VALUE, outputs, true, 15);
			final byte[] bytes = new byte[((int) (scratchBytes.getFilePointer()))];
			assert (bytes.length) > 0;
			scratchBytes.writeTo(bytes, 0);
			indexBuilder.add(Util.toIntsRef(prefix, scratchIntsRef), new BytesRef(bytes, 0, bytes.length));
			scratchBytes.reset();
			for (BlockTreeTermsWriter.PendingBlock block : blocks) {
				if ((block.subIndices) != null) {
					for (FST<BytesRef> subIndex : block.subIndices) {
						append(indexBuilder, subIndex, scratchIntsRef);
					}
					block.subIndices = null;
				}
			}
			index = indexBuilder.finish();
			assert (subIndices) == null;
		}

		private void append(Builder<BytesRef> builder, FST<BytesRef> subIndex, IntsRefBuilder scratchIntsRef) throws IOException {
			final BytesRefFSTEnum<BytesRef> subIndexEnum = new BytesRefFSTEnum<>(subIndex);
			BytesRefFSTEnum.InputOutput<BytesRef> indexEnt;
			while ((indexEnt = subIndexEnum.next()) != null) {
				builder.add(Util.toIntsRef(indexEnt.input, scratchIntsRef), indexEnt.output);
			} 
		}
	}

	private final RAMOutputStream scratchBytes = new RAMOutputStream();

	private final IntsRefBuilder scratchIntsRef = new IntsRefBuilder();

	static final BytesRef EMPTY_BYTES_REF = new BytesRef();

	class TermsWriter {
		private final FieldInfo fieldInfo;

		private final int longsSize;

		private long numTerms;

		final FixedBitSet docsSeen;

		long sumTotalTermFreq;

		long sumDocFreq;

		long indexStartFP;

		private final BytesRefBuilder lastTerm = new BytesRefBuilder();

		private int[] prefixStarts = new int[8];

		private final long[] longs;

		private final List<BlockTreeTermsWriter.PendingEntry> pending = new ArrayList<>();

		private final List<BlockTreeTermsWriter.PendingBlock> newBlocks = new ArrayList<>();

		private BlockTreeTermsWriter.PendingTerm firstPendingTerm;

		private BlockTreeTermsWriter.PendingTerm lastPendingTerm;

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
				BlockTreeTermsWriter.PendingEntry ent = pending.get(i);
				int suffixLeadLabel;
				if (ent.isTerm) {
					BlockTreeTermsWriter.PendingTerm term = ((BlockTreeTermsWriter.PendingTerm) (ent));
					if ((term.termBytes.length) == prefixLength) {
						assert lastSuffixLeadLabel == (-1) : (("i=" + i) + " lastSuffixLeadLabel=") + lastSuffixLeadLabel;
						suffixLeadLabel = -1;
					}else {
						suffixLeadLabel = (term.termBytes[prefixLength]) & 255;
					}
				}else {
					BlockTreeTermsWriter.PendingBlock block = ((BlockTreeTermsWriter.PendingBlock) (ent));
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
			BlockTreeTermsWriter.PendingBlock firstBlock = newBlocks.get(0);
			assert (firstBlock.isFloor) || ((newBlocks.size()) == 1);
			firstBlock.compileIndex(newBlocks, scratchBytes, scratchIntsRef);
			pending.subList(((pending.size()) - count), pending.size()).clear();
			pending.add(firstBlock);
			newBlocks.clear();
		}

		private BlockTreeTermsWriter.PendingBlock writeBlock(int prefixLength, boolean isFloor, int floorLeadLabel, int start, int end, boolean hasTerms, boolean hasSubBlocks) throws IOException {
			assert end > start;
			long startFP = termsOut.getFilePointer();
			boolean hasFloorLeadLabel = isFloor && (floorLeadLabel != (-1));
			final BytesRef prefix = new BytesRef((prefixLength + (hasFloorLeadLabel ? 1 : 0)));
			System.arraycopy(lastTerm.get().bytes, 0, prefix.bytes, 0, prefixLength);
			prefix.length = prefixLength;
			int numEntries = end - start;
			int code = numEntries << 1;
			if (end == (pending.size())) {
				code |= 1;
			}
			termsOut.writeVInt(code);
			boolean isLeafBlock = hasSubBlocks == false;
			final List<FST<BytesRef>> subIndices;
			boolean absolute = true;
			if (isLeafBlock) {
				subIndices = null;
				for (int i = start; i < end; i++) {
					BlockTreeTermsWriter.PendingEntry ent = pending.get(i);
					assert ent.isTerm : "i=" + i;
					BlockTreeTermsWriter.PendingTerm term = ((BlockTreeTermsWriter.PendingTerm) (ent));
					assert StringHelper.startsWith(term.termBytes, prefix) : (("term.term=" + (term.termBytes)) + " prefix=") + prefix;
					BlockTermState state = term.state;
					final int suffix = (term.termBytes.length) - prefixLength;
					suffixWriter.writeVInt(suffix);
					suffixWriter.writeBytes(term.termBytes, prefixLength, suffix);
					assert (floorLeadLabel == (-1)) || (((term.termBytes[prefixLength]) & 255) >= floorLeadLabel);
					statsWriter.writeVInt(state.docFreq);
					if ((fieldInfo.getIndexOptions()) != (IndexOptions.DOCS)) {
						assert (state.totalTermFreq) >= (state.docFreq) : ((state.totalTermFreq) + " vs ") + (state.docFreq);
						statsWriter.writeVLong(((state.totalTermFreq) - (state.docFreq)));
					}
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
					BlockTreeTermsWriter.PendingEntry ent = pending.get(i);
					if (ent.isTerm) {
						BlockTreeTermsWriter.PendingTerm term = ((BlockTreeTermsWriter.PendingTerm) (ent));
						assert StringHelper.startsWith(term.termBytes, prefix) : (("term.term=" + (term.termBytes)) + " prefix=") + prefix;
						BlockTermState state = term.state;
						final int suffix = (term.termBytes.length) - prefixLength;
						suffixWriter.writeVInt((suffix << 1));
						suffixWriter.writeBytes(term.termBytes, prefixLength, suffix);
						statsWriter.writeVInt(state.docFreq);
						if ((fieldInfo.getIndexOptions()) != (IndexOptions.DOCS)) {
							assert (state.totalTermFreq) >= (state.docFreq);
							statsWriter.writeVLong(((state.totalTermFreq) - (state.docFreq)));
						}
						postingsWriter.encodeTerm(longs, bytesWriter, fieldInfo, state, absolute);
						for (int pos = 0; pos < (longsSize); pos++) {
							assert (longs[pos]) >= 0;
							metaWriter.writeVLong(longs[pos]);
						}
						bytesWriter.writeTo(metaWriter);
						bytesWriter.reset();
						absolute = false;
					}else {
						BlockTreeTermsWriter.PendingBlock block = ((BlockTreeTermsWriter.PendingBlock) (ent));
						assert StringHelper.startsWith(block.prefix, prefix);
						final int suffix = (block.prefix.length) - prefixLength;
						assert StringHelper.startsWith(block.prefix, prefix);
						assert suffix > 0;
						suffixWriter.writeVInt(((suffix << 1) | 1));
						suffixWriter.writeBytes(block.prefix.bytes, prefixLength, suffix);
						assert (floorLeadLabel == (-1)) || (((block.prefix.bytes[prefixLength]) & 255) >= floorLeadLabel) : (("floorLeadLabel=" + floorLeadLabel) + " suffixLead=") + ((block.prefix.bytes[prefixLength]) & 255);
						assert (block.fp) < startFP;
						suffixWriter.writeVLong((startFP - (block.fp)));
						subIndices.add(block.index);
					}
				}
				assert (subIndices.size()) != 0;
			}
			termsOut.writeVInt((((int) ((suffixWriter.getFilePointer()) << 1)) | (isLeafBlock ? 1 : 0)));
			suffixWriter.writeTo(termsOut);
			suffixWriter.reset();
			termsOut.writeVInt(((int) (statsWriter.getFilePointer())));
			statsWriter.writeTo(termsOut);
			statsWriter.reset();
			termsOut.writeVInt(((int) (metaWriter.getFilePointer())));
			metaWriter.writeTo(termsOut);
			metaWriter.reset();
			if (hasFloorLeadLabel) {
				prefix.bytes[((prefix.length)++)] = ((byte) (floorLeadLabel));
			}
			return new BlockTreeTermsWriter.PendingBlock(prefix, startFP, hasTerms, isFloor, floorLeadLabel, subIndices);
		}

		TermsWriter(FieldInfo fieldInfo) {
			this.fieldInfo = fieldInfo;
			assert (fieldInfo.getIndexOptions()) != (IndexOptions.NONE);
			docsSeen = new FixedBitSet(maxDoc);
			this.longsSize = postingsWriter.setField(fieldInfo);
			this.longs = new long[longsSize];
		}

		public void write(BytesRef text, TermsEnum termsEnum) throws IOException {
			BlockTermState state = postingsWriter.writeTerm(text, termsEnum, docsSeen);
			if (state != null) {
				assert (state.docFreq) != 0;
				assert ((fieldInfo.getIndexOptions()) == (IndexOptions.DOCS)) || ((state.totalTermFreq) >= (state.docFreq)) : "postingsWriter=" + (postingsWriter);
				pushTerm(text);
				BlockTreeTermsWriter.PendingTerm term = new BlockTreeTermsWriter.PendingTerm(text, state);
				pending.add(term);
				sumDocFreq += state.docFreq;
				sumTotalTermFreq += state.totalTermFreq;
				(numTerms)++;
				if ((firstPendingTerm) == null) {
					firstPendingTerm = term;
				}
				lastPendingTerm = term;
			}
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
				pushTerm(new BytesRef());
				pushTerm(new BytesRef());
				writeBlocks(0, pending.size());
				assert ((pending.size()) == 1) && (!(pending.get(0).isTerm)) : (("pending.size()=" + (pending.size())) + " pending=") + (pending);
				final BlockTreeTermsWriter.PendingBlock root = ((BlockTreeTermsWriter.PendingBlock) (pending.get(0)));
				assert (root.prefix.length) == 0;
				assert (root.index.getEmptyOutput()) != null;
				indexStartFP = indexOut.getFilePointer();
				root.index.save(indexOut);
				assert (firstPendingTerm) != null;
				BytesRef minTerm = new BytesRef(firstPendingTerm.termBytes);
				assert (lastPendingTerm) != null;
				BytesRef maxTerm = new BytesRef(lastPendingTerm.termBytes);
				fields.add(new BlockTreeTermsWriter.FieldMetaData(fieldInfo, ((BlockTreeTermsWriter.PendingBlock) (pending.get(0))).index.getEmptyOutput(), numTerms, indexStartFP, sumTotalTermFreq, sumDocFreq, docsSeen.cardinality(), longsSize, minTerm, maxTerm));
			}else {
				assert ((sumTotalTermFreq) == 0) || (((fieldInfo.getIndexOptions()) == (IndexOptions.DOCS)) && ((sumTotalTermFreq) == (-1)));
				assert (sumDocFreq) == 0;
				assert (docsSeen.cardinality()) == 0;
			}
		}

		private final RAMOutputStream suffixWriter = new RAMOutputStream();

		private final RAMOutputStream statsWriter = new RAMOutputStream();

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
			final long dirStart = termsOut.getFilePointer();
			final long indexDirStart = indexOut.getFilePointer();
			termsOut.writeVInt(fields.size());
			for (BlockTreeTermsWriter.FieldMetaData field : fields) {
				termsOut.writeVInt(field.fieldInfo.number);
				assert (field.numTerms) > 0;
				termsOut.writeVLong(field.numTerms);
				termsOut.writeVInt(field.rootCode.length);
				termsOut.writeBytes(field.rootCode.bytes, field.rootCode.offset, field.rootCode.length);
				assert (field.fieldInfo.getIndexOptions()) != (IndexOptions.NONE);
				if ((field.fieldInfo.getIndexOptions()) != (IndexOptions.DOCS)) {
					termsOut.writeVLong(field.sumTotalTermFreq);
				}
				termsOut.writeVLong(field.sumDocFreq);
				termsOut.writeVInt(field.docCount);
				termsOut.writeVInt(field.longsSize);
				indexOut.writeVLong(field.indexStartFP);
				BlockTreeTermsWriter.writeBytesRef(termsOut, field.minTerm);
				BlockTreeTermsWriter.writeBytesRef(termsOut, field.maxTerm);
			}
			writeTrailer(termsOut, dirStart);
			CodecUtil.writeFooter(termsOut);
			writeIndexTrailer(indexOut, indexDirStart);
			CodecUtil.writeFooter(indexOut);
			success = true;
		} finally {
			if (success) {
				IOUtils.close(termsOut, indexOut, postingsWriter);
			}else {
				IOUtils.closeWhileHandlingException(termsOut, indexOut, postingsWriter);
			}
		}
	}

	private static void writeBytesRef(IndexOutput out, BytesRef bytes) throws IOException {
		out.writeVInt(bytes.length);
		out.writeBytes(bytes.bytes, bytes.offset, bytes.length);
	}
}

