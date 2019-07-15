

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.function.ToLongFunction;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.FilterScorer;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.ScorerSupplier;

import static org.apache.lucene.search.BooleanClause.Occur.FILTER;
import static org.apache.lucene.search.BooleanClause.Occur.MUST;
import static org.apache.lucene.search.BooleanClause.Occur.MUST_NOT;
import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;


final class Boolean2ScorerSupplier extends ScorerSupplier {
	private final Map<BooleanClause.Occur, Collection<ScorerSupplier>> subs = null;

	private final boolean needsScores = false;

	private final int minShouldMatch = 0;

	private long cost = -1;

	private long computeCost() {
		OptionalLong minRequiredCost = Stream.concat(subs.get(MUST).stream(), subs.get(FILTER).stream()).mapToLong(ScorerSupplier::cost).min();
		if ((minRequiredCost.isPresent()) && ((minShouldMatch) == 0)) {
			return minRequiredCost.getAsLong();
		}else {
			final Collection<ScorerSupplier> optionalScorers = subs.get(SHOULD);
		}
		return 0l;
	}

	@Override
	public long cost() {
		if ((cost) == (-1)) {
			cost = computeCost();
		}
		return cost;
	}

	@Override
	public Scorer get(long leadCost) throws IOException {
		leadCost = Math.min(leadCost, cost());
		if (subs.get(SHOULD).isEmpty()) {
			return excl(req(subs.get(FILTER), subs.get(MUST), leadCost), subs.get(MUST_NOT), leadCost);
		}
		if ((subs.get(FILTER).isEmpty()) && (subs.get(MUST).isEmpty())) {
			return excl(opt(subs.get(SHOULD), minShouldMatch, needsScores, leadCost), subs.get(MUST_NOT), leadCost);
		}
		if ((minShouldMatch) > 0) {
			Scorer req = excl(req(subs.get(FILTER), subs.get(MUST), leadCost), subs.get(MUST_NOT), leadCost);
			Scorer opt = opt(subs.get(SHOULD), minShouldMatch, needsScores, leadCost);
		}else {
			assert needsScores;
		}
		return null;
	}

	private Scorer req(Collection<ScorerSupplier> requiredNoScoring, Collection<ScorerSupplier> requiredScoring, long leadCost) throws IOException {
		if (((requiredNoScoring.size()) + (requiredScoring.size())) == 1) {
			Scorer req = (requiredNoScoring.isEmpty() ? requiredScoring : requiredNoScoring).iterator().next().get(leadCost);
			if ((needsScores) == false) {
				return req;
			}
			if (requiredScoring.isEmpty()) {
				return new FilterScorer(req) {
					@Override
					public float score() throws IOException {
						return 0.0F;
					}
				};
			}
			return req;
		}else {
			List<Scorer> requiredScorers = new ArrayList<>();
			List<Scorer> scoringScorers = new ArrayList<>();
			for (ScorerSupplier s : requiredNoScoring) {
				requiredScorers.add(s.get(leadCost));
			}
			for (ScorerSupplier s : requiredScoring) {
				Scorer scorer = s.get(leadCost);
				requiredScorers.add(scorer);
				scoringScorers.add(scorer);
			}
		}
		return null;
	}

	private Scorer excl(Scorer main, Collection<ScorerSupplier> prohibited, long leadCost) throws IOException {
		if (prohibited.isEmpty()) {
			return main;
		}else {
		}
		return null;
	}

	private Scorer opt(Collection<ScorerSupplier> optional, int minShouldMatch, boolean needsScores, long leadCost) throws IOException {
		if ((optional.size()) == 1) {
			return optional.iterator().next().get(leadCost);
		}else {
			final List<Scorer> optionalScorers = new ArrayList<>();
			for (ScorerSupplier scorer : optional) {
				optionalScorers.add(scorer.get(leadCost));
			}
			if (minShouldMatch > 1) {
			}else {
			}
		}
		return null;
	}
}

