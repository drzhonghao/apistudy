

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.Term[];
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.PriorityQueue;

import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;


public class MultiPhraseQuery extends Query {
	public static class Builder {
		private String field;

		private final ArrayList<Term[]> termArrays;

		private final ArrayList<Integer> positions;

		private int slop;

		public Builder() {
			this.field = null;
			this.termArrays = new ArrayList<>();
			this.positions = new ArrayList<>();
			this.slop = 0;
		}

		public Builder(MultiPhraseQuery multiPhraseQuery) {
			this.field = multiPhraseQuery.field;
			int length = multiPhraseQuery.termArrays.length;
			this.termArrays = new ArrayList<>(length);
			this.positions = new ArrayList<>(length);
			for (int i = 0; i < length; ++i) {
				this.termArrays.add(multiPhraseQuery.termArrays[i]);
				this.positions.add(multiPhraseQuery.positions[i]);
			}
			this.slop = multiPhraseQuery.slop;
		}

		public MultiPhraseQuery.Builder setSlop(int s) {
			if (s < 0) {
				throw new IllegalArgumentException("slop value cannot be negative");
			}
			slop = s;
			return this;
		}

		public MultiPhraseQuery.Builder add(Term term) {
			return add(new Term[]{ term });
		}

		public MultiPhraseQuery.Builder add(Term[] terms) {
			int position = 0;
			if ((positions.size()) > 0)
				position = (positions.get(((positions.size()) - 1))) + 1;

			return add(terms, position);
		}

		public MultiPhraseQuery.Builder add(Term[] terms, int position) {
			Objects.requireNonNull(terms, "Term array must not be null");
			if ((termArrays.size()) == 0)
				field = terms[0].field();

			for (Term term : terms) {
				if (!(term.field().equals(field))) {
					throw new IllegalArgumentException(((("All phrase terms must be in the same field (" + (field)) + "): ") + term));
				}
			}
			termArrays.add(terms);
			positions.add(position);
			return this;
		}

		public MultiPhraseQuery build() {
			int[] positionsArray = new int[this.positions.size()];
			for (int i = 0; i < (this.positions.size()); ++i) {
				positionsArray[i] = this.positions.get(i);
			}
			Term[][] termArraysArray = termArrays.toArray(new Term[termArrays.size()][]);
			return new MultiPhraseQuery(field, termArraysArray, positionsArray, slop);
		}
	}

	private final String field;

	private final Term[][] termArrays;

	private final int[] positions;

	private final int slop;

	private MultiPhraseQuery(String field, Term[][] termArrays, int[] positions, int slop) {
		this.field = field;
		this.termArrays = termArrays;
		this.positions = positions;
		this.slop = slop;
	}

	public int getSlop() {
		return slop;
	}

	public Term[][] getTermArrays() {
		return termArrays;
	}

	public int[] getPositions() {
		return positions;
	}

