

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.codecs.PostingsReaderBase;
import org.apache.lucene.codecs.memory.FSTOrdTermsWriter;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.store.ChecksumIndexInput;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.Accountables;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.RamUsageEstimator;
import org.apache.lucene.util.automaton.ByteRunAutomaton;
import org.apache.lucene.util.automaton.CompiledAutomaton;
import org.apache.lucene.util.automaton.RunAutomaton;
import org.apache.lucene.util.fst.BytesRefFSTEnum;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.FST.Arc;
import org.apache.lucene.util.fst.Outputs;
import org.apache.lucene.util.fst.PositiveIntOutputs;
import org.apache.lucene.util.fst.Util;

import static org.apache.lucene.index.TermsEnum.SeekStatus.END;
import static org.apache.lucene.index.TermsEnum.SeekStatus.FOUND;
import static org.apache.lucene.index.TermsEnum.SeekStatus.NOT_FOUND;
import static org.apache.lucene.util.automaton.CompiledAutomaton.AUTOMATON_TYPE.NORMAL;


public class FSTOrdTermsReader extends FieldsProducer {
	static final int INTERVAL = FSTOrdTermsWriter.SKIP_INTERVAL;

	final TreeMap<String, FSTOrdTermsReader.TermsReader> fields = new TreeMap<>();

	final PostingsReaderBase postingsReader;

	public FSTOrdTermsReader(SegmentReadState state, PostingsReaderBase postingsReader) throws IOException {
		this.postingsReader = postingsReader;
		ChecksumIndexInput indexIn = null;
		IndexInput blockIn = null;
		boolean success = false;
		try {
			CodecUtil.checksumEntireFile(blockIn);
			this.postingsReader.init(blockIn, state);
			seekDir(blockIn);
			final FieldInfos fieldInfos = state.fieldInfos;
			final int numFields = blockIn.readVInt();
			for (int i = 0; i < numFields; i++) {
				FieldInfo fieldInfo = fieldInfos.fieldInfo(blockIn.readVInt());
				boolean hasFreq = (fieldInfo.getIndexOptions()) != (IndexOptions.DOCS);
				long numTerms = blockIn.readVLong();
				long sumTotalTermFreq = (hasFreq) ? blockIn.readVLong() : -1;
				long sumDocFreq = blockIn.readVLong();
				int docCount = blockIn.readVInt();
				int longsSize = blockIn.readVInt();
				FST<Long> index = new FST<>(indexIn, PositiveIntOutputs.getSingleton());
				FSTOrdTermsReader.TermsReader current = new FSTOrdTermsReader.TermsReader(fieldInfo, blockIn, numTerms, sumTotalTermFreq, sumDocFreq, docCount, longsSize, index);
				FSTOrdTermsReader.TermsReader previous = fields.put(fieldInfo.name, current);
				checkFieldSummary(state.segmentInfo, indexIn, blockIn, current, previous);
			}
			CodecUtil.checkFooter(indexIn);
			success = true;
		} finally {
			if (success) {
				IOUtils.close(indexIn, blockIn);
			}else {
				IOUtils.closeWhileHandlingException(indexIn, blockIn);
			}
		}
	}

	private void seekDir(IndexInput in) throws IOException {
		in.seek((((in.length()) - (CodecUtil.footerLength())) - 8));
		in.seek(in.readLong());
	}

	private void checkFieldSummary(SegmentInfo info, IndexInput indexIn, IndexInput blockIn, FSTOrdTermsReader.TermsReader field, FSTOrdTermsReader.TermsReader previous) throws IOException {
		if (((field.docCount) < 0) || ((field.docCount) > (info.maxDoc()))) {
			throw new CorruptIndexException((((((("invalid docCount: " + (field.docCount)) + " maxDoc: ") + (info.maxDoc())) + " (blockIn=") + blockIn) + ")"), indexIn);
		}
		if ((field.sumDocFreq) < (field.docCount)) {
			throw new CorruptIndexException((((((("invalid sumDocFreq: " + (field.sumDocFreq)) + " docCount: ") + (field.docCount)) + " (blockIn=") + blockIn) + ")"), indexIn);
		}
		if (((field.sumTotalTermFreq) != (-1)) && ((field.sumTotalTermFreq) < (field.sumDocFreq))) {
			throw new CorruptIndexException((((((("invalid sumTotalTermFreq: " + (field.sumTotalTermFreq)) + " sumDocFreq: ") + (field.sumDocFreq)) + " (blockIn=") + blockIn) + ")"), indexIn);
		}
		if (previous != null) {
			throw new CorruptIndexException((((("duplicate fields: " + (field.fieldInfo.name)) + " (blockIn=") + blockIn) + ")"), indexIn);
		}
	}

