

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Weight;

import static org.apache.lucene.search.BooleanClause.Occur.FILTER;
import static org.apache.lucene.search.BooleanClause.Occur.MUST;
import static org.apache.lucene.search.BooleanClause.Occur.MUST_NOT;
import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;


public class BooleanQuery extends Query implements Iterable<BooleanClause> {
	private static int maxClauseCount = 1024;

	public static class TooManyClauses extends RuntimeException {
		public TooManyClauses() {
			super(("maxClauseCount is set to " + (BooleanQuery.maxClauseCount)));
		}
	}

	public static int getMaxClauseCount() {
		return BooleanQuery.maxClauseCount;
	}

	public static void setMaxClauseCount(int maxClauseCount) {
		if (maxClauseCount < 1) {
			throw new IllegalArgumentException("maxClauseCount must be >= 1");
		}
		BooleanQuery.maxClauseCount = maxClauseCount;
	}

	public static class Builder {
		private int minimumNumberShouldMatch;

		private final List<BooleanClause> clauses = new ArrayList<>();

		public Builder() {
		}

		public BooleanQuery.Builder setMinimumNumberShouldMatch(int min) {
			this.minimumNumberShouldMatch = min;
			return this;
		}

		public BooleanQuery.Builder add(BooleanClause clause) {
			if ((clauses.size()) >= (BooleanQuery.maxClauseCount)) {
				throw new BooleanQuery.TooManyClauses();
			}
			clauses.add(clause);
			return this;
		}

		public BooleanQuery.Builder add(Query query, BooleanClause.Occur occur) {
			return add(new BooleanClause(query, occur));
		}

		public BooleanQuery build() {
			return new BooleanQuery(minimumNumberShouldMatch, clauses.toArray(new BooleanClause[0]));
		}
	}

	private final int minimumNumberShouldMatch;

	private final List<BooleanClause> clauses;

	private final Map<BooleanClause.Occur, Collection<Query>> clauseSets;

	private BooleanQuery(int minimumNumberShouldMatch, BooleanClause[] clauses) {
		this.minimumNumberShouldMatch = minimumNumberShouldMatch;
		this.clauses = Collections.unmodifiableList(Arrays.asList(clauses));
		clauseSets = new EnumMap<>(BooleanClause.Occur.class);
		clauseSets.put(FILTER, new HashSet<>());
		clauseSets.put(MUST_NOT, new HashSet<>());
		for (BooleanClause clause : clauses) {
			clauseSets.get(clause.getOccur()).add(clause.getQuery());
		}
	}

	public int getMinimumNumberShouldMatch() {
		return minimumNumberShouldMatch;
	}

	public List<BooleanClause> clauses() {
		return clauses;
	}

	Collection<Query> getClauses(BooleanClause.Occur occur) {
		return clauseSets.get(occur);
	}

	@Override
	public final Iterator<BooleanClause> iterator() {
		return clauses.iterator();
	}

	private BooleanQuery rewriteNoScoring() {
		if ((clauseSets.get(MUST).size()) == 0) {
			return this;
		}
		BooleanQuery.Builder newQuery = new BooleanQuery.Builder();
		newQuery.setMinimumNumberShouldMatch(getMinimumNumberShouldMatch());
		for (BooleanClause clause : clauses) {
			if ((clause.getOccur()) == (MUST)) {
				newQuery.add(clause.getQuery(), FILTER);
			}else {
				newQuery.add(clause);
			}
		}
		return newQuery.build();
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
		BooleanQuery query = this;
		if (needsScores == false) {
			query = rewriteNoScoring();
		}
		return null;
	}

