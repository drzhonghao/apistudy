import org.apache.lucene.queries.function.*;


import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TwoPhaseIterator;

/**
 * {@link Scorer} which returns the result of {@link FunctionValues#floatVal(int)} as
 * the score for a document, and which filters out documents that don't match {@link #matches(int)}.
 * This Scorer has a {@link TwoPhaseIterator}.  This is similar to {@link FunctionQuery},
 * but this one has no {@link org.apache.lucene.search.Weight} normalization factors/multipliers
 * and that one doesn't filter either.
 * <p>
 * Note: If the scores are needed, then the underlying value will probably be
 * fetched/computed twice -- once to filter and next to return the score.  If that's non-trivial then
 * consider wrapping it in an implementation that will cache the current value.
 * </p>
 *
 * @see FunctionQuery
 * @lucene.experimental
 */
public abstract class ValueSourceScorer extends Scorer {
  protected final FunctionValues values;
  private final TwoPhaseIterator twoPhaseIterator;
  private final DocIdSetIterator disi;

  protected ValueSourceScorer(LeafReaderContext readerContext, FunctionValues values) {
    super(null);//no weight
    this.values = values;
    final DocIdSetIterator approximation = DocIdSetIterator.all(readerContext.reader().maxDoc()); // no approximation!
    this.twoPhaseIterator = new TwoPhaseIterator(approximation) {
      @Override
      public boolean matches() throws IOException {
        return ValueSourceScorer.this.matches(approximation.docID());
      }

      @Override
      public float matchCost() {
        return 100; // TODO: use cost of ValueSourceScorer.this.matches()
      }
    };
    this.disi = TwoPhaseIterator.asDocIdSetIterator(twoPhaseIterator);
  }

  /** Override to decide if this document matches. It's called by {@link TwoPhaseIterator#matches()}. */
  public abstract boolean matches(int doc) throws IOException;

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
    // (same as FunctionQuery, but no qWeight)  TODO consider adding configurable qWeight
    float score = values.floatVal(disi.docID());
    // Current Lucene priority queues can't handle NaN and -Infinity, so
    // map to -Float.MAX_VALUE. This conditional handles both -infinity
    // and NaN since comparisons with NaN are always false.
    return score > Float.NEGATIVE_INFINITY ? score : -Float.MAX_VALUE;
  }

}