	@Override
	public Iterator<String> iterator() {
		return Collections.unmodifiableSet(fields.keySet()).iterator();
	}

	@Override
	public Terms terms(String field) throws IOException {
		assert field != null;
		return fields.get(field);
	}

	@Override
	public int size() {
		return fields.size();
	}

	@Override
	public void close() throws IOException {
		try {
			IOUtils.close(postingsReader);
		} finally {
			fields.clear();
		}
	}

	final class TermsReader extends Terms implements Accountable {
		final FieldInfo fieldInfo;

		final long numTerms;

		final long sumTotalTermFreq;

		final long sumDocFreq;

		final int docCount;

		final int longsSize;

		final FST<Long> index;

		final int numSkipInfo;

		final long[] skipInfo;

		final byte[] statsBlock;

		final byte[] metaLongsBlock;

		final byte[] metaBytesBlock;

		TermsReader(FieldInfo fieldInfo, IndexInput blockIn, long numTerms, long sumTotalTermFreq, long sumDocFreq, int docCount, int longsSize, FST<Long> index) throws IOException {
			this.fieldInfo = fieldInfo;
			this.numTerms = numTerms;
			this.sumTotalTermFreq = sumTotalTermFreq;
			this.sumDocFreq = sumDocFreq;
			this.docCount = docCount;
			this.longsSize = longsSize;
			this.index = index;
			assert (numTerms & (~4294967295L)) == 0;
			final int numBlocks = ((int) ((numTerms + (FSTOrdTermsReader.INTERVAL)) - 1)) / (FSTOrdTermsReader.INTERVAL);
			this.numSkipInfo = longsSize + 3;
			this.skipInfo = new long[numBlocks * (numSkipInfo)];
			this.statsBlock = new byte[((int) (blockIn.readVLong()))];
			this.metaLongsBlock = new byte[((int) (blockIn.readVLong()))];
			this.metaBytesBlock = new byte[((int) (blockIn.readVLong()))];
			int last = 0;
			int next = 0;
			for (int i = 1; i < numBlocks; i++) {
				next = (numSkipInfo) * i;
				for (int j = 0; j < (numSkipInfo); j++) {
					skipInfo[(next + j)] = (skipInfo[(last + j)]) + (blockIn.readVLong());
				}
				last = next;
			}
			blockIn.readBytes(statsBlock, 0, statsBlock.length);
			blockIn.readBytes(metaLongsBlock, 0, metaLongsBlock.length);
			blockIn.readBytes(metaBytesBlock, 0, metaBytesBlock.length);
		}

		public boolean hasFreqs() {
			return (fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS)) >= 0;
		}

