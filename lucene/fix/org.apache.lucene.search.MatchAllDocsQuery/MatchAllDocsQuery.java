

import java.io.IOException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.BulkScorer;
import org.apache.lucene.search.ConstantScoreScorer;
import org.apache.lucene.search.ConstantScoreWeight;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.Bits;


public final class MatchAllDocsQuery extends Query {
	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) {
		return new ConstantScoreWeight(this, boost) {
			@Override
			public String toString() {
				return ("weight(" + (MatchAllDocsQuery.this)) + ")";
			}

			@Override
			public Scorer scorer(LeafReaderContext context) throws IOException {
				return new ConstantScoreScorer(this, score(), DocIdSetIterator.all(context.reader().maxDoc()));
			}

			@Override
			public boolean isCacheable(LeafReaderContext ctx) {
				return true;
			}

			@Override
			public BulkScorer bulkScorer(LeafReaderContext context) throws IOException {
				final float score = score();
				final int maxDoc = context.reader().maxDoc();
				return new BulkScorer() {
					@Override
					public int score(LeafCollector collector, Bits acceptDocs, int min, int max) throws IOException {
						max = Math.min(max, maxDoc);
						for (int doc = min; doc < max; ++doc) {
							if ((acceptDocs == null) || (acceptDocs.get(doc))) {
								collector.collect(doc);
							}
						}
						return max == maxDoc ? DocIdSetIterator.NO_MORE_DOCS : max;
					}

					@Override
					public long cost() {
						return maxDoc;
					}
				};
			}
		};
	}

	@Override
	public String toString(String field) {
		return "*:*";
	}

	@Override
	public boolean equals(Object o) {
		return sameClassAs(o);
	}

	@Override
	public int hashCode() {
		return classHash();
	}
}

