

import java.io.IOException;
import java.util.Set;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.OrdinalMap;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.ConstantScoreWeight;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.LongBitSet;
import org.apache.lucene.util.LongValues;


final class GlobalOrdinalsQuery extends Query {
	private final LongBitSet foundOrds;

	private final String joinField;

	private final OrdinalMap globalOrds;

	private final Query toQuery;

	private final Query fromQuery;

	private final Object indexReaderContextId;

	GlobalOrdinalsQuery(LongBitSet foundOrds, String joinField, OrdinalMap globalOrds, Query toQuery, Query fromQuery, Object indexReaderContextId) {
		this.foundOrds = foundOrds;
		this.joinField = joinField;
		this.globalOrds = globalOrds;
		this.toQuery = toQuery;
		this.fromQuery = fromQuery;
		this.indexReaderContextId = indexReaderContextId;
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
		if ((searcher.getTopReaderContext().id()) != (indexReaderContextId)) {
			throw new IllegalStateException("Creating the weight against a different index reader than this query has been built for.");
		}
		return new GlobalOrdinalsQuery.W(this, toQuery.createWeight(searcher, false, 1.0F), boost);
	}

	@Override
	public boolean equals(Object other) {
		return (sameClassAs(other)) && (equalsTo(getClass().cast(other)));
	}

	private boolean equalsTo(GlobalOrdinalsQuery other) {
		return (((fromQuery.equals(other.fromQuery)) && (joinField.equals(other.joinField))) && (toQuery.equals(other.toQuery))) && (indexReaderContextId.equals(other.indexReaderContextId));
	}

	@Override
	public int hashCode() {
		int result = classHash();
		result = (31 * result) + (joinField.hashCode());
		result = (31 * result) + (toQuery.hashCode());
		result = (31 * result) + (fromQuery.hashCode());
		result = (31 * result) + (indexReaderContextId.hashCode());
		return result;
	}

	@Override
	public String toString(String field) {
		return (("GlobalOrdinalsQuery{" + "joinField=") + (joinField)) + '}';
	}

	final class W extends ConstantScoreWeight {
		private final Weight approximationWeight;

		W(Query query, Weight approximationWeight, float boost) {
			super(query, boost);
			this.approximationWeight = approximationWeight;
		}

		@Override
		public void extractTerms(Set<Term> terms) {
		}

		@Override
		public Explanation explain(LeafReaderContext context, int doc) throws IOException {
			SortedDocValues values = DocValues.getSorted(context.reader(), joinField);
			if (values == null) {
				return Explanation.noMatch("Not a match");
			}
			if ((values.advance(doc)) != doc) {
				return Explanation.noMatch("Not a match");
			}
			int segmentOrd = values.ordValue();
			BytesRef joinValue = values.lookupOrd(segmentOrd);
			int ord;
			if ((globalOrds) != null) {
				ord = ((int) (globalOrds.getGlobalOrds(context.ord).get(segmentOrd)));
			}else {
				ord = segmentOrd;
			}
			if ((foundOrds.get(ord)) == false) {
				return Explanation.noMatch(("Not a match, join value " + (Term.toString(joinValue))));
			}
			return Explanation.match(score(), ("A match, join value " + (Term.toString(joinValue))));
		}

		@Override
		public Scorer scorer(LeafReaderContext context) throws IOException {
			SortedDocValues values = DocValues.getSorted(context.reader(), joinField);
			if (values == null) {
				return null;
			}
			Scorer approximationScorer = approximationWeight.scorer(context);
			if (approximationScorer == null) {
				return null;
			}
			if ((globalOrds) != null) {
			}
			{
			}
			return null;
		}

		@Override
		public boolean isCacheable(LeafReaderContext ctx) {
			return false;
		}
	}

	static final class OrdinalMapScorer {
		final LongBitSet foundOrds;

		final LongValues segmentOrdToGlobalOrdLookup;

		public OrdinalMapScorer(Weight weight, float score, LongBitSet foundOrds, SortedDocValues values, DocIdSetIterator approximationScorer, LongValues segmentOrdToGlobalOrdLookup) {
			this.foundOrds = foundOrds;
			this.segmentOrdToGlobalOrdLookup = segmentOrdToGlobalOrdLookup;
		}

		protected TwoPhaseIterator createTwoPhaseIterator(DocIdSetIterator approximation) {
			return new TwoPhaseIterator(approximation) {
				@Override
				public boolean matches() throws IOException {
					return false;
				}

				@Override
				public float matchCost() {
					return 100;
				}
			};
		}
	}

	static final class SegmentOrdinalScorer {
		final LongBitSet foundOrds;

		public SegmentOrdinalScorer(Weight weight, float score, LongBitSet foundOrds, SortedDocValues values, DocIdSetIterator approximationScorer) {
			this.foundOrds = foundOrds;
		}

		protected TwoPhaseIterator createTwoPhaseIterator(DocIdSetIterator approximation) {
			return new TwoPhaseIterator(approximation) {
				@Override
				public boolean matches() throws IOException {
					return false;
				}

				@Override
				public float matchCost() {
					return 100;
				}
			};
		}
	}
}

