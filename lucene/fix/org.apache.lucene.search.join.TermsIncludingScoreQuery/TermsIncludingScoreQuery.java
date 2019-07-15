

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.lucene.util.BitSetIterator;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefHash;
import org.apache.lucene.util.FixedBitSet;


class TermsIncludingScoreQuery extends Query {
	private final ScoreMode scoreMode;

	private final String toField;

	private final boolean multipleValuesPerDocument;

	private final BytesRefHash terms;

	private final float[] scores;

	private final int[] ords;

	private final Query fromQuery;

	private final String fromField;

	private final Object topReaderContextId;

	TermsIncludingScoreQuery(ScoreMode scoreMode, String toField, boolean multipleValuesPerDocument, BytesRefHash terms, float[] scores, String fromField, Query fromQuery, Object indexReaderContextId) {
		this.scoreMode = scoreMode;
		this.toField = toField;
		this.multipleValuesPerDocument = multipleValuesPerDocument;
		this.terms = terms;
		this.scores = scores;
		this.ords = terms.sort();
		this.fromField = fromField;
		this.fromQuery = fromQuery;
		this.topReaderContextId = indexReaderContextId;
	}

	@Override
	public String toString(String string) {
		return String.format(Locale.ROOT, "TermsIncludingScoreQuery{field=%s;fromQuery=%s}", toField, fromQuery);
	}

	@Override
	public boolean equals(Object other) {
		return (sameClassAs(other)) && (equalsTo(getClass().cast(other)));
	}

	private boolean equalsTo(TermsIncludingScoreQuery other) {
		return ((((Objects.equals(scoreMode, other.scoreMode)) && (Objects.equals(toField, other.toField))) && (Objects.equals(fromField, other.fromField))) && (Objects.equals(fromQuery, other.fromQuery))) && (Objects.equals(topReaderContextId, other.topReaderContextId));
	}

	@Override
	public int hashCode() {
		return (classHash()) + (Objects.hash(scoreMode, toField, fromField, fromQuery, topReaderContextId));
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
		if (needsScores == false) {
		}
		return new Weight(this) {
			@Override
			public void extractTerms(Set<Term> terms) {
			}

			@Override
			public Explanation explain(LeafReaderContext context, int doc) throws IOException {
				Terms terms = context.reader().terms(toField);
				if (terms != null) {
					TermsEnum segmentTermsEnum = terms.iterator();
					BytesRef spare = new BytesRef();
					PostingsEnum postingsEnum = null;
					for (int i = 0; i < (TermsIncludingScoreQuery.this.terms.size()); i++) {
						if (segmentTermsEnum.seekExact(TermsIncludingScoreQuery.this.terms.get(ords[i], spare))) {
							postingsEnum = segmentTermsEnum.postings(postingsEnum, PostingsEnum.NONE);
							if ((postingsEnum.advance(doc)) == doc) {
								final float score = TermsIncludingScoreQuery.this.scores[ords[i]];
								return Explanation.match(score, ("Score based on join value " + (segmentTermsEnum.term().utf8ToString())));
							}
						}
					}
				}
				return Explanation.noMatch("Not a match");
			}

			@Override
			public Scorer scorer(LeafReaderContext context) throws IOException {
				Terms terms = context.reader().terms(toField);
				if (terms == null) {
					return null;
				}
				final long cost = (context.reader().maxDoc()) * (terms.size());
				TermsEnum segmentTermsEnum = terms.iterator();
				if (multipleValuesPerDocument) {
					return new TermsIncludingScoreQuery.MVInOrderScorer(this, segmentTermsEnum, context.reader().maxDoc(), cost);
				}else {
					return new TermsIncludingScoreQuery.SVInOrderScorer(this, segmentTermsEnum, context.reader().maxDoc(), cost);
				}
			}

			@Override
			public boolean isCacheable(LeafReaderContext ctx) {
				return true;
			}
		};
	}

	class SVInOrderScorer extends Scorer {
		final DocIdSetIterator matchingDocsIterator;

		final float[] scores;

		final long cost;

		SVInOrderScorer(Weight weight, TermsEnum termsEnum, int maxDoc, long cost) throws IOException {
			super(weight);
			FixedBitSet matchingDocs = new FixedBitSet(maxDoc);
			this.scores = new float[maxDoc];
			fillDocsAndScores(matchingDocs, termsEnum);
			this.matchingDocsIterator = new BitSetIterator(matchingDocs, cost);
			this.cost = cost;
		}

		protected void fillDocsAndScores(FixedBitSet matchingDocs, TermsEnum termsEnum) throws IOException {
			BytesRef spare = new BytesRef();
			PostingsEnum postingsEnum = null;
			for (int i = 0; i < (terms.size()); i++) {
				if (termsEnum.seekExact(terms.get(ords[i], spare))) {
					postingsEnum = termsEnum.postings(postingsEnum, PostingsEnum.NONE);
					float score = TermsIncludingScoreQuery.this.scores[ords[i]];
					for (int doc = postingsEnum.nextDoc(); doc != (DocIdSetIterator.NO_MORE_DOCS); doc = postingsEnum.nextDoc()) {
						matchingDocs.set(doc);
						scores[doc] = score;
					}
				}
			}
		}

		@Override
		public float score() throws IOException {
			return scores[docID()];
		}

		@Override
		public int docID() {
			return matchingDocsIterator.docID();
		}

		@Override
		public DocIdSetIterator iterator() {
			return matchingDocsIterator;
		}
	}

	class MVInOrderScorer extends TermsIncludingScoreQuery.SVInOrderScorer {
		MVInOrderScorer(Weight weight, TermsEnum termsEnum, int maxDoc, long cost) throws IOException {
			super(weight, termsEnum, maxDoc, cost);
		}

		@Override
		protected void fillDocsAndScores(FixedBitSet matchingDocs, TermsEnum termsEnum) throws IOException {
			BytesRef spare = new BytesRef();
			PostingsEnum postingsEnum = null;
			for (int i = 0; i < (terms.size()); i++) {
				if (termsEnum.seekExact(terms.get(ords[i], spare))) {
					postingsEnum = termsEnum.postings(postingsEnum, PostingsEnum.NONE);
					float score = TermsIncludingScoreQuery.this.scores[ords[i]];
					for (int doc = postingsEnum.nextDoc(); doc != (DocIdSetIterator.NO_MORE_DOCS); doc = postingsEnum.nextDoc()) {
						if (!(matchingDocs.get(doc))) {
							scores[doc] = score;
							matchingDocs.set(doc);
						}
					}
				}
			}
		}
	}
}

