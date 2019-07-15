

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BulkScorer;
import org.apache.lucene.search.ConstantScoreScorer;
import org.apache.lucene.search.ConstantScoreWeight;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Matches;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefIterator;


final class MultiTermQueryConstantScoreWrapper<Q extends MultiTermQuery> extends Query {
	private static final int BOOLEAN_REWRITE_TERM_COUNT_THRESHOLD = 16;

	private static class TermAndState {
		final BytesRef term;

		final TermState state;

		final int docFreq;

		final long totalTermFreq;

		TermAndState(BytesRef term, TermState state, int docFreq, long totalTermFreq) {
			this.term = term;
			this.state = state;
			this.docFreq = docFreq;
			this.totalTermFreq = totalTermFreq;
		}
	}

	private static class WeightOrDocIdSet {
		final Weight weight;

		final DocIdSet set;

		WeightOrDocIdSet(Weight weight) {
			this.weight = Objects.requireNonNull(weight);
			this.set = null;
		}

		WeightOrDocIdSet(DocIdSet bitset) {
			this.set = bitset;
			this.weight = null;
		}
	}

	protected final Q query;

	protected MultiTermQueryConstantScoreWrapper(Q query) {
		this.query = query;
	}

	@Override
	public String toString(String field) {
		return query.toString(field);
	}

	@Override
	public final boolean equals(final Object other) {
		return (sameClassAs(other)) && (query.equals(((MultiTermQueryConstantScoreWrapper<?>) (other)).query));
	}

	@Override
	public final int hashCode() {
		return (31 * (classHash())) + (query.hashCode());
	}

	public Q getQuery() {
		return query;
	}

	public final String getField() {
		return query.getField();
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
		return new ConstantScoreWeight(this, boost) {
			private boolean collectTerms(LeafReaderContext context, TermsEnum termsEnum, List<MultiTermQueryConstantScoreWrapper.TermAndState> terms) throws IOException {
				final int threshold = Math.min(MultiTermQueryConstantScoreWrapper.BOOLEAN_REWRITE_TERM_COUNT_THRESHOLD, BooleanQuery.getMaxClauseCount());
				for (int i = 0; i < threshold; ++i) {
					final BytesRef term = termsEnum.next();
					if (term == null) {
						return true;
					}
					TermState state = termsEnum.termState();
					terms.add(new MultiTermQueryConstantScoreWrapper.TermAndState(BytesRef.deepCopyOf(term), state, termsEnum.docFreq(), termsEnum.totalTermFreq()));
				}
				return (termsEnum.next()) == null;
			}

			private MultiTermQueryConstantScoreWrapper.WeightOrDocIdSet rewrite(LeafReaderContext context) throws IOException {
				PostingsEnum docs = null;
				final List<MultiTermQueryConstantScoreWrapper.TermAndState> collectedTerms = new ArrayList<>();
				if ((collectedTerms.isEmpty()) == false) {
					for (MultiTermQueryConstantScoreWrapper.TermAndState t : collectedTerms) {
					}
				}
				return null;
			}

			private Scorer scorer(DocIdSet set) throws IOException {
				if (set == null) {
					return null;
				}
				final DocIdSetIterator disi = set.iterator();
				if (disi == null) {
					return null;
				}
				return new ConstantScoreScorer(this, score(), disi);
			}

			@Override
			public BulkScorer bulkScorer(LeafReaderContext context) throws IOException {
				final MultiTermQueryConstantScoreWrapper.WeightOrDocIdSet weightOrBitSet = rewrite(context);
				if ((weightOrBitSet.weight) != null) {
					return weightOrBitSet.weight.bulkScorer(context);
				}else {
					final Scorer scorer = scorer(weightOrBitSet.set);
					if (scorer == null) {
						return null;
					}
					return new Weight.DefaultBulkScorer(scorer);
				}
			}

			@Override
			public Matches matches(LeafReaderContext context, int doc) throws IOException {
				return null;
			}

			@Override
			public Scorer scorer(LeafReaderContext context) throws IOException {
				final MultiTermQueryConstantScoreWrapper.WeightOrDocIdSet weightOrBitSet = rewrite(context);
				if ((weightOrBitSet.weight) != null) {
					return weightOrBitSet.weight.scorer(context);
				}else {
					return scorer(weightOrBitSet.set);
				}
			}

			@Override
			public boolean isCacheable(LeafReaderContext ctx) {
				return true;
			}
		};
	}
}

