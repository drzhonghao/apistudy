

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Scorer.ChildScorer;
import org.apache.lucene.search.TwoPhaseIterator;


class ReqOptSumScorer extends Scorer {
	protected final Scorer reqScorer;

	protected final Scorer optScorer;

	protected final DocIdSetIterator optIterator;

	@Override
	public TwoPhaseIterator twoPhaseIterator() {
		return reqScorer.twoPhaseIterator();
	}

	@Override
	public DocIdSetIterator iterator() {
		return reqScorer.iterator();
	}

	@Override
	public int docID() {
		return reqScorer.docID();
	}

	@Override
	public float score() throws IOException {
		int curDoc = reqScorer.docID();
		float score = reqScorer.score();
		int optScorerDoc = optIterator.docID();
		if (optScorerDoc < curDoc) {
			optScorerDoc = optIterator.advance(curDoc);
		}
		if (optScorerDoc == curDoc) {
			score += optScorer.score();
		}
		return score;
	}

	@Override
	public Collection<Scorer.ChildScorer> getChildren() {
		ArrayList<Scorer.ChildScorer> children = new ArrayList<>(2);
		children.add(new Scorer.ChildScorer(reqScorer, "MUST"));
		children.add(new Scorer.ChildScorer(optScorer, "SHOULD"));
		return children;
	}
}

