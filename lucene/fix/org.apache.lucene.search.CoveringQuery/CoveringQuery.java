

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LongValues;
import org.apache.lucene.search.LongValuesSource;
import org.apache.lucene.search.Matches;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SegmentCacheable;
import org.apache.lucene.search.Weight;


public final class CoveringQuery extends Query {
	private final Collection<Query> queries = null;

	private final LongValuesSource minimumNumberMatch;

	private final int hashCode;

	public CoveringQuery(Collection<Query> queries, LongValuesSource minimumNumberMatch) {
		if ((queries.size()) > (BooleanQuery.getMaxClauseCount())) {
			throw new BooleanQuery.TooManyClauses();
		}
		if (minimumNumberMatch.needsScores()) {
			throw new IllegalArgumentException("The minimum number of matches may not depend on the score.");
		}
		this.queries.addAll(queries);
		this.minimumNumberMatch = Objects.requireNonNull(minimumNumberMatch);
		this.hashCode = computeHashCode();
	}

	@Override
	public String toString(String field) {
		String queriesToString = queries.stream().map(( q) -> q.toString(field)).sorted().collect(Collectors.joining(", "));
		return ((("CoveringQuery(queries=[" + queriesToString) + "], minimumNumberMatch=") + (minimumNumberMatch)) + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if ((sameClassAs(obj)) == false) {
			return false;
		}
		CoveringQuery that = ((CoveringQuery) (obj));
		return (((hashCode) == (that.hashCode)) && (Objects.equals(queries, that.queries))) && (Objects.equals(minimumNumberMatch, that.minimumNumberMatch));
	}

	private int computeHashCode() {
		int h = classHash();
		h = (31 * h) + (queries.hashCode());
		h = (31 * h) + (minimumNumberMatch.hashCode());
		return h;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public Query rewrite(IndexReader reader) throws IOException {
		boolean actuallyRewritten = false;
		for (Query query : queries) {
			Query r = query.rewrite(reader);
			actuallyRewritten |= query != r;
		}
		if (actuallyRewritten) {
		}
		return super.rewrite(reader);
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
		final List<Weight> weights = new ArrayList<>(queries.size());
		for (Query query : queries) {
			weights.add(searcher.createWeight(query, needsScores, boost));
		}
		return new CoveringQuery.CoveringWeight(this, weights, minimumNumberMatch.rewrite(searcher));
	}

	private static class CoveringWeight extends Weight {
		private final Collection<Weight> weights;

		private final LongValuesSource minimumNumberMatch;

		CoveringWeight(Query query, Collection<Weight> weights, LongValuesSource minimumNumberMatch) {
			super(query);
			this.weights = weights;
			this.minimumNumberMatch = minimumNumberMatch;
		}

		@Override
		public void extractTerms(Set<Term> terms) {
			for (Weight weight : weights) {
				weight.extractTerms(terms);
			}
		}

		@Override
		public Matches matches(LeafReaderContext context, int doc) throws IOException {
			LongValues minMatchValues = minimumNumberMatch.getValues(context, null);
			if ((minMatchValues.advanceExact(doc)) == false) {
				return null;
			}
			final long minimumNumberMatch = Math.max(1, minMatchValues.longValue());
			long matchCount = 0;
			List<Matches> subMatches = new ArrayList<>();
			for (Weight weight : weights) {
				Matches matches = weight.matches(context, doc);
				if (matches != null) {
					matchCount++;
					subMatches.add(matches);
				}
			}
			if (matchCount < minimumNumberMatch) {
				return null;
			}
			return Matches.fromSubMatches(subMatches);
		}

		@Override
		public Explanation explain(LeafReaderContext context, int doc) throws IOException {
			LongValues minMatchValues = minimumNumberMatch.getValues(context, null);
			if ((minMatchValues.advanceExact(doc)) == false) {
				return Explanation.noMatch("minimumNumberMatch has no value on the current document");
			}
			final long minimumNumberMatch = Math.max(1, minMatchValues.longValue());
			int freq = 0;
			double score = 0;
			List<Explanation> subExpls = new ArrayList<>();
			for (Weight weight : weights) {
				Explanation subExpl = weight.explain(context, doc);
				if (subExpl.isMatch()) {
					freq++;
					score += subExpl.getValue();
				}
				subExpls.add(subExpl);
			}
			if (freq >= minimumNumberMatch) {
				return Explanation.match(((float) (score)), (((freq + " matches for ") + minimumNumberMatch) + " required matches, sum of:"), subExpls);
			}else {
				return Explanation.noMatch((((freq + " matches for ") + minimumNumberMatch) + " required matches"), subExpls);
			}
		}

		@Override
		public Scorer scorer(LeafReaderContext context) throws IOException {
			Collection<Scorer> scorers = new ArrayList<>();
			for (Weight w : weights) {
				Scorer s = w.scorer(context);
				if (s != null) {
					scorers.add(s);
				}
			}
			if (scorers.isEmpty()) {
				return null;
			}
			return null;
		}

		@Override
		public boolean isCacheable(LeafReaderContext ctx) {
			return minimumNumberMatch.isCacheable(ctx);
		}
	}
}

