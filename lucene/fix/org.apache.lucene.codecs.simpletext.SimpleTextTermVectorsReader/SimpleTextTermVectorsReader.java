

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.lucene.codecs.TermVectorsReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.BufferedChecksumIndexInput;
import org.apache.lucene.store.ChecksumIndexInput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.CharsRefBuilder;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.RamUsageEstimator;

import static org.apache.lucene.index.TermsEnum.SeekStatus.END;
import static org.apache.lucene.index.TermsEnum.SeekStatus.FOUND;
import static org.apache.lucene.index.TermsEnum.SeekStatus.NOT_FOUND;


public class SimpleTextTermVectorsReader extends TermVectorsReader {
	private static final long BASE_RAM_BYTES_USED = ((RamUsageEstimator.shallowSizeOfInstance(SimpleTextTermVectorsReader.class)) + (RamUsageEstimator.shallowSizeOfInstance(BytesRef.class))) + (RamUsageEstimator.shallowSizeOfInstance(CharsRef.class));

	private long[] offsets;

	private IndexInput in;

	private BytesRefBuilder scratch = new BytesRefBuilder();

	private CharsRefBuilder scratchUTF16 = new CharsRefBuilder();

	public SimpleTextTermVectorsReader(Directory directory, SegmentInfo si, IOContext context) throws IOException {
		boolean success = false;
		try {
			success = true;
		} finally {
			if (!success) {
				try {
					close();
				} catch (Throwable t) {
				}
			}
		}
		readIndex(si.maxDoc());
	}

	SimpleTextTermVectorsReader(long[] offsets, IndexInput in) {
		this.offsets = offsets;
		this.in = in;
	}

	private void readIndex(int maxDoc) throws IOException {
		ChecksumIndexInput input = new BufferedChecksumIndexInput(in);
		offsets = new long[maxDoc];
		int upto = 0;
		assert upto == (offsets.length);
	}

	@Override
	public Fields get(int doc) throws IOException {
		SortedMap<String, SimpleTextTermVectorsReader.SimpleTVTerms> fields = new TreeMap<>();
		in.seek(offsets[doc]);
		readLine();
		return new SimpleTextTermVectorsReader.SimpleTVFields(fields);
	}

	@Override
	public TermVectorsReader clone() {
		if ((in) == null) {
			throw new AlreadyClosedException("this TermVectorsReader is closed");
		}
		return new SimpleTextTermVectorsReader(offsets, in.clone());
	}

	@Override
	public void close() throws IOException {
		try {
			IOUtils.close(in);
		} finally {
			in = null;
			offsets = null;
		}
	}

	private void readLine() throws IOException {
	}

	private int parseIntAt(int offset) {
		scratchUTF16.copyUTF8Bytes(scratch.bytes(), offset, ((scratch.length()) - offset));
		return ArrayUtil.parseInt(scratchUTF16.chars(), 0, scratchUTF16.length());
	}

	private String readString(int offset, BytesRefBuilder scratch) {
		scratchUTF16.copyUTF8Bytes(scratch.bytes(), offset, ((scratch.length()) - offset));
		return scratchUTF16.toString();
	}

	private static class SimpleTVFields extends Fields {
		private final SortedMap<String, SimpleTextTermVectorsReader.SimpleTVTerms> fields;

		SimpleTVFields(SortedMap<String, SimpleTextTermVectorsReader.SimpleTVTerms> fields) {
			this.fields = fields;
		}

		@Override
		public Iterator<String> iterator() {
			return Collections.unmodifiableSet(fields.keySet()).iterator();
		}

		@Override
		public Terms terms(String field) throws IOException {
			return fields.get(field);
		}

		@Override
		public int size() {
			return fields.size();
		}
	}

	private static class SimpleTVTerms extends Terms {
		final SortedMap<BytesRef, SimpleTextTermVectorsReader.SimpleTVPostings> terms;

		final boolean hasOffsets;

		final boolean hasPositions;

		final boolean hasPayloads;

		SimpleTVTerms(boolean hasOffsets, boolean hasPositions, boolean hasPayloads) {
			this.hasOffsets = hasOffsets;
			this.hasPositions = hasPositions;
			this.hasPayloads = hasPayloads;
			terms = new TreeMap<>();
		}

		@Override
		public TermsEnum iterator() throws IOException {
			return new SimpleTextTermVectorsReader.SimpleTVTermsEnum(terms);
		}

		@Override
		public long size() throws IOException {
			return terms.size();
		}

		@Override
		public long getSumTotalTermFreq() throws IOException {
			return -1;
		}

		@Override
		public long getSumDocFreq() throws IOException {
			return terms.size();
		}

		@Override
		public int getDocCount() throws IOException {
			return 1;
		}

		@Override
		public boolean hasFreqs() {
			return true;
		}

		@Override
		public boolean hasOffsets() {
			return hasOffsets;
		}

		@Override
		public boolean hasPositions() {
			return hasPositions;
		}

		@Override
		public boolean hasPayloads() {
			return hasPayloads;
		}
	}

	private static class SimpleTVPostings {
		private int freq;

		private int[] positions;

		private int[] startOffsets;

		private int[] endOffsets;

		private BytesRef[] payloads;
	}

	private static class SimpleTVTermsEnum extends TermsEnum {
		SortedMap<BytesRef, SimpleTextTermVectorsReader.SimpleTVPostings> terms;

