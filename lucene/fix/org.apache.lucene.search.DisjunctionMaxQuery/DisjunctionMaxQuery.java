

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Matches;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SegmentCacheable;
import org.apache.lucene.search.Weight;

import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;


public final class DisjunctionMaxQuery extends Query implements Iterable<Query> {
	private final Query[] disjuncts;

	private final float tieBreakerMultiplier;

	public DisjunctionMaxQuery(Collection<Query> disjuncts, float tieBreakerMultiplier) {
		Objects.requireNonNull(disjuncts, "Collection of Querys must not be null");
		this.tieBreakerMultiplier = tieBreakerMultiplier;
		this.disjuncts = disjuncts.toArray(new Query[disjuncts.size()]);
	}

	@Override
	public Iterator<Query> iterator() {
		return getDisjuncts().iterator();
	}

	public List<Query> getDisjuncts() {
		return Collections.unmodifiableList(Arrays.asList(disjuncts));
	}

	public float getTieBreakerMultiplier() {
		return tieBreakerMultiplier;
	}

	protected class DisjunctionMaxWeight extends Weight {
		protected final ArrayList<Weight> weights = new ArrayList<>();

		private final boolean needsScores;

		public DisjunctionMaxWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
			super(DisjunctionMaxQuery.this);
			for (Query disjunctQuery : disjuncts) {
				weights.add(searcher.createWeight(disjunctQuery, needsScores, boost));
			}
			this.needsScores = needsScores;
		}

		@Override
		public void extractTerms(Set<Term> terms) {
			for (Weight weight : weights) {
				weight.extractTerms(terms);
			}
		}

		@Override
		public Matches matches(LeafReaderContext context, int doc) throws IOException {
			List<Matches> mis = new ArrayList<>();
			for (Weight weight : weights) {
				Matches mi = weight.matches(context, doc);
				if (mi != null) {
					mis.add(mi);
				}
			}
			return Matches.fromSubMatches(mis);
		}

		@Override
		public Scorer scorer(LeafReaderContext context) throws IOException {
			List<Scorer> scorers = new ArrayList<>();
			for (Weight w : weights) {
				Scorer subScorer = w.scorer(context);
				if (subScorer != null) {
					scorers.add(subScorer);
				}
			}
			if (scorers.isEmpty()) {
				return null;
			}else
				if ((scorers.size()) == 1) {
					return scorers.get(0);
				}else {
				}

			return null;
		}

		@Override
		public boolean isCacheable(LeafReaderContext ctx) {
			for (Weight w : weights) {
				if ((w.isCacheable(ctx)) == false)
					return false;

			}
			return true;
		}

		@Override
		public Explanation explain(LeafReaderContext context, int doc) throws IOException {
			boolean match = false;
			float max = Float.NEGATIVE_INFINITY;
			float sum = 0.0F;
			List<Explanation> subs = new ArrayList<>();
			for (Weight wt : weights) {
				Explanation e = wt.explain(context, doc);
				if (e.isMatch()) {
					match = true;
					subs.add(e);
					sum += e.getValue();
					max = Math.max(max, e.getValue());
				}
			}
			if (match) {
				final float score = max + ((sum - max) * (tieBreakerMultiplier));
				final String desc = ((tieBreakerMultiplier) == 0.0F) ? "max of:" : ("max plus " + (tieBreakerMultiplier)) + " times others of:";
				return Explanation.match(score, desc, subs);
			}else {
				return Explanation.noMatch("No matching clause");
			}
		}
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
		return new DisjunctionMaxQuery.DisjunctionMaxWeight(searcher, needsScores, boost);
	}

	@Override
	public Query rewrite(IndexReader reader) throws IOException {
		if ((disjuncts.length) == 1) {
			return disjuncts[0];
		}
		if ((tieBreakerMultiplier) == 1.0F) {
			BooleanQuery.Builder builder = new BooleanQuery.Builder();
			for (Query sub : disjuncts) {
				builder.add(sub, SHOULD);
			}
			return builder.build();
		}
		boolean actuallyRewritten = false;
		List<Query> rewrittenDisjuncts = new ArrayList<>();
		for (Query sub : disjuncts) {
			Query rewrittenSub = sub.rewrite(reader);
			actuallyRewritten |= rewrittenSub != sub;
			rewrittenDisjuncts.add(rewrittenSub);
		}
		if (actuallyRewritten) {
			return new DisjunctionMaxQuery(rewrittenDisjuncts, tieBreakerMultiplier);
		}
		return super.rewrite(reader);
	}

	@Override
	public String toString(String field) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("(");
		for (int i = 0; i < (disjuncts.length); i++) {
			Query subquery = disjuncts[i];
			if (subquery instanceof BooleanQuery) {
				buffer.append("(");
				buffer.append(subquery.toString(field));
				buffer.append(")");
			}else
				buffer.append(subquery.toString(field));

			if (i != ((disjuncts.length) - 1))
				buffer.append(" | ");

		}
		buffer.append(")");
		if ((tieBreakerMultiplier) != 0.0F) {
			buffer.append("~");
			buffer.append(tieBreakerMultiplier);
		}
		return buffer.toString();
	}

	@Override
	public boolean equals(Object other) {
		return (sameClassAs(other)) && (equalsTo(getClass().cast(other)));
	}

	private boolean equalsTo(DisjunctionMaxQuery other) {
		return ((tieBreakerMultiplier) == (other.tieBreakerMultiplier)) && (Arrays.equals(disjuncts, other.disjuncts));
	}

	@Override
	public int hashCode() {
		int h = classHash();
		h = (31 * h) + (Float.floatToIntBits(tieBreakerMultiplier));
		h = (31 * h) + (Arrays.hashCode(disjuncts));
		return h;
	}
}

