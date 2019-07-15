import org.apache.lucene.search.join.*;


import java.io.IOException;

import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.search.Weight;

abstract class BaseGlobalOrdinalScorer extends Scorer {

  final SortedDocValues values;
  final DocIdSetIterator approximation;

  float score;

  public BaseGlobalOrdinalScorer(Weight weight, SortedDocValues values, DocIdSetIterator approximationScorer) {
    super(weight);
    this.values = values;
    this.approximation = approximationScorer;
  }

  @Override
  public float score() throws IOException {
    return score;
  }

  @Override
  public int docID() {
    return approximation.docID();
  }

  @Override
  public DocIdSetIterator iterator() {
    return TwoPhaseIterator.asDocIdSetIterator(twoPhaseIterator());
  }

  @Override
  public TwoPhaseIterator twoPhaseIterator() {
    return createTwoPhaseIterator(approximation);
  }

  protected abstract TwoPhaseIterator createTwoPhaseIterator(DocIdSetIterator approximation);

}
