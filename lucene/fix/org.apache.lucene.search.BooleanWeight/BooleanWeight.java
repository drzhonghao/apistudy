

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BulkScorer;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Matches;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.ScorerSupplier;
import org.apache.lucene.search.SegmentCacheable;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.Bits;

import static org.apache.lucene.search.BooleanClause.Occur.FILTER;
import static org.apache.lucene.search.BooleanClause.Occur.MUST;
import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;
import static org.apache.lucene.search.BooleanClause.Occur.values;


final class BooleanWeight extends Weight {
	final Similarity similarity;

	final BooleanQuery query;

	final ArrayList<Weight> weights;

	final boolean needsScores;

	BooleanWeight(BooleanQuery query, IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
		super(query);
		this.query = query;
		this.needsScores = needsScores;
		this.similarity = searcher.getSimilarity(needsScores);
		weights = new ArrayList<>();
		for (BooleanClause c : query) {
			Weight w = searcher.createWeight(c.getQuery(), (needsScores && (c.isScoring())), boost);
			weights.add(w);
		}
	}

	@Override
	public void extractTerms(Set<Term> terms) {
		int i = 0;
		for (BooleanClause clause : query) {
			if ((clause.isScoring()) || (((needsScores) == false) && ((clause.isProhibited()) == false))) {
				weights.get(i).extractTerms(terms);
			}
			i++;
		}
	}

	@Override
	public Explanation explain(LeafReaderContext context, int doc) throws IOException {
		final int minShouldMatch = query.getMinimumNumberShouldMatch();
		List<Explanation> subs = new ArrayList<>();
		float sum = 0.0F;
		boolean fail = false;
		int matchCount = 0;
		int shouldMatchCount = 0;
		Iterator<BooleanClause> cIter = query.iterator();
		for (Iterator<Weight> wIter = weights.iterator(); wIter.hasNext();) {
			Weight w = wIter.next();
			BooleanClause c = cIter.next();
			Explanation e = w.explain(context, doc);
			if (e.isMatch()) {
				if (c.isScoring()) {
					subs.add(e);
					sum += e.getValue();
				}else
					if (c.isRequired()) {
						subs.add(Explanation.match(0.0F, "match on required clause, product of:", Explanation.match(0.0F, ((FILTER) + " clause")), e));
					}else
						if (c.isProhibited()) {
							subs.add(Explanation.noMatch((("match on prohibited clause (" + (c.getQuery().toString())) + ")"), e));
							fail = true;
						}


				if (!(c.isProhibited())) {
					matchCount++;
				}
				if ((c.getOccur()) == (SHOULD)) {
					shouldMatchCount++;
				}
			}else
				if (c.isRequired()) {
					subs.add(Explanation.noMatch((("no match on required clause (" + (c.getQuery().toString())) + ")"), e));
					fail = true;
				}

		}
		if (fail) {
			return Explanation.noMatch("Failure to meet condition(s) of required/prohibited clause(s)", subs);
		}else
			if (matchCount == 0) {
				return Explanation.noMatch("No matching clauses", subs);
			}else
				if (shouldMatchCount < minShouldMatch) {
					return Explanation.noMatch(("Failure to match minimum number of optional clauses: " + minShouldMatch), subs);
				}else {
					return Explanation.match(sum, "sum of:", subs);
				}


	}

	@Override
	public Matches matches(LeafReaderContext context, int doc) throws IOException {
		final int minShouldMatch = query.getMinimumNumberShouldMatch();
		List<Matches> matches = new ArrayList<>();
		int shouldMatchCount = 0;
		Iterator<Weight> wIt = weights.iterator();
		Iterator<BooleanClause> cIt = query.clauses().iterator();
		while (wIt.hasNext()) {
			Weight w = wIt.next();
			BooleanClause bc = cIt.next();
			Matches m = w.matches(context, doc);
			if (bc.isProhibited()) {
				if (m != null) {
					return null;
				}
			}
			if (bc.isRequired()) {
				if (m == null) {
					return null;
				}
				matches.add(m);
			}
			if ((bc.getOccur()) == (SHOULD)) {
				if (m != null) {
					matches.add(m);
					shouldMatchCount++;
				}
			}
		} 
		if (shouldMatchCount < minShouldMatch) {
			return null;
		}
		return Matches.fromSubMatches(matches);
	}

