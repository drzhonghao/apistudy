

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Scorer.ChildScorer;
import org.apache.lucene.search.TwoPhaseIterator;


class ReqExclScorer extends Scorer {
	private final Scorer reqScorer;

	private final DocIdSetIterator reqApproximation;

	private final DocIdSetIterator exclApproximation;

	private final TwoPhaseIterator reqTwoPhaseIterator;

	private final TwoPhaseIterator exclTwoPhaseIterator;

	private static boolean matchesOrNull(TwoPhaseIterator it) throws IOException {
		return (it == null) || (it.matches());
	}

	@Override
	public DocIdSetIterator iterator() {
		return TwoPhaseIterator.asDocIdSetIterator(twoPhaseIterator());
	}

	@Override
	public int docID() {
		return reqApproximation.docID();
	}

	@Override
	public float score() throws IOException {
		return reqScorer.score();
	}

	@Override
	public Collection<Scorer.ChildScorer> getChildren() {
		return Collections.singleton(new Scorer.ChildScorer(reqScorer, "MUST"));
	}

	private static final int ADVANCE_COST = 10;

	private static float matchCost(DocIdSetIterator reqApproximation, TwoPhaseIterator reqTwoPhaseIterator, DocIdSetIterator exclApproximation, TwoPhaseIterator exclTwoPhaseIterator) {
		float matchCost = 2;
		if (reqTwoPhaseIterator != null) {
			matchCost += reqTwoPhaseIterator.matchCost();
		}
		final float exclMatchCost = (ReqExclScorer.ADVANCE_COST) + (exclTwoPhaseIterator == null ? 0 : exclTwoPhaseIterator.matchCost());
		float ratio;
		if ((reqApproximation.cost()) <= 0) {
			ratio = 1.0F;
		}else
			if ((exclApproximation.cost()) <= 0) {
				ratio = 0.0F;
			}else {
				ratio = ((float) (Math.min(reqApproximation.cost(), exclApproximation.cost()))) / (reqApproximation.cost());
			}

		matchCost += ratio * exclMatchCost;
		return matchCost;
	}

	@Override
	public TwoPhaseIterator twoPhaseIterator() {
		final float matchCost = ReqExclScorer.matchCost(reqApproximation, reqTwoPhaseIterator, exclApproximation, exclTwoPhaseIterator);
		if (((reqTwoPhaseIterator) == null) || (((exclTwoPhaseIterator) != null) && ((reqTwoPhaseIterator.matchCost()) <= (exclTwoPhaseIterator.matchCost())))) {
			return new TwoPhaseIterator(reqApproximation) {
				@Override
				public boolean matches() throws IOException {
					final int doc = reqApproximation.docID();
					int exclDoc = exclApproximation.docID();
					if (exclDoc < doc) {
						exclDoc = exclApproximation.advance(doc);
					}
					if (exclDoc != doc) {
						return ReqExclScorer.matchesOrNull(reqTwoPhaseIterator);
					}
					return (ReqExclScorer.matchesOrNull(reqTwoPhaseIterator)) && (!(ReqExclScorer.matchesOrNull(exclTwoPhaseIterator)));
				}

				@Override
				public float matchCost() {
					return matchCost;
				}
			};
		}else {
			return new TwoPhaseIterator(reqApproximation) {
				@Override
				public boolean matches() throws IOException {
					final int doc = reqApproximation.docID();
					int exclDoc = exclApproximation.docID();
					if (exclDoc < doc) {
						exclDoc = exclApproximation.advance(doc);
					}
					if (exclDoc != doc) {
						return ReqExclScorer.matchesOrNull(reqTwoPhaseIterator);
					}
					return (!(ReqExclScorer.matchesOrNull(exclTwoPhaseIterator))) && (ReqExclScorer.matchesOrNull(reqTwoPhaseIterator));
				}

				@Override
				public float matchCost() {
					return matchCost;
				}
			};
		}
	}
}