	@Override
	public Query rewrite(IndexReader reader) throws IOException {
		if ((clauses.size()) == 0) {
			return new MatchNoDocsQuery("empty BooleanQuery");
		}
		if ((clauses.size()) == 1) {
			BooleanClause c = clauses.get(0);
			Query query = c.getQuery();
			if (((minimumNumberShouldMatch) == 1) && ((c.getOccur()) == (SHOULD))) {
				return query;
			}else
				if ((minimumNumberShouldMatch) == 0) {
					switch (c.getOccur()) {
						case SHOULD :
						case MUST :
							return query;
						case FILTER :
							return new BoostQuery(new ConstantScoreQuery(query), 0);
						case MUST_NOT :
							return new MatchNoDocsQuery("pure negative BooleanQuery");
						default :
							throw new AssertionError();
					}
				}

		}
		{
			BooleanQuery.Builder builder = new BooleanQuery.Builder();
			builder.setMinimumNumberShouldMatch(getMinimumNumberShouldMatch());
			boolean actuallyRewritten = false;
			for (BooleanClause clause : this) {
				Query query = clause.getQuery();
				Query rewritten = query.rewrite(reader);
				if (rewritten != query) {
					actuallyRewritten = true;
				}
				builder.add(rewritten, clause.getOccur());
			}
			if (actuallyRewritten) {
				return builder.build();
			}
		}
		{
			int clauseCount = 0;
			for (Collection<Query> queries : clauseSets.values()) {
				clauseCount += queries.size();
			}
			if (clauseCount != (clauses.size())) {
				BooleanQuery.Builder rewritten = new BooleanQuery.Builder();
				rewritten.setMinimumNumberShouldMatch(minimumNumberShouldMatch);
				for (Map.Entry<BooleanClause.Occur, Collection<Query>> entry : clauseSets.entrySet()) {
					final BooleanClause.Occur occur = entry.getKey();
					for (Query query : entry.getValue()) {
						rewritten.add(query, occur);
					}
				}
				return rewritten.build();
			}
		}
		final Collection<Query> mustNotClauses = clauseSets.get(MUST_NOT);
		if (!(mustNotClauses.isEmpty())) {
			final Predicate<Query> p = clauseSets.get(MUST)::contains;
			if (mustNotClauses.stream().anyMatch(p.or(clauseSets.get(FILTER)::contains))) {
				return new MatchNoDocsQuery("FILTER or MUST clause also in MUST_NOT");
			}
			if (mustNotClauses.contains(new MatchAllDocsQuery())) {
				return new MatchNoDocsQuery("MUST_NOT clause is MatchAllDocsQuery");
			}
		}
		if (((clauseSets.get(MUST).size()) > 0) && ((clauseSets.get(FILTER).size()) > 0)) {
			final Set<Query> filters = new HashSet<Query>(clauseSets.get(FILTER));
			boolean modified = filters.remove(new MatchAllDocsQuery());
			modified |= filters.removeAll(clauseSets.get(MUST));
			if (modified) {
				BooleanQuery.Builder builder = new BooleanQuery.Builder();
				builder.setMinimumNumberShouldMatch(getMinimumNumberShouldMatch());
				for (BooleanClause clause : clauses) {
					if ((clause.getOccur()) != (FILTER)) {
						builder.add(clause);
					}
				}
				for (Query filter : filters) {
					builder.add(filter, FILTER);
				}
				return builder.build();
			}
		}
		if (((clauseSets.get(SHOULD).size()) > 0) && ((clauseSets.get(FILTER).size()) > 0)) {
			final Collection<Query> filters = clauseSets.get(FILTER);
			final Collection<Query> shoulds = clauseSets.get(SHOULD);
			Set<Query> intersection = new HashSet<>(filters);
			intersection.retainAll(shoulds);
			if ((intersection.isEmpty()) == false) {
				BooleanQuery.Builder builder = new BooleanQuery.Builder();
				int minShouldMatch = getMinimumNumberShouldMatch();
				for (BooleanClause clause : clauses) {
					if (intersection.contains(clause.getQuery())) {
						if ((clause.getOccur()) == (SHOULD)) {
							builder.add(new BooleanClause(clause.getQuery(), MUST));
							minShouldMatch--;
						}
					}else {
						builder.add(clause);
					}
				}
				builder.setMinimumNumberShouldMatch(Math.max(0, minShouldMatch));
				return builder.build();
			}
		}
		if (((clauseSets.get(SHOULD).size()) > 0) && ((minimumNumberShouldMatch) <= 1)) {
			Map<Query, Double> shouldClauses = new HashMap<>();
			for (Query query : clauseSets.get(SHOULD)) {
				double boost = 1;
				while (query instanceof BoostQuery) {
					BoostQuery bq = ((BoostQuery) (query));
					boost *= bq.getBoost();
					query = bq.getQuery();
				} 
				shouldClauses.put(query, ((shouldClauses.getOrDefault(query, 0.0)) + boost));
			}
			if ((shouldClauses.size()) != (clauseSets.get(SHOULD).size())) {
				BooleanQuery.Builder builder = new BooleanQuery.Builder().setMinimumNumberShouldMatch(minimumNumberShouldMatch);
				for (Map.Entry<Query, Double> entry : shouldClauses.entrySet()) {
					Query query = entry.getKey();
					float boost = entry.getValue().floatValue();
					if (boost != 1.0F) {
						query = new BoostQuery(query, boost);
					}
					builder.add(query, SHOULD);
				}
				for (BooleanClause clause : clauses) {
					if ((clause.getOccur()) != (SHOULD)) {
						builder.add(clause);
					}
				}
				return builder.build();
			}
		}
		if ((clauseSets.get(MUST).size()) > 0) {
			Map<Query, Double> mustClauses = new HashMap<>();
			for (Query query : clauseSets.get(MUST)) {
				double boost = 1;
				while (query instanceof BoostQuery) {
					BoostQuery bq = ((BoostQuery) (query));
					boost *= bq.getBoost();
					query = bq.getQuery();
				} 
				mustClauses.put(query, ((mustClauses.getOrDefault(query, 0.0)) + boost));
			}
			if ((mustClauses.size()) != (clauseSets.get(MUST).size())) {
				BooleanQuery.Builder builder = new BooleanQuery.Builder().setMinimumNumberShouldMatch(minimumNumberShouldMatch);
				for (Map.Entry<Query, Double> entry : mustClauses.entrySet()) {
					Query query = entry.getKey();
					float boost = entry.getValue().floatValue();
					if (boost != 1.0F) {
						query = new BoostQuery(query, boost);
					}
					builder.add(query, MUST);
				}
				for (BooleanClause clause : clauses) {
					if ((clause.getOccur()) != (MUST)) {
						builder.add(clause);
					}
				}
				return builder.build();
			}
		}
		{
			final Collection<Query> musts = clauseSets.get(MUST);
			final Collection<Query> filters = clauseSets.get(FILTER);
			if (((musts.size()) == 1) && ((filters.size()) > 0)) {
				Query must = musts.iterator().next();
				float boost = 1.0F;
				if (must instanceof BoostQuery) {
					BoostQuery boostQuery = ((BoostQuery) (must));
					must = boostQuery.getQuery();
					boost = boostQuery.getBoost();
				}
				if ((must.getClass()) == (MatchAllDocsQuery.class)) {
					BooleanQuery.Builder builder = new BooleanQuery.Builder();
					for (BooleanClause clause : clauses) {
						switch (clause.getOccur()) {
							case FILTER :
							case MUST_NOT :
								builder.add(clause);
								break;
							default :
								break;
						}
					}
					Query rewritten = builder.build();
					rewritten = new ConstantScoreQuery(rewritten);
					if (boost != 1.0F) {
						rewritten = new BoostQuery(rewritten, boost);
					}
					builder = new BooleanQuery.Builder().setMinimumNumberShouldMatch(getMinimumNumberShouldMatch()).add(rewritten, MUST);
					for (Query query : clauseSets.get(SHOULD)) {
						builder.add(query, SHOULD);
					}
					rewritten = builder.build();
					return rewritten;
				}
			}
		}
		return super.rewrite(reader);
	}