	@Override
	public Query rewrite(IndexReader reader) throws IOException {
		if ((termArrays.length) == 0) {
			return new MatchNoDocsQuery("empty MultiPhraseQuery");
		}else
			if ((termArrays.length) == 1) {
				Term[] terms = termArrays[0];
				BooleanQuery.Builder builder = new BooleanQuery.Builder();
				for (Term term : terms) {
					builder.add(new TermQuery(term), SHOULD);
				}
				return builder.build();
			}else {
				return super.rewrite(reader);
			}

	}

	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
		final Map<Term, TermContext> termStates = new HashMap<>();
	}

	@Override
	public final String toString(String f) {
		StringBuilder buffer = new StringBuilder();
		if (((field) == null) || (!(field.equals(f)))) {
			buffer.append(field);
			buffer.append(":");
		}
		buffer.append("\"");
		int lastPos = -1;
		for (int i = 0; i < (termArrays.length); ++i) {
			Term[] terms = termArrays[i];
			int position = positions[i];
			if (i != 0) {
				buffer.append(" ");
				for (int j = 1; j < (position - lastPos); j++) {
					buffer.append("? ");
				}
			}
			if ((terms.length) > 1) {
				buffer.append("(");
				for (int j = 0; j < (terms.length); j++) {
					buffer.append(terms[j].text());
					if (j < ((terms.length) - 1))
						buffer.append(" ");

				}
				buffer.append(")");
			}else {
				buffer.append(terms[0].text());
			}
			lastPos = position;
		}
		buffer.append("\"");
		if ((slop) != 0) {
			buffer.append("~");
			buffer.append(slop);
		}
		return buffer.toString();
	}

	@Override
	public boolean equals(Object other) {
		return (sameClassAs(other)) && (equalsTo(getClass().cast(other)));
	}

	private boolean equalsTo(MultiPhraseQuery other) {
		return (((this.slop) == (other.slop)) && (termArraysEquals(this.termArrays, other.termArrays))) && (Arrays.equals(this.positions, other.positions));
	}

	@Override
	public int hashCode() {
		return (((classHash()) ^ (slop)) ^ (termArraysHashCode())) ^ (Arrays.hashCode(positions));
	}

	private int termArraysHashCode() {
		int hashCode = 1;
		for (final Term[] termArray : termArrays) {
			hashCode = (31 * hashCode) + (termArray == null ? 0 : Arrays.hashCode(termArray));
		}
		return hashCode;
	}

	private boolean termArraysEquals(Term[][] termArrays1, Term[][] termArrays2) {
		if ((termArrays1.length) != (termArrays2.length)) {
			return false;
		}
		for (int i = 0; i < (termArrays1.length); ++i) {
			Term[] termArray1 = termArrays1[i];
			Term[] termArray2 = termArrays2[i];
			if (!(termArray1 == null ? termArray2 == null : Arrays.equals(termArray1, termArray2))) {
				return false;
			}
		}
		return true;
	}

	static class UnionPostingsEnum extends PostingsEnum {
		final MultiPhraseQuery.UnionPostingsEnum.DocsQueue docsQueue;

		final long cost;

		final MultiPhraseQuery.UnionPostingsEnum.PositionsQueue posQueue = new MultiPhraseQuery.UnionPostingsEnum.PositionsQueue();

		int posQueueDoc = -2;

		final PostingsEnum[] subs;

		UnionPostingsEnum(Collection<PostingsEnum> subs) {
			docsQueue = new MultiPhraseQuery.UnionPostingsEnum.DocsQueue(subs.size());
			long cost = 0;
			for (PostingsEnum sub : subs) {
				docsQueue.add(sub);
				cost += sub.cost();
			}
			this.cost = cost;
			this.subs = subs.toArray(new PostingsEnum[subs.size()]);
		}

		@Override
		public int freq() throws IOException {
			int doc = docID();
			if (doc != (posQueueDoc)) {
				posQueue.clear();
				for (PostingsEnum sub : subs) {
					if ((sub.docID()) == doc) {
						int freq = sub.freq();
						for (int i = 0; i < freq; i++) {
							posQueue.add(sub.nextPosition());
						}
					}
				}
				posQueue.sort();
				posQueueDoc = doc;
			}
			return posQueue.size();
		}

		@Override
		public int nextPosition() throws IOException {
			return posQueue.next();
		}

		@Override
		public int docID() {
			return docsQueue.top().docID();
		}

		@Override
		public int nextDoc() throws IOException {
			PostingsEnum top = docsQueue.top();
			int doc = top.docID();
			do {
				top.nextDoc();
				top = docsQueue.updateTop();
			} while ((top.docID()) == doc );
			return top.docID();
		}

		@Override
		public int advance(int target) throws IOException {
			PostingsEnum top = docsQueue.top();
			do {
				top.advance(target);
				top = docsQueue.updateTop();
			} while ((top.docID()) < target );
			return top.docID();
		}

		@Override
		public long cost() {
			return cost;
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

		static class DocsQueue extends PriorityQueue<PostingsEnum> {
			DocsQueue(int size) {
				super(size);
			}

			@Override
			public final boolean lessThan(PostingsEnum a, PostingsEnum b) {
				return (a.docID()) < (b.docID());
			}
		}

		static class PositionsQueue {
			private int arraySize = 16;

			private int index = 0;

			private int size = 0;

			private int[] array = new int[arraySize];

			void add(int i) {
				if ((size) == (arraySize))
					growArray();

				array[((size)++)] = i;
			}

			int next() {
				return array[((index)++)];
			}

			void sort() {
				Arrays.sort(array, index, size);
			}

			void clear() {
				index = 0;
				size = 0;
			}

			int size() {
				return size;
			}

			private void growArray() {
				int[] newArray = new int[(arraySize) * 2];
				System.arraycopy(array, 0, newArray, 0, arraySize);
				array = newArray;
				arraySize *= 2;
			}
		}
	}

	static class PostingsAndPosition {
		final PostingsEnum pe;

		int pos;

		int upto;

		PostingsAndPosition(PostingsEnum pe) {
			this.pe = pe;
		}
	}

	static class UnionFullPostingsEnum extends MultiPhraseQuery.UnionPostingsEnum {
		int freq = -1;

		boolean started = false;

		final PriorityQueue<MultiPhraseQuery.PostingsAndPosition> posQueue;

		final Collection<MultiPhraseQuery.PostingsAndPosition> subs;

		UnionFullPostingsEnum(List<PostingsEnum> subs) {
			super(subs);
			this.posQueue = new PriorityQueue<MultiPhraseQuery.PostingsAndPosition>(subs.size()) {
				@Override
				protected boolean lessThan(MultiPhraseQuery.PostingsAndPosition a, MultiPhraseQuery.PostingsAndPosition b) {
					return (a.pos) < (b.pos);
				}
			};
			this.subs = new ArrayList<>();
			for (PostingsEnum pe : subs) {
				this.subs.add(new MultiPhraseQuery.PostingsAndPosition(pe));
			}
		}

		@Override
		public int freq() throws IOException {
			int doc = docID();
			if (doc == (posQueueDoc)) {
				return freq;
			}
			freq = 0;
			started = false;
			posQueue.clear();
			for (MultiPhraseQuery.PostingsAndPosition pp : subs) {
				if ((pp.pe.docID()) == doc) {
					pp.pos = pp.pe.nextPosition();
					pp.upto = pp.pe.freq();
					posQueue.add(pp);
					freq += pp.upto;
				}
			}
			return freq;
		}

		@Override
		public int nextPosition() throws IOException {
			if ((started) == false) {
				started = true;
				return posQueue.top().pos;
			}
			if ((posQueue.top().upto) == 1) {
				posQueue.pop();
				return posQueue.top().pos;
			}
			posQueue.top().pos = posQueue.top().pe.nextPosition();
			(posQueue.top().upto)--;
			posQueue.updateTop();
			return posQueue.top().pos;
		}

		@Override
		public int startOffset() throws IOException {
			return posQueue.top().pe.startOffset();
		}

		@Override
		public int endOffset() throws IOException {
			return posQueue.top().pe.endOffset();
		}

		@Override
		public BytesRef getPayload() throws IOException {
			return posQueue.top().pe.getPayload();
		}
	}
}

