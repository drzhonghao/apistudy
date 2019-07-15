

import java.io.IOException;
import java.util.ArrayList;
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
import org.apache.lucene.codecs.blockterms.TermsIndexReaderBase;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.OrdTermState;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.Accountables;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.RamUsageEstimator;

import static org.apache.lucene.index.TermsEnum.SeekStatus.END;
import static org.apache.lucene.index.TermsEnum.SeekStatus.FOUND;
import static org.apache.lucene.index.TermsEnum.SeekStatus.NOT_FOUND;


public class BlockTermsReader extends FieldsProducer {
	private static final long BASE_RAM_BYTES_USED = RamUsageEstimator.shallowSizeOfInstance(BlockTermsReader.class);

	private final IndexInput in = null;

	private final PostingsReaderBase postingsReader;

	private final TreeMap<String, BlockTermsReader.FieldReader> fields = new TreeMap<>();

	private TermsIndexReaderBase indexReader;

	private static class FieldAndTerm implements Cloneable {
		String field;

		BytesRef term;

		public FieldAndTerm() {
		}

		public FieldAndTerm(BlockTermsReader.FieldAndTerm other) {
			field = other.field;
			term = BytesRef.deepCopyOf(other.term);
		}

		@Override
		public boolean equals(Object _other) {
			BlockTermsReader.FieldAndTerm other = ((BlockTermsReader.FieldAndTerm) (_other));
			return (other.field.equals(field)) && (term.bytesEquals(other.term));
		}

		@Override
		public BlockTermsReader.FieldAndTerm clone() {
			return new BlockTermsReader.FieldAndTerm(this);
		}

		@Override
		public int hashCode() {
			return ((field.hashCode()) * 31) + (term.hashCode());
		}
	}

	public BlockTermsReader(TermsIndexReaderBase indexReader, PostingsReaderBase postingsReader, SegmentReadState state) throws IOException {
		this.postingsReader = postingsReader;
		boolean success = false;
		try {
			postingsReader.init(in, state);
			CodecUtil.retrieveChecksum(in);
			seekDir(in);
			final int numFields = in.readVInt();
			if (numFields < 0) {
				throw new CorruptIndexException(("invalid number of fields: " + numFields), in);
			}
			for (int i = 0; i < numFields; i++) {
				final int field = in.readVInt();
				final long numTerms = in.readVLong();
				assert numTerms >= 0;
				final long termsStartPointer = in.readVLong();
				final FieldInfo fieldInfo = state.fieldInfos.fieldInfo(field);
				final long sumTotalTermFreq = ((fieldInfo.getIndexOptions()) == (IndexOptions.DOCS)) ? -1 : in.readVLong();
				final long sumDocFreq = in.readVLong();
				final int docCount = in.readVInt();
				final int longsSize = in.readVInt();
				if ((docCount < 0) || (docCount > (state.segmentInfo.maxDoc()))) {
					throw new CorruptIndexException(((("invalid docCount: " + docCount) + " maxDoc: ") + (state.segmentInfo.maxDoc())), in);
				}
				if (sumDocFreq < docCount) {
					throw new CorruptIndexException(((("invalid sumDocFreq: " + sumDocFreq) + " docCount: ") + docCount), in);
				}
				if ((sumTotalTermFreq != (-1)) && (sumTotalTermFreq < sumDocFreq)) {
					throw new CorruptIndexException(((("invalid sumTotalTermFreq: " + sumTotalTermFreq) + " sumDocFreq: ") + sumDocFreq), in);
				}
				BlockTermsReader.FieldReader previous = fields.put(fieldInfo.name, new BlockTermsReader.FieldReader(fieldInfo, numTerms, termsStartPointer, sumTotalTermFreq, sumDocFreq, docCount, longsSize));
				if (previous != null) {
					throw new CorruptIndexException(("duplicate fields: " + (fieldInfo.name)), in);
				}
			}
			success = true;
		} finally {
			if (!success) {
				in.close();
			}
		}
		this.indexReader = indexReader;
	}

	private void seekDir(IndexInput input) throws IOException {
		input.seek((((input.length()) - (CodecUtil.footerLength())) - 8));
		long dirOffset = input.readLong();
		input.seek(dirOffset);
	}

