import org.apache.lucene.search.intervals.*;


import java.io.IOException;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.TwoPhaseIterator;

/**
 * A {@link DocIdSetIterator} that also allows iteration over matching
 * intervals in a document.
 *
 * Once the iterator is positioned on a document by calling {@link #advance(int)}
 * or {@link #nextDoc()}, intervals may be retrieved by calling {@link #nextInterval()}
 * until {@link #NO_MORE_INTERVALS} is returned.
 *
 * The limits of the current interval are returned by {@link #start()} and {@link #end()}.
 * When the iterator has been moved to a new document, but before {@link #nextInterval()}
 * has been called, both these methods return {@code -1}.
 *
 * Note that it is possible for a document to return {@link #NO_MORE_INTERVALS}
 * on the first call to {@link #nextInterval()}
 */
public abstract class IntervalIterator extends DocIdSetIterator {

  /**
   * When returned from {@link #nextInterval()}, indicates that there are no more
   * matching intervals on the current document
   */
  public static final int NO_MORE_INTERVALS = Integer.MAX_VALUE;

  /**
   * The start of the current interval
   *
   * Returns -1 if {@link #nextInterval()} has not yet been called
   */
  public abstract int start();

  /**
   * The end of the current interval
   *
   * Returns -1 if {@link #nextInterval()} has not yet been called
   */
  public abstract int end();

  /**
   * Advance the iterator to the next interval
   *
   * @return the start of the next interval, or {@link IntervalIterator#NO_MORE_INTERVALS} if
   *         there are no more intervals on the current document
   */
  public abstract int nextInterval() throws IOException;

  /**
   * An indication of the average cost of iterating over all intervals in a document
   *
   * @see TwoPhaseIterator#matchCost()
   */
  public abstract float matchCost();

}
