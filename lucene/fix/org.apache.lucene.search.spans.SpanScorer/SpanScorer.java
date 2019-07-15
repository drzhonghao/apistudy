

import java.io.IOException;
import java.util.Objects;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.spans.SpanWeight;
import org.apache.lucene.search.spans.Spans;


public class SpanScorer extends Scorer {
	protected final Spans spans;

	protected final Similarity.SimScorer docScorer;

	private float freq;

	private int numMatches;

	private int lastScoredDoc = -1;

	public SpanScorer(SpanWeight weight, Spans spans, Similarity.SimScorer docScorer) {
		super(weight);
		this.spans = Objects.requireNonNull(spans);
		this.docScorer = docScorer;
	}

	public Spans getSpans() {
		return spans;
	}

	@Override
	public int docID() {
		return spans.docID();
	}

	@Override
	public DocIdSetIterator iterator() {
		return spans;
	}

	@Override
	public TwoPhaseIterator twoPhaseIterator() {
		return spans.asTwoPhaseIterator();
	}

	protected float scoreCurrentDoc() throws IOException {
		assert (docScorer) != null : (getClass()) + " has a null docScorer!";
		return docScorer.score(docID(), freq);
	}

	protected final void setFreqCurrentDoc() throws IOException {
		freq = 0.0F;
		numMatches = 0;
		assert (spans.startPosition()) == (-1) : "incorrect initial start position, " + (spans);
		assert (spans.endPosition()) == (-1) : "incorrect initial end position, " + (spans);
		int prevStartPos = -1;
		int prevEndPos = -1;
		int startPos = spans.nextStartPosition();
		assert startPos != (Spans.NO_MORE_POSITIONS) : "initial startPos NO_MORE_POSITIONS, " + (spans);
		do {
			assert startPos >= prevStartPos;
			int endPos = spans.endPosition();
			assert endPos != (Spans.NO_MORE_POSITIONS);
			assert (startPos != prevStartPos) || (endPos >= prevEndPos) : "decreased endPos=" + endPos;
			(numMatches)++;
			if ((docScorer) == null) {
				freq = 1;
				return;
			}
			freq += docScorer.computeSlopFactor(spans.width());
			prevStartPos = startPos;
			prevEndPos = endPos;
			startPos = spans.nextStartPosition();
		} while (startPos != (Spans.NO_MORE_POSITIONS) );
		assert (spans.startPosition()) == (Spans.NO_MORE_POSITIONS) : "incorrect final start position, " + (spans);
		assert (spans.endPosition()) == (Spans.NO_MORE_POSITIONS) : "incorrect final end position, " + (spans);
	}

	private void ensureFreq() throws IOException {
		int currentDoc = docID();
		if ((lastScoredDoc) != currentDoc) {
			setFreqCurrentDoc();
			lastScoredDoc = currentDoc;
		}
	}

	@Override
	public final float score() throws IOException {
		ensureFreq();
		return scoreCurrentDoc();
	}

	final float sloppyFreq() throws IOException {
		ensureFreq();
		return freq;
	}
}

