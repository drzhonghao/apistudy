

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import org.apache.lucene.index.MultiPostingsEnum;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.ReaderSlice;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.BytesRefIterator;
import org.apache.lucene.util.PriorityQueue;

import static org.apache.lucene.index.TermsEnum.SeekStatus.END;
import static org.apache.lucene.index.TermsEnum.SeekStatus.FOUND;
import static org.apache.lucene.index.TermsEnum.SeekStatus.NOT_FOUND;


public final class MultiTermsEnum extends TermsEnum {
	private static final Comparator<MultiTermsEnum.TermsEnumWithSlice> INDEX_COMPARATOR = new Comparator<MultiTermsEnum.TermsEnumWithSlice>() {
		@Override
		public int compare(MultiTermsEnum.TermsEnumWithSlice o1, MultiTermsEnum.TermsEnumWithSlice o2) {
			return (o1.index) - (o2.index);
		}
	};

	private final MultiTermsEnum.TermMergeQueue queue;

	private final MultiTermsEnum.TermsEnumWithSlice[] subs;

	private final MultiTermsEnum.TermsEnumWithSlice[] currentSubs;

	private final MultiTermsEnum.TermsEnumWithSlice[] top;

	private final MultiPostingsEnum.EnumWithSlice[] subDocs;

	private BytesRef lastSeek;

	private boolean lastSeekExact;

	private final BytesRefBuilder lastSeekScratch = new BytesRefBuilder();

	private int numTop;

	private int numSubs;

	private BytesRef current;

	static class TermsEnumIndex {
		public static final MultiTermsEnum.TermsEnumIndex[] EMPTY_ARRAY = new MultiTermsEnum.TermsEnumIndex[0];

		final int subIndex;

		final TermsEnum termsEnum;

		public TermsEnumIndex(TermsEnum termsEnum, int subIndex) {
			this.termsEnum = termsEnum;
			this.subIndex = subIndex;
		}
	}

	public int getMatchCount() {
		return numTop;
	}

	public MultiTermsEnum.TermsEnumWithSlice[] getMatchArray() {
		return top;
	}

	public MultiTermsEnum(ReaderSlice[] slices) {
		queue = new MultiTermsEnum.TermMergeQueue(slices.length);
		top = new MultiTermsEnum.TermsEnumWithSlice[slices.length];
		subs = new MultiTermsEnum.TermsEnumWithSlice[slices.length];
		subDocs = new MultiPostingsEnum.EnumWithSlice[slices.length];
		for (int i = 0; i < (slices.length); i++) {
			subs[i] = new MultiTermsEnum.TermsEnumWithSlice(i, slices[i]);
			subDocs[i].slice = slices[i];
		}
		currentSubs = new MultiTermsEnum.TermsEnumWithSlice[slices.length];
	}

	@Override
	public BytesRef term() {
		return current;
	}

	public TermsEnum reset(MultiTermsEnum.TermsEnumIndex[] termsEnumsIndex) throws IOException {
		assert (termsEnumsIndex.length) <= (top.length);
		numSubs = 0;
		numTop = 0;
		queue.clear();
		for (int i = 0; i < (termsEnumsIndex.length); i++) {
			final MultiTermsEnum.TermsEnumIndex termsEnumIndex = termsEnumsIndex[i];
			assert termsEnumIndex != null;
			final BytesRef term = termsEnumIndex.termsEnum.next();
			if (term != null) {
				final MultiTermsEnum.TermsEnumWithSlice entry = subs[termsEnumIndex.subIndex];
				entry.reset(termsEnumIndex.termsEnum, term);
				queue.add(entry);
				currentSubs[((numSubs)++)] = entry;
			}else {
			}
		}
		if ((queue.size()) == 0) {
			return TermsEnum.EMPTY;
		}else {
			return this;
		}
	}

	@Override
	public boolean seekExact(BytesRef term) throws IOException {
		queue.clear();
		numTop = 0;
		boolean seekOpt = false;
		if (((lastSeek) != null) && ((lastSeek.compareTo(term)) <= 0)) {
			seekOpt = true;
		}
		lastSeek = null;
		lastSeekExact = true;
		for (int i = 0; i < (numSubs); i++) {
			final boolean status;
			if (seekOpt) {
				final BytesRef curTerm = currentSubs[i].current;
				if (curTerm != null) {
					final int cmp = term.compareTo(curTerm);
					if (cmp == 0) {
						status = true;
					}else
						if (cmp < 0) {
							status = false;
						}else {
							status = currentSubs[i].terms.seekExact(term);
						}

				}else {
					status = false;
				}
			}else {
				status = currentSubs[i].terms.seekExact(term);
			}
			if (status) {
				top[((numTop)++)] = currentSubs[i];
				current = currentSubs[i].current = currentSubs[i].terms.term();
				assert term.equals(currentSubs[i].current);
			}
		}
		return (numTop) > 0;
	}

