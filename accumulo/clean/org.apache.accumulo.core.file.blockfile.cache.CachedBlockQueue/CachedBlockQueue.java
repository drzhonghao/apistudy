import org.apache.accumulo.core.file.blockfile.cache.HeapSize;
import org.apache.accumulo.core.file.blockfile.cache.CachedBlock;
import org.apache.accumulo.core.file.blockfile.cache.*;


import java.util.LinkedList;
import java.util.PriorityQueue;

/**
 * A memory-bound queue that will grow until an element brings total size &gt;= maxSize. From then
 * on, only entries that are sorted larger than the smallest current entry will be
 * inserted/replaced.
 *
 * <p>
 * Use this when you want to find the largest elements (according to their ordering, not their heap
 * size) that consume as close to the specified maxSize as possible. Default behavior is to grow
 * just above rather than just below specified max.
 *
 * <p>
 * Object used in this queue must implement {@link HeapSize} as well as {@link Comparable}.
 */
public class CachedBlockQueue implements HeapSize {

  private PriorityQueue<CachedBlock> queue;

  private long heapSize;
  private long maxSize;

  /**
   * @param maxSize
   *          the target size of elements in the queue
   * @param blockSize
   *          expected average size of blocks
   */
  public CachedBlockQueue(long maxSize, long blockSize) {
    int initialSize = (int) Math.ceil(maxSize / (double) blockSize);
    if (initialSize == 0)
      initialSize++;
    queue = new PriorityQueue<>(initialSize);
    heapSize = 0;
    this.maxSize = maxSize;
  }

  /**
   * Attempt to add the specified cached block to this queue.
   *
   * <p>
   * If the queue is smaller than the max size, or if the specified element is ordered before the
   * smallest element in the queue, the element will be added to the queue. Otherwise, there is no
   * side effect of this call.
   *
   * @param cb
   *          block to try to add to the queue
   */
  public void add(CachedBlock cb) {
    if (heapSize < maxSize) {
      queue.add(cb);
      heapSize += cb.heapSize();
    } else {
      CachedBlock head = queue.peek();
      if (cb.compareTo(head) > 0) {
        heapSize += cb.heapSize();
        heapSize -= head.heapSize();
        if (heapSize > maxSize) {
          queue.poll();
        } else {
          heapSize += head.heapSize();
        }
        queue.add(cb);
      }
    }
  }

  /**
   * Get a sorted List of all elements in this queue, in descending order.
   *
   * @return list of cached elements in descending order
   */
  public CachedBlock[] get() {
    LinkedList<CachedBlock> blocks = new LinkedList<>();
    while (!queue.isEmpty()) {
      blocks.addFirst(queue.poll());
    }
    return blocks.toArray(new CachedBlock[blocks.size()]);
  }

  /**
   * Get a sorted List of all elements in this queue, in descending order.
   *
   * @return list of cached elements in descending order
   */
  public LinkedList<CachedBlock> getList() {
    LinkedList<CachedBlock> blocks = new LinkedList<>();
    while (!queue.isEmpty()) {
      blocks.addFirst(queue.poll());
    }
    return blocks;
  }

  /**
   * Total size of all elements in this queue.
   *
   * @return size of all elements currently in queue, in bytes
   */
  @Override
  public long heapSize() {
    return heapSize;
  }
}