	@Override
	public String toString(String field) {
		StringBuilder buffer = new StringBuilder();
		boolean needParens = (getMinimumNumberShouldMatch()) > 0;
		if (needParens) {
			buffer.append("(");
		}
		int i = 0;
		for (BooleanClause c : this) {
			buffer.append(c.getOccur().toString());
			Query subQuery = c.getQuery();
			if (subQuery instanceof BooleanQuery) {
				buffer.append("(");
				buffer.append(subQuery.toString(field));
				buffer.append(")");
			}else {
				buffer.append(subQuery.toString(field));
			}
			if (i != ((clauses.size()) - 1)) {
				buffer.append(" ");
			}
			i += 1;
		}
		if (needParens) {
			buffer.append(")");
		}
		if ((getMinimumNumberShouldMatch()) > 0) {
			buffer.append('~');
			buffer.append(getMinimumNumberShouldMatch());
		}
		return buffer.toString();
	}

	@Override
	public boolean equals(Object o) {
		return (sameClassAs(o)) && (equalsTo(getClass().cast(o)));
	}

	private boolean equalsTo(BooleanQuery other) {
		return ((getMinimumNumberShouldMatch()) == (other.getMinimumNumberShouldMatch())) && (clauseSets.equals(other.clauseSets));
	}

	private int computeHashCode() {
		int hashCode = Objects.hash(minimumNumberShouldMatch, clauseSets);
		if (hashCode == 0) {
			hashCode = 1;
		}
		return hashCode;
	}

	private int hashCode;

	@Override
	public int hashCode() {
		if ((hashCode) == 0) {
			hashCode = computeHashCode();
			assert (hashCode) != 0;
		}
		assert (hashCode) == (computeHashCode());
		return hashCode;
	}
}