	@Override
	public TermsEnum.SeekStatus seekCeil(BytesRef term) throws IOException {
		queue.clear();
		numTop = 0;
		lastSeekExact = false;
		boolean seekOpt = false;
		if (((lastSeek) != null) && ((lastSeek.compareTo(term)) <= 0)) {
			seekOpt = true;
		}
		lastSeekScratch.copyBytes(term);
		lastSeek = lastSeekScratch.get();
		for (int i = 0; i < (numSubs); i++) {
			final TermsEnum.SeekStatus status;
			if (seekOpt) {
				final BytesRef curTerm = currentSubs[i].current;
				if (curTerm != null) {
					final int cmp = term.compareTo(curTerm);
					if (cmp == 0) {
						status = FOUND;
					}else
						if (cmp < 0) {
							status = NOT_FOUND;
						}else {
							status = currentSubs[i].terms.seekCeil(term);
						}

				}else {
					status = END;
				}
			}else {
				status = currentSubs[i].terms.seekCeil(term);
			}
			if (status == (FOUND)) {
				top[((numTop)++)] = currentSubs[i];
				current = currentSubs[i].current = currentSubs[i].terms.term();
				queue.add(currentSubs[i]);
			}else {
				if (status == (NOT_FOUND)) {
					currentSubs[i].current = currentSubs[i].terms.term();
					assert (currentSubs[i].current) != null;
					queue.add(currentSubs[i]);
				}else {
					assert status == (END);
					currentSubs[i].current = null;
				}
			}
		}
		if ((numTop) > 0) {
			return FOUND;
		}else
			if ((queue.size()) > 0) {
				pullTop();
				return NOT_FOUND;
			}else {
				return END;
			}

	}

	@Override
	public void seekExact(long ord) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long ord() {
		throw new UnsupportedOperationException();
	}

	private void pullTop() {
		assert (numTop) == 0;
		numTop = queue.fillTop(top);
		current = top[0].current;
	}

	private void pushTop() throws IOException {
		for (int i = 0; i < (numTop); i++) {
			MultiTermsEnum.TermsEnumWithSlice top = queue.top();
			top.current = top.terms.next();
			if ((top.current) == null) {
				queue.pop();
			}else {
				queue.updateTop();
			}
		}
		numTop = 0;
	}

	@Override
	public BytesRef next() throws IOException {
		if (lastSeekExact) {
			final TermsEnum.SeekStatus status = seekCeil(current);
			assert status == (FOUND);
			lastSeekExact = false;
		}
		lastSeek = null;
		pushTop();
		if ((queue.size()) > 0) {
			pullTop();
		}else {
			current = null;
		}
		return current;
	}

	@Override
	public int docFreq() throws IOException {
		int sum = 0;
		for (int i = 0; i < (numTop); i++) {
			sum += top[i].terms.docFreq();
		}
		return sum;
	}

	@Override
	public long totalTermFreq() throws IOException {
		long sum = 0;
		for (int i = 0; i < (numTop); i++) {
			final long v = top[i].terms.totalTermFreq();
			if (v == (-1)) {
				return v;
			}
			sum += v;
		}
		return sum;
	}

	@Override
	public PostingsEnum postings(PostingsEnum reuse, int flags) throws IOException {
		MultiPostingsEnum docsEnum;
		if ((reuse != null) && (reuse instanceof MultiPostingsEnum)) {
			docsEnum = ((MultiPostingsEnum) (reuse));
		}else {
		}
		int upto = 0;
		ArrayUtil.timSort(top, 0, numTop, MultiTermsEnum.INDEX_COMPARATOR);
		for (int i = 0; i < (numTop); i++) {
			final MultiTermsEnum.TermsEnumWithSlice entry = top[i];
			subDocs[upto].slice = entry.subSlice;
			upto++;
		}
		docsEnum = null;
		return docsEnum.reset(subDocs, upto);
	}

	static final class TermsEnumWithSlice {
		private final ReaderSlice subSlice;

		TermsEnum terms;

		public BytesRef current;

		final int index;

		public TermsEnumWithSlice(int index, ReaderSlice subSlice) {
			this.subSlice = subSlice;
			this.index = index;
			assert (subSlice.length) >= 0 : "length=" + (subSlice.length);
		}

		public void reset(TermsEnum terms, BytesRef term) {
			this.terms = terms;
			current = term;
		}

		@Override
		public String toString() {
			return ((subSlice.toString()) + ":") + (terms);
		}
	}

	private static final class TermMergeQueue extends PriorityQueue<MultiTermsEnum.TermsEnumWithSlice> {
		final int[] stack;

		TermMergeQueue(int size) {
			super(size);
			this.stack = new int[size];
		}

		@Override
		protected boolean lessThan(MultiTermsEnum.TermsEnumWithSlice termsA, MultiTermsEnum.TermsEnumWithSlice termsB) {
			return (termsA.current.compareTo(termsB.current)) < 0;
		}

		int fillTop(MultiTermsEnum.TermsEnumWithSlice[] tops) {
			final int size = size();
			if (size == 0) {
				return 0;
			}
			tops[0] = top();
			int numTop = 1;
			stack[0] = 1;
			int stackLen = 1;
			while (stackLen != 0) {
				final int index = stack[(--stackLen)];
				final int leftChild = index << 1;
				for (int child = leftChild, end = Math.min(size, (leftChild + 1)); child <= end; ++child) {
					MultiTermsEnum.TermsEnumWithSlice te = get(child);
					if (te.current.equals(tops[0].current)) {
						tops[(numTop++)] = te;
						stack[(stackLen++)] = child;
					}
				}
			} 
			return numTop;
		}

		private MultiTermsEnum.TermsEnumWithSlice get(int i) {
			return ((MultiTermsEnum.TermsEnumWithSlice) (getHeapArray()[i]));
		}
	}

	@Override
	public String toString() {
		return ("MultiTermsEnum(" + (Arrays.toString(subs))) + ")";
	}
}

