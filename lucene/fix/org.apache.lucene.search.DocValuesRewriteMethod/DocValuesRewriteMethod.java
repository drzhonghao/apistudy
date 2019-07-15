

import java.io.IOException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.ConstantScoreWeight;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Matches;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;


public final class DocValuesRewriteMethod extends MultiTermQuery.RewriteMethod {
	@Override
	public Query rewrite(IndexReader reader, MultiTermQuery query) {
		return new ConstantScoreQuery(new DocValuesRewriteMethod.MultiTermQueryDocValuesWrapper(query));
	}

	static class MultiTermQueryDocValuesWrapper extends Query {
		protected final MultiTermQuery query;

		protected MultiTermQueryDocValuesWrapper(MultiTermQuery query) {
			this.query = query;
		}

		@Override
		public String toString(String field) {
			return query.toString(field);
		}

		@Override
		public final boolean equals(final Object other) {
			return (sameClassAs(other)) && (query.equals(((DocValuesRewriteMethod.MultiTermQueryDocValuesWrapper) (other)).query));
		}

		@Override
		public final int hashCode() {
			return (31 * (classHash())) + (query.hashCode());
		}

		public final String getField() {
			return query.getField();
		}

		@Override
		public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
			return new ConstantScoreWeight(this, boost) {
				@Override
				public Matches matches(LeafReaderContext context, int doc) throws IOException {
					return null;
				}

				private TermsEnum getTermsEnum(SortedSetDocValues fcsi) throws IOException {
					return null;
				}

				@Override
				public Scorer scorer(LeafReaderContext context) throws IOException {
					return null;
				}

				@Override
				public boolean isCacheable(LeafReaderContext ctx) {
					return false;
				}
			};
		}
	}

	@Override
	public boolean equals(Object other) {
		return (other != null) && ((getClass()) == (other.getClass()));
	}

	@Override
	public int hashCode() {
		return 641;
	}
}

