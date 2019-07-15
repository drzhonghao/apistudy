

import java.io.IOException;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;


class PhraseScorer extends Scorer {
	final boolean needsScores;

	private final Similarity.SimScorer simScorer;

	final float matchCost;

	private float freq = 0;

	@Override
	public TwoPhaseIterator twoPhaseIterator() {
		return null;
	}

	@Override
	public float score() throws IOException {
		if ((freq) == 0) {
		}
		return simScorer.score(docID(), freq);
	}

	@Override
	public DocIdSetIterator iterator() {
		return TwoPhaseIterator.asDocIdSetIterator(twoPhaseIterator());
	}

	@Override
	public String toString() {
		return ("PhraseScorer(" + (weight)) + ")";
	}
}

