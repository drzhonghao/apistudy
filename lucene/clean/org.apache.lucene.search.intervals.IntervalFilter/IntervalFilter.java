import org.apache.lucene.search.intervals.IntervalIterator;
import org.apache.lucene.search.intervals.*;


import java.io.IOException;
import java.util.Objects;

/**
 * Wraps an {@link IntervalIterator} and passes through those intervals that match the {@link #accept()} function
 */
public abstract class IntervalFilter extends IntervalIterator {

  private final IntervalIterator in;

  /**
   * Create a new filter
   */
  public IntervalFilter(IntervalIterator in) {
    this.in = Objects.requireNonNull(in);
  }

  @Override
  public int docID() {
    return in.docID();
  }

  @Override
  public int nextDoc() throws IOException {
    return in.nextDoc();
  }

  @Override
  public int advance(int target) throws IOException {
    return in.advance(target);
  }

  @Override
  public long cost() {
    return in.cost();
  }

  @Override
  public int start() {
    return in.start();
  }

  @Override
  public int end() {
    return in.end();
  }

  @Override
  public float matchCost() {
    return in.matchCost();
  }

  /**
   * @return {@code true} if the wrapped iterator's interval should be passed on
   */
  protected abstract boolean accept();

  @Override
  public final int nextInterval() throws IOException {
    int next;
    do {
      next = in.nextInterval();
    }
    while (accept() == false && next != IntervalIterator.NO_MORE_INTERVALS);
    return next;
  }

}