	static BulkScorer disableScoring(final BulkScorer scorer) {
		return new BulkScorer() {
			@Override
			public int score(final LeafCollector collector, Bits acceptDocs, int min, int max) throws IOException {
				return 0;
			}

			@Override
			public long cost() {
				return scorer.cost();
			}
		};
	}

	BulkScorer optionalBulkScorer(LeafReaderContext context) throws IOException {
		List<BulkScorer> optional = new ArrayList<BulkScorer>();
		Iterator<BooleanClause> cIter = query.iterator();
		for (Weight w : weights) {
			BooleanClause c = cIter.next();
			if ((c.getOccur()) != (SHOULD)) {
				continue;
			}
			BulkScorer subScorer = w.bulkScorer(context);
			if (subScorer != null) {
				optional.add(subScorer);
			}
		}
		if ((optional.size()) == 0) {
			return null;
		}
		if ((query.getMinimumNumberShouldMatch()) > (optional.size())) {
			return null;
		}
		if ((optional.size()) == 1) {
			return optional.get(0);
		}
		return null;
	}

	private BulkScorer requiredBulkScorer(LeafReaderContext context) throws IOException {
		BulkScorer scorer = null;
		Iterator<BooleanClause> cIter = query.iterator();
		for (Weight w : weights) {
			BooleanClause c = cIter.next();
			if ((c.isRequired()) == false) {
				continue;
			}
			if (scorer != null) {
				return null;
			}
			scorer = w.bulkScorer(context);
			if (scorer == null) {
				return null;
			}
			if (((c.isScoring()) == false) && (needsScores)) {
				scorer = BooleanWeight.disableScoring(scorer);
			}
		}
		return scorer;
	}

	BulkScorer booleanScorer(LeafReaderContext context) throws IOException {
		BulkScorer positiveScorer;
		positiveScorer = null;
		if (positiveScorer == null) {
			return null;
		}
		List<Scorer> prohibited = new ArrayList<>();
		Iterator<BooleanClause> cIter = query.iterator();
		for (Weight w : weights) {
			BooleanClause c = cIter.next();
			if (c.isProhibited()) {
				Scorer scorer = w.scorer(context);
				if (scorer != null) {
					prohibited.add(scorer);
				}
			}
		}
		if (prohibited.isEmpty()) {
			return positiveScorer;
		}else {
		}
		return null;
	}

	@Override
	public BulkScorer bulkScorer(LeafReaderContext context) throws IOException {
		final BulkScorer bulkScorer = booleanScorer(context);
		if (bulkScorer != null) {
			return bulkScorer;
		}else {
			return super.bulkScorer(context);
		}
	}

	@Override
	public Scorer scorer(LeafReaderContext context) throws IOException {
		ScorerSupplier scorerSupplier = scorerSupplier(context);
		if (scorerSupplier == null) {
			return null;
		}
		return scorerSupplier.get(Long.MAX_VALUE);
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
	public ScorerSupplier scorerSupplier(LeafReaderContext context) throws IOException {
		int minShouldMatch = query.getMinimumNumberShouldMatch();
		final Map<BooleanClause.Occur, Collection<ScorerSupplier>> scorers = new EnumMap<>(BooleanClause.Occur.class);
		for (BooleanClause.Occur occur : values()) {
			scorers.put(occur, new ArrayList<>());
		}
		Iterator<BooleanClause> cIter = query.iterator();
		for (Weight w : weights) {
			BooleanClause c = cIter.next();
			ScorerSupplier subScorer = w.scorerSupplier(context);
			if (subScorer == null) {
				if (c.isRequired()) {
					return null;
				}
			}else {
				scorers.get(c.getOccur()).add(subScorer);
			}
		}
		if ((scorers.get(SHOULD).size()) == minShouldMatch) {
			scorers.get(MUST).addAll(scorers.get(SHOULD));
			scorers.get(SHOULD).clear();
			minShouldMatch = 0;
		}
		if (((scorers.get(FILTER).isEmpty()) && (scorers.get(MUST).isEmpty())) && (scorers.get(SHOULD).isEmpty())) {
			return null;
		}else
			if ((scorers.get(SHOULD).size()) < minShouldMatch) {
				return null;
			}

		if (((!(needsScores)) && (minShouldMatch == 0)) && (((scorers.get(MUST).size()) + (scorers.get(FILTER).size())) > 0)) {
			scorers.get(SHOULD).clear();
		}
		return null;
	}
}

