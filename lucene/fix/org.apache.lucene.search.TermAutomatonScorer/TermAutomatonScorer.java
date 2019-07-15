

import java.io.IOException;
import java.util.Map;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.RunAutomaton;


class TermAutomatonScorer extends Scorer {
	private final RunAutomaton runAutomaton;

	private final Map<Integer, BytesRef> idToTerm;

	private TermAutomatonScorer.PosState[] positions;

	int posShift;

	private final int anyTermID;

	private final Similarity.SimScorer docScorer;

	private int numSubsOnDoc;

	private final long cost;

	private int docID = -1;

	private int freq;

	private static class DocIDQueue {
		public DocIDQueue(int maxSize) {
		}
	}

	private static class PositionQueue {
		public PositionQueue(int maxSize) {
		}
	}

	private void pushCurrentDoc() {
		for (int i = 0; i < (numSubsOnDoc); i++) {
		}
		numSubsOnDoc = 0;
	}

	@Override
	public DocIdSetIterator iterator() {
		return new DocIdSetIterator() {
			@Override
			public int docID() {
				return docID;
			}

			@Override
			public long cost() {
				return cost;
			}

			@Override
			public int nextDoc() throws IOException {
				pushCurrentDoc();
				return doNext();
			}

			@Override
			public int advance(int target) throws IOException {
				pushCurrentDoc();
				return doNext();
			}

			private int doNext() throws IOException {
				assert (numSubsOnDoc) == 0;
				while (true) {
					if ((docID) == (DocIdSetIterator.NO_MORE_DOCS)) {
						return docID;
					}
					countMatches();
					if ((freq) > 0) {
						return docID;
					}
					pushCurrentDoc();
				} 
			}
		};
	}

	private TermAutomatonScorer.PosState getPosition(int pos) {
		return positions[(pos - (posShift))];
	}

	private void shift(int pos) {
		int limit = pos - (posShift);
		for (int i = 0; i < limit; i++) {
			positions[i].count = 0;
		}
		posShift = pos;
	}

	private void countMatches() throws IOException {
		freq = 0;
		for (int i = 0; i < (numSubsOnDoc); i++) {
		}
		int lastPos = -1;
		posShift = -1;
		int limit = (lastPos + 1) - (posShift);
		for (int i = 0; i <= limit; i++) {
			positions[i].count = 0;
		}
	}

	@Override
	public String toString() {
		return ("TermAutomatonScorer(" + (weight)) + ")";
	}

	@Override
	public int docID() {
		return docID;
	}

	@Override
	public float score() throws IOException {
		return docScorer.score(docID, freq);
	}

	final int freq() {
		return freq;
	}

	static class TermRunAutomaton extends RunAutomaton {
		public TermRunAutomaton(Automaton a, int termCount) {
			super(a, termCount);
		}
	}

	private static class PosState {
		int[] states = new int[2];

		int count;

		public void add(int state) {
			if ((states.length) == (count)) {
				states = ArrayUtil.grow(states);
			}
			states[((count)++)] = state;
		}
	}
}

