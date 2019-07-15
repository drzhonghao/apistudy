

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Weight;


class DrillSidewaysQuery extends Query {
	final Query baseQuery;

	final Collector drillDownCollector;

	final Collector[] drillSidewaysCollectors;

	final Query[] drillDownQueries;

	final boolean scoreSubDocsAtOnce;

	DrillSidewaysQuery(Query baseQuery, Collector drillDownCollector, Collector[] drillSidewaysCollectors, Query[] drillDownQueries, boolean scoreSubDocsAtOnce) {
		this.baseQuery = Objects.requireNonNull(baseQuery);
		this.drillDownCollector = drillDownCollector;
		this.drillSidewaysCollectors = drillSidewaysCollectors;
		this.drillDownQueries = drillDownQueries;
		this.scoreSubDocsAtOnce = scoreSubDocsAtOnce;
	}

	@Override
	public String toString(String field) {
		return "DrillSidewaysQuery";
	}

	@Override
	public Query rewrite(IndexReader reader) throws IOException {
		Query newQuery = baseQuery;
		while (true) {
			Query rewrittenQuery = newQuery.rewrite(reader);
			if (rewrittenQuery == newQuery) {
				break;
			}
			newQuery = rewrittenQuery;
		} 
		if (newQuery == (baseQuery)) {
			return super.rewrite(reader);
		}else {
			return new DrillSidewaysQuery(newQuery, drillDownCollector, drillSidewaysCollectors, drillDownQueries, scoreSubDocsAtOnce);
		}
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
		final Weight baseWeight = baseQuery.createWeight(searcher, needsScores, boost);
		final Weight[] drillDowns = new Weight[drillDownQueries.length];
		for (int dim = 0; dim < (drillDownQueries.length); dim++) {
			drillDowns[dim] = searcher.createWeight(searcher.rewrite(drillDownQueries[dim]), false, 1);
		}
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = classHash();
		result = (prime * result) + (Objects.hashCode(baseQuery));
		result = (prime * result) + (Objects.hashCode(drillDownCollector));
		result = (prime * result) + (Arrays.hashCode(drillDownQueries));
		result = (prime * result) + (Arrays.hashCode(drillSidewaysCollectors));
		return result;
	}

	@Override
	public boolean equals(Object other) {
		return (sameClassAs(other)) && (equalsTo(getClass().cast(other)));
	}

	private boolean equalsTo(DrillSidewaysQuery other) {
		return (((Objects.equals(baseQuery, other.baseQuery)) && (Objects.equals(drillDownCollector, other.drillDownCollector))) && (Arrays.equals(drillDownQueries, other.drillDownQueries))) && (Arrays.equals(drillSidewaysCollectors, other.drillSidewaysCollectors));
	}
}

