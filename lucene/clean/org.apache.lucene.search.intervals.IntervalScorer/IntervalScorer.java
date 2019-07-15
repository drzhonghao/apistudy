import org.apache.lucene.search.intervals.*;


import java.io.IOException;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;

class IntervalScorer extends Scorer {

  private final IntervalIterator intervals;
  private final Similarity.SimScorer simScorer;

  private float freq = -1;
  private int lastScoredDoc = -1;

  protected IntervalScorer(Weight weight, IntervalIterator intervals, Similarity.SimScorer simScorer) {
    super(weight);
    this.intervals = intervals;
    this.simScorer = simScorer;
  }

  @Override
  public int docID() {
    return intervals.docID();
  }

  @Override
  public float score() throws IOException {
    ensureFreq();
    return simScorer.score(docID(), freq);
  }

  public Explanation explain(String topLevel) throws IOException {
    ensureFreq();
    Explanation freqExplanation = Explanation.match(freq, "intervalFreq=" + freq);
    Explanation scoreExplanation = simScorer.explain(docID(), freqExplanation);
    return Explanation.match(scoreExplanation.getValue(),
        topLevel + ", result of:",
        scoreExplanation);
  }

  public float freq() throws IOException {
    ensureFreq();
    return freq;
  }

  private void ensureFreq() throws IOException {
    if (lastScoredDoc != docID()) {
      lastScoredDoc = docID();
      freq = 0;
      do {
        freq += (1.0 / (intervals.end() - intervals.start() + 1));
      }
      while (intervals.nextInterval() != IntervalIterator.NO_MORE_INTERVALS);
    }
  }

  @Override
  public DocIdSetIterator iterator() {
    return TwoPhaseIterator.asDocIdSetIterator(twoPhaseIterator());
  }

  @Override
  public TwoPhaseIterator twoPhaseIterator() {
    return new TwoPhaseIterator(intervals) {
      @Override
      public boolean matches() throws IOException {
        return intervals.nextInterval() != IntervalIterator.NO_MORE_INTERVALS;
      }

      @Override
      public float matchCost() {
        return intervals.matchCost();
      }
    };
  }

}
