import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.*;



import java.io.IOException;

/**
 * A constant-scoring {@link Scorer}.
 * @lucene.internal
 */
public final class ConstantScoreScorer extends Scorer {

  private final float score;
  private final TwoPhaseIterator twoPhaseIterator;
  private final DocIdSetIterator disi;

  /** Constructor based on a {@link DocIdSetIterator} which will be used to
   *  drive iteration. Two phase iteration will not be supported.
   *  @param weight the parent weight
   *  @param score the score to return on each document
   *  @param disi the iterator that defines matching documents */
  public ConstantScoreScorer(Weight weight, float score, DocIdSetIterator disi) {
    super(weight);
    this.score = score;
    this.twoPhaseIterator = null;
    this.disi = disi;
  }

  /** Constructor based on a {@link TwoPhaseIterator}. In that case the
   *  {@link Scorer} will support two-phase iteration.
   *  @param weight the parent weight
   *  @param score the score to return on each document
   *  @param twoPhaseIterator the iterator that defines matching documents */
  public ConstantScoreScorer(Weight weight, float score, TwoPhaseIterator twoPhaseIterator) {
    super(weight);
    this.score = score;
    this.twoPhaseIterator = twoPhaseIterator;
    this.disi = TwoPhaseIterator.asDocIdSetIterator(twoPhaseIterator);
  }

  @Override
  public DocIdSetIterator iterator() {
    return disi;
  }

  @Override
  public TwoPhaseIterator twoPhaseIterator() {
    return twoPhaseIterator;
  }

  @Override
  public int docID() {
    return disi.docID();
  }

  @Override
  public float score() throws IOException {
    return score;
  }

}