		Iterator<Map.Entry<BytesRef, SimpleTextTermVectorsReader.SimpleTVPostings>> iterator;

		Map.Entry<BytesRef, SimpleTextTermVectorsReader.SimpleTVPostings> current;

		SimpleTVTermsEnum(SortedMap<BytesRef, SimpleTextTermVectorsReader.SimpleTVPostings> terms) {
			this.terms = terms;
			this.iterator = terms.entrySet().iterator();
		}

		@Override
		public TermsEnum.SeekStatus seekCeil(BytesRef text) throws IOException {
			iterator = terms.tailMap(text).entrySet().iterator();
			if (!(iterator.hasNext())) {
				return END;
			}else {
				return next().equals(text) ? FOUND : NOT_FOUND;
			}
		}

		@Override
		public void seekExact(long ord) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public BytesRef next() throws IOException {
			if (!(iterator.hasNext())) {
				return null;
			}else {
				current = iterator.next();
				return current.getKey();
			}
		}

		@Override
		public BytesRef term() throws IOException {
			return current.getKey();
		}

		@Override
		public long ord() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public int docFreq() throws IOException {
			return 1;
		}

		@Override
		public long totalTermFreq() throws IOException {
			return current.getValue().freq;
		}

		@Override
		public PostingsEnum postings(PostingsEnum reuse, int flags) throws IOException {
			if (PostingsEnum.featureRequested(flags, PostingsEnum.POSITIONS)) {
				SimpleTextTermVectorsReader.SimpleTVPostings postings = current.getValue();
				if (((postings.positions) != null) || ((postings.startOffsets) != null)) {
					SimpleTextTermVectorsReader.SimpleTVPostingsEnum e = new SimpleTextTermVectorsReader.SimpleTVPostingsEnum();
					e.reset(postings.positions, postings.startOffsets, postings.endOffsets, postings.payloads);
					return e;
				}
			}
			SimpleTextTermVectorsReader.SimpleTVDocsEnum e = new SimpleTextTermVectorsReader.SimpleTVDocsEnum();
			e.reset(((PostingsEnum.featureRequested(flags, PostingsEnum.FREQS)) == false ? 1 : current.getValue().freq));
			return e;
		}
	}

	private static class SimpleTVDocsEnum extends PostingsEnum {
		private boolean didNext;

		private int doc = -1;

		private int freq;

		@Override
		public int freq() throws IOException {
			assert (freq) != (-1);
			return freq;
		}

		@Override
		public int nextPosition() throws IOException {
			return -1;
		}

		@Override
		public int startOffset() throws IOException {
			return -1;
		}

		@Override
		public int endOffset() throws IOException {
			return -1;
		}

		@Override
		public BytesRef getPayload() throws IOException {
			return null;
		}

		@Override
		public int docID() {
			return doc;
		}

		@Override
		public int nextDoc() {
			if (!(didNext)) {
				didNext = true;
				return doc = 0;
			}else {
				return doc = DocIdSetIterator.NO_MORE_DOCS;
			}
		}

		@Override
		public int advance(int target) throws IOException {
			return slowAdvance(target);
		}

		public void reset(int freq) {
			this.freq = freq;
			this.doc = -1;
			didNext = false;
		}

		@Override
		public long cost() {
			return 1;
		}
	}

	private static class SimpleTVPostingsEnum extends PostingsEnum {
		private boolean didNext;

		private int doc = -1;

		private int nextPos;

		private int[] positions;

		private BytesRef[] payloads;

		private int[] startOffsets;

		private int[] endOffsets;

		@Override
		public int freq() throws IOException {
			if ((positions) != null) {
				return positions.length;
			}else {
				assert (startOffsets) != null;
				return startOffsets.length;
			}
		}

		@Override
		public int docID() {
			return doc;
		}

		@Override
		public int nextDoc() {
			if (!(didNext)) {
				didNext = true;
				return doc = 0;
			}else {
				return doc = DocIdSetIterator.NO_MORE_DOCS;
			}
		}

		@Override
		public int advance(int target) throws IOException {
			return slowAdvance(target);
		}

		public void reset(int[] positions, int[] startOffsets, int[] endOffsets, BytesRef[] payloads) {
			this.positions = positions;
			this.startOffsets = startOffsets;
			this.endOffsets = endOffsets;
			this.payloads = payloads;
			this.doc = -1;
			didNext = false;
			nextPos = 0;
		}

		@Override
		public BytesRef getPayload() {
			return (payloads) == null ? null : payloads[((nextPos) - 1)];
		}

		@Override
		public int nextPosition() {
			if ((positions) != null) {
				assert (nextPos) < (positions.length);
				return positions[((nextPos)++)];
			}else {
				assert (nextPos) < (startOffsets.length);
				(nextPos)++;
				return -1;
			}
		}

		@Override
		public int startOffset() {
			if ((startOffsets) == null) {
				return -1;
			}else {
				return startOffsets[((nextPos) - 1)];
			}
		}

		@Override
		public int endOffset() {
			if ((endOffsets) == null) {
				return -1;
			}else {
				return endOffsets[((nextPos) - 1)];
			}
		}

		@Override
		public long cost() {
			return 1;
		}
	}

	@Override
	public long ramBytesUsed() {
		return (SimpleTextTermVectorsReader.BASE_RAM_BYTES_USED) + (RamUsageEstimator.sizeOf(offsets));
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public void checkIntegrity() throws IOException {
	}
}

