import org.apache.accumulo.core.iterators.system.*;


import java.io.IOException;
import java.util.Comparator;
import java.util.PriorityQueue;

import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;

/**
 * Constructs a {@link PriorityQueue} of multiple SortedKeyValueIterators. Provides a simple way to
 * interact with multiple SortedKeyValueIterators in sorted order.
 */
public abstract class HeapIterator implements SortedKeyValueIterator<Key,Value> {
  private PriorityQueue<SortedKeyValueIterator<Key,Value>> heap;
  private SortedKeyValueIterator<Key,Value> topIdx = null;
  private Key nextKey;

  private static class SKVIComparator implements Comparator<SortedKeyValueIterator<Key,Value>> {

    @Override
    public int compare(SortedKeyValueIterator<Key,Value> o1, SortedKeyValueIterator<Key,Value> o2) {
      return o1.getTopKey().compareTo(o2.getTopKey());
    }
  }

  protected HeapIterator() {
    heap = null;
  }

  protected HeapIterator(int maxSize) {
    createHeap(maxSize);
  }

  protected void createHeap(int maxSize) {
    if (heap != null)
      throw new IllegalStateException("heap already exist");

    heap = new PriorityQueue<>(maxSize == 0 ? 1 : maxSize, new SKVIComparator());
  }

  @Override
  final public Key getTopKey() {
    return topIdx.getTopKey();
  }

  @Override
  final public Value getTopValue() {
    return topIdx.getTopValue();
  }

  @Override
  final public boolean hasTop() {
    return topIdx != null;
  }

  @Override
  final public void next() throws IOException {
    if (topIdx == null) {
      throw new IllegalStateException("Called next() when there is no top");
    }

    topIdx.next();
    if (!topIdx.hasTop()) {
      if (nextKey == null) {
        // No iterators left
        topIdx = null;
        return;
      }

      pullReferencesFromHeap();
    } else {
      if (nextKey == null) {
        // topIdx is the only iterator
        return;
      }

      if (nextKey.compareTo(topIdx.getTopKey()) < 0) {
        // Grab the next top iterator and put the current top iterator back on the heap
        // This updating of references is special-cased to save on percolation on edge cases
        // since the current top is guaranteed to not be the minimum
        SortedKeyValueIterator<Key,Value> nextTopIdx = heap.remove();
        heap.add(topIdx);

        topIdx = nextTopIdx;
        nextKey = heap.peek().getTopKey();
      }
    }
  }

  private void pullReferencesFromHeap() {
    topIdx = heap.remove();
    if (!heap.isEmpty()) {
      nextKey = heap.peek().getTopKey();
    } else {
      nextKey = null;
    }
  }

  final protected void clear() {
    heap.clear();
    topIdx = null;
    nextKey = null;
  }

  final protected void addSource(SortedKeyValueIterator<Key,Value> source) {
    if (source.hasTop()) {
      heap.add(source);
      if (topIdx != null) {
        heap.add(topIdx);
      }

      pullReferencesFromHeap();
    }
  }
}
