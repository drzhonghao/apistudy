import org.apache.lucene.search.suggest.*;


import java.util.Comparator;

import org.apache.lucene.search.suggest.fst.BytesRefSorter;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefArray;
import org.apache.lucene.util.BytesRefIterator;
import org.apache.lucene.util.Counter;

/**
 * An {@link BytesRefSorter} that keeps all the entries in memory.
 * @lucene.experimental
 * @lucene.internal
 */
public final class InMemorySorter implements BytesRefSorter {
  private final BytesRefArray buffer = new BytesRefArray(Counter.newCounter());
  private boolean closed = false;
  private final Comparator<BytesRef> comparator;

  /**
   * Creates an InMemorySorter, sorting entries by the
   * provided comparator.
   */
  public InMemorySorter(Comparator<BytesRef> comparator) {
    this.comparator = comparator;
  }
  
  @Override
  public void add(BytesRef utf8) {
    if (closed) throw new IllegalStateException();
    buffer.append(utf8);
  }

  @Override
  public BytesRefIterator iterator() {
    closed = true;
    return buffer.iterator(comparator);
  }

  @Override
  public Comparator<BytesRef> getComparator() {
    return comparator;
  }
}