		@Override
		public boolean hasOffsets() {
			return (fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS)) >= 0;
		}

		@Override
		public boolean hasPositions() {
			return (fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS)) >= 0;
		}

		@Override
		public boolean hasPayloads() {
			return fieldInfo.hasPayloads();
		}

		@Override
		public long size() {
			return numTerms;
		}

		@Override
		public long getSumTotalTermFreq() {
			return sumTotalTermFreq;
		}

		@Override
		public long getSumDocFreq() throws IOException {
			return sumDocFreq;
		}

		@Override
		public int getDocCount() throws IOException {
			return docCount;
		}

		@Override
		public TermsEnum iterator() throws IOException {
			return new FSTOrdTermsReader.TermsReader.SegmentTermsEnum();
		}

		@Override
		public TermsEnum intersect(CompiledAutomaton compiled, BytesRef startTerm) throws IOException {
			if ((compiled.type) != (NORMAL)) {
				throw new IllegalArgumentException("please use CompiledAutomaton.getTermsEnum instead");
			}
			return new FSTOrdTermsReader.TermsReader.IntersectTermsEnum(compiled, startTerm);
		}

		@Override
		public long ramBytesUsed() {
			long ramBytesUsed = 0;
			if ((index) != null) {
				ramBytesUsed += index.ramBytesUsed();
				ramBytesUsed += RamUsageEstimator.sizeOf(metaBytesBlock);
				ramBytesUsed += RamUsageEstimator.sizeOf(metaLongsBlock);
				ramBytesUsed += RamUsageEstimator.sizeOf(skipInfo);
				ramBytesUsed += RamUsageEstimator.sizeOf(statsBlock);
			}
			return ramBytesUsed;
		}

		@Override
		public Collection<Accountable> getChildResources() {
			if ((index) == null) {
				return Collections.emptyList();
			}else {
				return Collections.singletonList(Accountables.namedAccountable("terms", index));
			}
		}

		@Override
		public String toString() {
			return ((((((("FSTOrdTerms(terms=" + (numTerms)) + ",postings=") + (sumDocFreq)) + ",positions=") + (sumTotalTermFreq)) + ",docs=") + (docCount)) + ")";
		}

		abstract class BaseTermsEnum extends TermsEnum {
			long ord;

			final BlockTermState state;

			final ByteArrayDataInput statsReader = new ByteArrayDataInput();

			final ByteArrayDataInput metaLongsReader = new ByteArrayDataInput();

			final ByteArrayDataInput metaBytesReader = new ByteArrayDataInput();

			int statsBlockOrd;

			int metaBlockOrd;

			long[][] longs;

			int[] bytesStart;

			int[] bytesLength;

			int[] docFreq;

			long[] totalTermFreq;

			BaseTermsEnum() throws IOException {
				this.state = postingsReader.newTermState();
				this.statsReader.reset(statsBlock);
				this.metaLongsReader.reset(metaLongsBlock);
				this.metaBytesReader.reset(metaBytesBlock);
				this.longs = new long[FSTOrdTermsReader.INTERVAL][longsSize];
				this.bytesStart = new int[FSTOrdTermsReader.INTERVAL];
				this.bytesLength = new int[FSTOrdTermsReader.INTERVAL];
				this.docFreq = new int[FSTOrdTermsReader.INTERVAL];
				this.totalTermFreq = new long[FSTOrdTermsReader.INTERVAL];
				this.statsBlockOrd = -1;
				this.metaBlockOrd = -1;
				if (!(hasFreqs())) {
					Arrays.fill(totalTermFreq, (-1));
				}
			}

			void decodeStats() throws IOException {
				final int upto = ((int) (ord)) % (FSTOrdTermsReader.INTERVAL);
				final int oldBlockOrd = statsBlockOrd;
				statsBlockOrd = ((int) (ord)) / (FSTOrdTermsReader.INTERVAL);
				if (oldBlockOrd != (statsBlockOrd)) {
					refillStats();
				}
				state.docFreq = docFreq[upto];
				state.totalTermFreq = totalTermFreq[upto];
			}

			void decodeMetaData() throws IOException {
				final int upto = ((int) (ord)) % (FSTOrdTermsReader.INTERVAL);
				final int oldBlockOrd = metaBlockOrd;
				metaBlockOrd = ((int) (ord)) / (FSTOrdTermsReader.INTERVAL);
				if ((metaBlockOrd) != oldBlockOrd) {
					refillMetadata();
				}
				metaBytesReader.setPosition(bytesStart[upto]);
				postingsReader.decodeTerm(longs[upto], metaBytesReader, fieldInfo, state, true);
			}

			final void refillStats() throws IOException {
				final int offset = (statsBlockOrd) * (numSkipInfo);
				final int statsFP = ((int) (skipInfo[offset]));
				statsReader.setPosition(statsFP);
				for (int i = 0; (i < (FSTOrdTermsReader.INTERVAL)) && (!(statsReader.eof())); i++) {
					int code = statsReader.readVInt();
					if (hasFreqs()) {
						docFreq[i] = code >>> 1;
						if ((code & 1) == 1) {
							totalTermFreq[i] = docFreq[i];
						}else {
							totalTermFreq[i] = (docFreq[i]) + (statsReader.readVLong());
						}
					}else {
						docFreq[i] = code;
					}
				}
			}

			final void refillMetadata() throws IOException {
				final int offset = (metaBlockOrd) * (numSkipInfo);
				final int metaLongsFP = ((int) (skipInfo[(offset + 1)]));
				final int metaBytesFP = ((int) (skipInfo[(offset + 2)]));
				metaLongsReader.setPosition(metaLongsFP);
				for (int j = 0; j < (longsSize); j++) {
					longs[0][j] = (skipInfo[((offset + 3) + j)]) + (metaLongsReader.readVLong());
				}
				bytesStart[0] = metaBytesFP;
				bytesLength[0] = ((int) (metaLongsReader.readVLong()));
				for (int i = 1; (i < (FSTOrdTermsReader.INTERVAL)) && (!(metaLongsReader.eof())); i++) {
					for (int j = 0; j < (longsSize); j++) {
						longs[i][j] = (longs[(i - 1)][j]) + (metaLongsReader.readVLong());
					}
					bytesStart[i] = (bytesStart[(i - 1)]) + (bytesLength[(i - 1)]);
					bytesLength[i] = ((int) (metaLongsReader.readVLong()));
				}
			}

			@Override
			public TermState termState() throws IOException {
				decodeMetaData();
				return state.clone();
			}

			@Override
			public int docFreq() throws IOException {
				return state.docFreq;
			}

			@Override
			public long totalTermFreq() throws IOException {
				return state.totalTermFreq;
			}

			@Override
			public PostingsEnum postings(PostingsEnum reuse, int flags) throws IOException {
				decodeMetaData();
				return postingsReader.postings(fieldInfo, state, reuse, flags);
			}

			@Override
			public void seekExact(long ord) throws IOException {
				throw new UnsupportedOperationException();
			}

			@Override
			public long ord() {
				throw new UnsupportedOperationException();
			}
		}

		private final class SegmentTermsEnum extends FSTOrdTermsReader.TermsReader.BaseTermsEnum {
			final BytesRefFSTEnum<Long> fstEnum;

			BytesRef term;

			boolean decoded;

			boolean seekPending;

			SegmentTermsEnum() throws IOException {
				this.fstEnum = new BytesRefFSTEnum<>(index);
				this.decoded = false;
				this.seekPending = false;
			}

			@Override
			public BytesRef term() throws IOException {
				return term;
			}

			@Override
			void decodeMetaData() throws IOException {
				if ((!(decoded)) && (!(seekPending))) {
					super.decodeMetaData();
					decoded = true;
				}
			}

			void updateEnum(final BytesRefFSTEnum.InputOutput<Long> pair) throws IOException {
				if (pair == null) {
					term = null;
				}else {
					term = pair.input;
					ord = pair.output;
					decodeStats();
				}
				decoded = false;
				seekPending = false;
			}

			@Override
			public BytesRef next() throws IOException {
				if (seekPending) {
					seekPending = false;
					TermsEnum.SeekStatus status = seekCeil(term);
					assert status == (FOUND);
				}
				updateEnum(fstEnum.next());
				return term;
			}

			@Override
			public boolean seekExact(BytesRef target) throws IOException {
				updateEnum(fstEnum.seekExact(target));
				return (term) != null;
			}

			@Override
			public TermsEnum.SeekStatus seekCeil(BytesRef target) throws IOException {
				updateEnum(fstEnum.seekCeil(target));
				if ((term) == null) {
					return END;
				}else {
					return term.equals(target) ? FOUND : NOT_FOUND;
				}
			}

			@Override
			public void seekExact(BytesRef target, TermState otherState) {
				if (!(target.equals(term))) {
					state.copyFrom(otherState);
					term = BytesRef.deepCopyOf(target);
					seekPending = true;
				}
			}
		}

		private final class IntersectTermsEnum extends FSTOrdTermsReader.TermsReader.BaseTermsEnum {
			BytesRefBuilder term;

			boolean decoded;

			boolean pending;

			FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame[] stack;

			int level;

			final FST<Long> fst;

			final FST.BytesReader fstReader;

			final Outputs<Long> fstOutputs;

			final ByteRunAutomaton fsa;

			private final class Frame {
				FST.Arc<Long> arc;

				int state;

				Frame() {
					this.arc = new FST.Arc<>();
					this.state = -1;
				}

				public String toString() {
					return (("arc=" + (arc)) + " state=") + (state);
				}
			}

			IntersectTermsEnum(CompiledAutomaton compiled, BytesRef startTerm) throws IOException {
				this.fst = index;
				this.fstReader = fst.getBytesReader();
				this.fstOutputs = index.outputs;
				this.fsa = compiled.runAutomaton;
				this.level = -1;
				this.stack = new FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame[16];
				for (int i = 0; i < (stack.length); i++) {
					this.stack[i] = new FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame();
				}
				FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame frame;
				frame = loadVirtualFrame(newFrame());
				(this.level)++;
				frame = loadFirstFrame(newFrame());
				pushFrame(frame);
				this.decoded = false;
				this.pending = false;
				if (startTerm == null) {
					pending = isAccept(topFrame());
				}else {
					doSeekCeil(startTerm);
					pending = ((((term) == null) || (!(startTerm.equals(term.get())))) && (isValid(topFrame()))) && (isAccept(topFrame()));
				}
			}

			@Override
			public BytesRef term() throws IOException {
				return (term) == null ? null : term.get();
			}

			@Override
			void decodeMetaData() throws IOException {
				if (!(decoded)) {
					super.decodeMetaData();
					decoded = true;
				}
			}

			@Override
			void decodeStats() throws IOException {
				final FST.Arc<Long> arc = topFrame().arc;
				assert (arc.nextFinalOutput) == (fstOutputs.getNoOutput());
				ord = arc.output;
				super.decodeStats();
			}

			@Override
			public TermsEnum.SeekStatus seekCeil(BytesRef target) throws IOException {
				throw new UnsupportedOperationException();
			}

			@Override
			public BytesRef next() throws IOException {
				if (pending) {
					pending = false;
					decodeStats();
					return term();
				}
				decoded = false;
				DFS : while ((level) > 0) {
					FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame frame = newFrame();
					if ((loadExpandFrame(topFrame(), frame)) != null) {
						pushFrame(frame);
						if (isAccept(frame)) {
							break;
						}
						continue;
					}
					frame = popFrame();
					while ((level) > 0) {
						if ((loadNextFrame(topFrame(), frame)) != null) {
							pushFrame(frame);
							if (isAccept(frame)) {
								break DFS;
							}
							continue DFS;
						}
						frame = popFrame();
					} 
					return null;
				} 
				decodeStats();
				return term();
			}

			BytesRef doSeekCeil(BytesRef target) throws IOException {
				FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame frame = null;
				int label;
				int upto = 0;
				int limit = target.length;
				while (upto < limit) {
					frame = newFrame();
					label = (target.bytes[upto]) & 255;
					frame = loadCeilFrame(label, topFrame(), frame);
					if ((frame == null) || ((frame.arc.label) != label)) {
						break;
					}
					assert isValid(frame);
					pushFrame(frame);
					upto++;
				} 
				if (upto == limit) {
					return term();
				}
				if (frame != null) {
					pushFrame(frame);
					return isAccept(frame) ? term() : next();
				}
				while ((level) > 0) {
					frame = popFrame();
					while (((level) > 0) && (!(canRewind(frame)))) {
						frame = popFrame();
					} 
					if ((loadNextFrame(topFrame(), frame)) != null) {
						pushFrame(frame);
						return isAccept(frame) ? term() : next();
					}
				} 
				return null;
			}

			FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame loadVirtualFrame(FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame frame) throws IOException {
				frame.arc.output = fstOutputs.getNoOutput();
				frame.arc.nextFinalOutput = fstOutputs.getNoOutput();
				frame.state = -1;
				return frame;
			}

			FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame loadFirstFrame(FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame frame) throws IOException {
				frame.arc = fst.getFirstArc(frame.arc);
				frame.state = 0;
				return frame;
			}

			FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame loadExpandFrame(FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame top, FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame frame) throws IOException {
				if (!(canGrow(top))) {
					return null;
				}
				frame.arc = fst.readFirstRealTargetArc(top.arc.target, frame.arc, fstReader);
				frame.state = fsa.step(top.state, frame.arc.label);
				if ((frame.state) == (-1)) {
					return loadNextFrame(top, frame);
				}
				return frame;
			}

			FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame loadNextFrame(FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame top, FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame frame) throws IOException {
				if (!(canRewind(frame))) {
					return null;
				}
				while (!(frame.arc.isLast())) {
					frame.arc = fst.readNextRealArc(frame.arc, fstReader);
					frame.state = fsa.step(top.state, frame.arc.label);
					if ((frame.state) != (-1)) {
						break;
					}
				} 
				if ((frame.state) == (-1)) {
					return null;
				}
				return frame;
			}

			FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame loadCeilFrame(int label, FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame top, FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame frame) throws IOException {
				FST.Arc<Long> arc = frame.arc;
				arc = Util.readCeilArc(label, fst, top.arc, arc, fstReader);
				if (arc == null) {
					return null;
				}
				frame.state = fsa.step(top.state, arc.label);
				if ((frame.state) == (-1)) {
					return loadNextFrame(top, frame);
				}
				return frame;
			}

			boolean isAccept(FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame frame) {
				return (fsa.isAccept(frame.state)) && (frame.arc.isFinal());
			}

			boolean isValid(FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame frame) {
				return (frame.state) != (-1);
			}

			boolean canGrow(FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame frame) {
				return ((frame.state) != (-1)) && (FST.targetHasArcs(frame.arc));
			}

			boolean canRewind(FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame frame) {
				return !(frame.arc.isLast());
			}

			void pushFrame(FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame frame) {
				final FST.Arc<Long> arc = frame.arc;
				arc.output = fstOutputs.add(topFrame().arc.output, arc.output);
				term = grow(arc.label);
				(level)++;
				assert frame == (stack[level]);
			}

			FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame popFrame() {
				term = shrink();
				return stack[((level)--)];
			}

			FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame newFrame() {
				if (((level) + 1) == (stack.length)) {
					final FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame[] temp = new FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame[ArrayUtil.oversize(((level) + 2), RamUsageEstimator.NUM_BYTES_OBJECT_REF)];
					System.arraycopy(stack, 0, temp, 0, stack.length);
					for (int i = stack.length; i < (temp.length); i++) {
						temp[i] = new FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame();
					}
					stack = temp;
				}
				return stack[((level) + 1)];
			}

			FSTOrdTermsReader.TermsReader.IntersectTermsEnum.Frame topFrame() {
				return stack[level];
			}

			BytesRefBuilder grow(int label) {
				if ((term) == null) {
					term = new BytesRefBuilder();
				}else {
					term.append(((byte) (label)));
				}
				return term;
			}

			BytesRefBuilder shrink() {
				if ((term.length()) == 0) {
					term = null;
				}else {
					term.setLength(((term.length()) - 1));
				}
				return term;
			}
		}
	}

	static <T> void walk(FST<T> fst) throws IOException {
		final ArrayList<FST.Arc<T>> queue = new ArrayList<>();
		final BitSet seen = new BitSet();
		final FST.BytesReader reader = fst.getBytesReader();
		final FST.Arc<T> startArc = fst.getFirstArc(new FST.Arc<T>());
		queue.add(startArc);
		while (!(queue.isEmpty())) {
			final FST.Arc<T> arc = queue.remove(0);
			final long node = arc.target;
			if ((FST.targetHasArcs(arc)) && (!(seen.get(((int) (node)))))) {
				seen.set(((int) (node)));
				fst.readFirstRealTargetArc(node, arc, reader);
				while (true) {
					queue.add(new FST.Arc<T>().copyFrom(arc));
					if (arc.isLast()) {
						break;
					}else {
						fst.readNextRealArc(arc, reader);
					}
				} 
			}
		} 
	}

	@Override
	public long ramBytesUsed() {
		long ramBytesUsed = postingsReader.ramBytesUsed();
		for (FSTOrdTermsReader.TermsReader r : fields.values()) {
			ramBytesUsed += r.ramBytesUsed();
		}
		return ramBytesUsed;
	}

	@Override
	public Collection<Accountable> getChildResources() {
		List<Accountable> resources = new ArrayList<>();
		resources.addAll(Accountables.namedAccountables("field", fields));
		resources.add(Accountables.namedAccountable("delegate", postingsReader));
		return Collections.unmodifiableList(resources);
	}

	@Override
	public String toString() {
		return (((((getClass().getSimpleName()) + "(fields=") + (fields.size())) + ",delegate=") + (postingsReader)) + ")";
	}

	@Override
	public void checkIntegrity() throws IOException {
		postingsReader.checkIntegrity();
	}
}