	@Override
	public void close() throws IOException {
		try {
			try {
				if ((indexReader) != null) {
					indexReader.close();
				}
			} finally {
				indexReader = null;
				if ((in) != null) {
					in.close();
				}
			}
		} finally {
			if ((postingsReader) != null) {
				postingsReader.close();
			}
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

	private static final long FIELD_READER_RAM_BYTES_USED = RamUsageEstimator.shallowSizeOfInstance(BlockTermsReader.FieldReader.class);

	private class FieldReader extends Terms implements Accountable {
		final long numTerms;

		final FieldInfo fieldInfo;

		final long termsStartPointer;

		final long sumTotalTermFreq;

		final long sumDocFreq;

		final int docCount;

		final int longsSize;

		FieldReader(FieldInfo fieldInfo, long numTerms, long termsStartPointer, long sumTotalTermFreq, long sumDocFreq, int docCount, int longsSize) {
			assert numTerms > 0;
			this.fieldInfo = fieldInfo;
			this.numTerms = numTerms;
			this.termsStartPointer = termsStartPointer;
			this.sumTotalTermFreq = sumTotalTermFreq;
			this.sumDocFreq = sumDocFreq;
			this.docCount = docCount;
			this.longsSize = longsSize;
		}

		@Override
		public long ramBytesUsed() {
			return BlockTermsReader.FIELD_READER_RAM_BYTES_USED;
		}

		@Override
		public TermsEnum iterator() throws IOException {
			return new BlockTermsReader.FieldReader.SegmentTermsEnum();
		}

		@Override
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

		private final class SegmentTermsEnum extends TermsEnum {
			private final IndexInput in;

			private final BlockTermState state;

			private final boolean doOrd;

			private final BlockTermsReader.FieldAndTerm fieldTerm = new BlockTermsReader.FieldAndTerm();

			private final TermsIndexReaderBase.FieldIndexEnum indexEnum;

			private final BytesRefBuilder term = new BytesRefBuilder();

			private boolean indexIsCurrent;

			private boolean didIndexNext;

			private BytesRef nextIndexTerm;

			private boolean seekPending;

			private byte[] termSuffixes;

			private ByteArrayDataInput termSuffixesReader = new ByteArrayDataInput();

			private int termBlockPrefix;

			private int blockTermCount;

			private byte[] docFreqBytes;

			private final ByteArrayDataInput freqReader = new ByteArrayDataInput();

			private int metaDataUpto;

			private long[] longs;

			private byte[] bytes;

			private ByteArrayDataInput bytesReader;

			public SegmentTermsEnum() throws IOException {
				in = BlockTermsReader.this.in.clone();
				in.seek(termsStartPointer);
				indexEnum = indexReader.getFieldEnum(fieldInfo);
				doOrd = indexReader.supportsOrd();
				fieldTerm.field = fieldInfo.name;
				state = postingsReader.newTermState();
				state.totalTermFreq = -1;
				state.ord = -1;
				termSuffixes = new byte[128];
				docFreqBytes = new byte[64];
				longs = new long[longsSize];
			}

			@Override
			public TermsEnum.SeekStatus seekCeil(final BytesRef target) throws IOException {
				if ((indexEnum) == null) {
					throw new IllegalStateException("terms index was not loaded");
				}
				if (didIndexNext) {
					if ((nextIndexTerm) == null) {
					}else {
					}
				}
				boolean doSeek = true;
				if (indexIsCurrent) {
					final int cmp = term.get().compareTo(target);
					if (cmp == 0) {
						return FOUND;
					}else
						if (cmp < 0) {
							if (!(didIndexNext)) {
								if ((indexEnum.next()) == (-1)) {
									nextIndexTerm = null;
								}else {
									nextIndexTerm = indexEnum.term();
								}
								didIndexNext = true;
							}
							if (((nextIndexTerm) == null) || ((target.compareTo(nextIndexTerm)) < 0)) {
								doSeek = false;
							}
						}

				}
				if (doSeek) {
					in.seek(indexEnum.seek(target));
					boolean result = nextBlock();
					assert result;
					indexIsCurrent = true;
					didIndexNext = false;
					if (doOrd) {
						state.ord = (indexEnum.ord()) - 1;
					}
					term.copyBytes(indexEnum.term());
				}else {
					if (((state.termBlockOrd) == (blockTermCount)) && (!(nextBlock()))) {
						indexIsCurrent = false;
						return END;
					}
				}
				seekPending = false;
				int common = 0;
				while (true) {
					if (common < (termBlockPrefix)) {
						final int cmp = ((term.byteAt(common)) & 255) - ((target.bytes[((target.offset) + common)]) & 255);
						if (cmp < 0) {
							if ((state.termBlockOrd) < (blockTermCount)) {
								while ((state.termBlockOrd) < ((blockTermCount) - 1)) {
									(state.termBlockOrd)++;
									(state.ord)++;
									termSuffixesReader.skipBytes(termSuffixesReader.readVInt());
								} 
								final int suffix = termSuffixesReader.readVInt();
								term.setLength(((termBlockPrefix) + suffix));
								term.grow(term.length());
								termSuffixesReader.readBytes(term.bytes(), termBlockPrefix, suffix);
							}
							(state.ord)++;
							if (!(nextBlock())) {
								indexIsCurrent = false;
								return END;
							}
							common = 0;
						}else
							if (cmp > 0) {
								assert (state.termBlockOrd) == 0;
								final int suffix = termSuffixesReader.readVInt();
								term.setLength(((termBlockPrefix) + suffix));
								term.grow(term.length());
								termSuffixesReader.readBytes(term.bytes(), termBlockPrefix, suffix);
								return NOT_FOUND;
							}else {
								common++;
							}

						continue;
					}
					while (true) {
						(state.termBlockOrd)++;
						(state.ord)++;
						final int suffix = termSuffixesReader.readVInt();
						final int termLen = (termBlockPrefix) + suffix;
						int bytePos = termSuffixesReader.getPosition();
						boolean next = false;
						final int limit = (target.offset) + (termLen < (target.length) ? termLen : target.length);
						int targetPos = (target.offset) + (termBlockPrefix);
						while (targetPos < limit) {
							final int cmp = ((termSuffixes[(bytePos++)]) & 255) - ((target.bytes[(targetPos++)]) & 255);
							if (cmp < 0) {
								next = true;
								break;
							}else
								if (cmp > 0) {
									term.setLength(((termBlockPrefix) + suffix));
									term.grow(term.length());
									termSuffixesReader.readBytes(term.bytes(), termBlockPrefix, suffix);
									return NOT_FOUND;
								}

						} 
						if ((!next) && ((target.length) <= termLen)) {
							term.setLength(((termBlockPrefix) + suffix));
							term.grow(term.length());
							termSuffixesReader.readBytes(term.bytes(), termBlockPrefix, suffix);
							if ((target.length) == termLen) {
								return FOUND;
							}else {
								return NOT_FOUND;
							}
						}
						if ((state.termBlockOrd) == (blockTermCount)) {
							term.setLength(((termBlockPrefix) + suffix));
							term.grow(term.length());
							termSuffixesReader.readBytes(term.bytes(), termBlockPrefix, suffix);
							break;
						}else {
							termSuffixesReader.skipBytes(suffix);
						}
					} 
					assert indexIsCurrent;
					if (!(nextBlock())) {
						indexIsCurrent = false;
						return END;
					}
					common = 0;
				} 
			}

			@Override
			public BytesRef next() throws IOException {
				if (seekPending) {
					assert !(indexIsCurrent);
					in.seek(state.blockFilePointer);
					final int pendingSeekCount = state.termBlockOrd;
					boolean result = nextBlock();
					final long savOrd = state.ord;
					assert result;
					while ((state.termBlockOrd) < pendingSeekCount) {
						BytesRef nextResult = _next();
						assert nextResult != null;
					} 
					seekPending = false;
					state.ord = savOrd;
				}
				return _next();
			}

			private BytesRef _next() throws IOException {
				if (((state.termBlockOrd) == (blockTermCount)) && (!(nextBlock()))) {
					indexIsCurrent = false;
					return null;
				}
				final int suffix = termSuffixesReader.readVInt();
				term.setLength(((termBlockPrefix) + suffix));
				term.grow(term.length());
				termSuffixesReader.readBytes(term.bytes(), termBlockPrefix, suffix);
				(state.termBlockOrd)++;
				(state.ord)++;
				return term.get();
			}

			@Override
			public BytesRef term() {
				return term.get();
			}

			@Override
			public int docFreq() throws IOException {
				decodeMetaData();
				return state.docFreq;
			}

			@Override
			public long totalTermFreq() throws IOException {
				decodeMetaData();
				return state.totalTermFreq;
			}

			@Override
			public PostingsEnum postings(PostingsEnum reuse, int flags) throws IOException {
				decodeMetaData();
				return postingsReader.postings(fieldInfo, state, reuse, flags);
			}

			@Override
			public void seekExact(BytesRef target, TermState otherState) {
				assert (otherState != null) && (otherState instanceof BlockTermState);
				assert (!(doOrd)) || ((((BlockTermState) (otherState)).ord) < (numTerms));
				state.copyFrom(otherState);
				seekPending = true;
				indexIsCurrent = false;
				term.copyBytes(target);
			}

			@Override
			public TermState termState() throws IOException {
				decodeMetaData();
				TermState ts = state.clone();
				return ts;
			}

			@Override
			public void seekExact(long ord) throws IOException {
				if ((indexEnum) == null) {
					throw new IllegalStateException("terms index was not loaded");
				}
				assert ord < (numTerms);
				in.seek(indexEnum.seek(ord));
				boolean result = nextBlock();
				assert result;
				indexIsCurrent = true;
				didIndexNext = false;
				seekPending = false;
				state.ord = (indexEnum.ord()) - 1;
				assert (state.ord) >= (-1) : "ord=" + (state.ord);
				term.copyBytes(indexEnum.term());
				int left = ((int) (ord - (state.ord)));
				while (left > 0) {
					final BytesRef term = _next();
					assert term != null;
					left--;
					assert indexIsCurrent;
				} 
			}

			@Override
			public long ord() {
				if (!(doOrd)) {
					throw new UnsupportedOperationException();
				}
				return state.ord;
			}

			private boolean nextBlock() throws IOException {
				state.blockFilePointer = in.getFilePointer();
				blockTermCount = in.readVInt();
				if ((blockTermCount) == 0) {
					return false;
				}
				termBlockPrefix = in.readVInt();
				int len = in.readVInt();
				if ((termSuffixes.length) < len) {
					termSuffixes = new byte[ArrayUtil.oversize(len, 1)];
				}
				in.readBytes(termSuffixes, 0, len);
				termSuffixesReader.reset(termSuffixes, 0, len);
				len = in.readVInt();
				if ((docFreqBytes.length) < len) {
					docFreqBytes = new byte[ArrayUtil.oversize(len, 1)];
				}
				in.readBytes(docFreqBytes, 0, len);
				freqReader.reset(docFreqBytes, 0, len);
				len = in.readVInt();
				if ((bytes) == null) {
					bytes = new byte[ArrayUtil.oversize(len, 1)];
					bytesReader = new ByteArrayDataInput();
				}else
					if ((bytes.length) < len) {
						bytes = new byte[ArrayUtil.oversize(len, 1)];
					}

				in.readBytes(bytes, 0, len);
				bytesReader.reset(bytes, 0, len);
				metaDataUpto = 0;
				state.termBlockOrd = 0;
				indexIsCurrent = false;
				return true;
			}

			private void decodeMetaData() throws IOException {
				if (!(seekPending)) {
					final int limit = state.termBlockOrd;
					boolean absolute = (metaDataUpto) == 0;
					while ((metaDataUpto) < limit) {
						state.docFreq = freqReader.readVInt();
						if ((fieldInfo.getIndexOptions()) != (IndexOptions.DOCS)) {
							state.totalTermFreq = (state.docFreq) + (freqReader.readVLong());
						}
						for (int i = 0; i < (longs.length); i++) {
							longs[i] = bytesReader.readVLong();
						}
						postingsReader.decodeTerm(longs, bytesReader, fieldInfo, state, absolute);
						(metaDataUpto)++;
						absolute = false;
					} 
				}else {
				}
			}
		}
	}

	@Override
	public long ramBytesUsed() {
		long ramBytesUsed = BlockTermsReader.BASE_RAM_BYTES_USED;
		ramBytesUsed += ((postingsReader) != null) ? postingsReader.ramBytesUsed() : 0;
		ramBytesUsed += ((indexReader) != null) ? indexReader.ramBytesUsed() : 0;
		ramBytesUsed += ((fields.size()) * 2L) * (RamUsageEstimator.NUM_BYTES_OBJECT_REF);
		for (BlockTermsReader.FieldReader reader : fields.values()) {
			ramBytesUsed += reader.ramBytesUsed();
		}
		return ramBytesUsed;
	}

	@Override
	public Collection<Accountable> getChildResources() {
		List<Accountable> resources = new ArrayList<>();
		if ((indexReader) != null) {
			resources.add(Accountables.namedAccountable("term index", indexReader));
		}
		if ((postingsReader) != null) {
			resources.add(Accountables.namedAccountable("delegate", postingsReader));
		}
		return Collections.unmodifiableList(resources);
	}

	@Override
	public String toString() {
		return (((((getClass().getSimpleName()) + "(index=") + (indexReader)) + ",delegate=") + (postingsReader)) + ")";
	}

	@Override
	public void checkIntegrity() throws IOException {
		CodecUtil.checksumEntireFile(in);
		postingsReader.checkIntegrity();
	}
}

