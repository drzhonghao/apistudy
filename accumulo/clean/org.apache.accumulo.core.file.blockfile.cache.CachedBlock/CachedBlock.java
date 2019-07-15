import org.apache.accumulo.core.file.blockfile.cache.HeapSize;
import org.apache.accumulo.core.file.blockfile.cache.CacheEntry;
import org.apache.accumulo.core.file.blockfile.cache.ClassSize;
import org.apache.accumulo.core.file.blockfile.cache.SizeConstants;
import org.apache.accumulo.core.file.blockfile.cache.*;


import java.util.Objects;

/**
 * Represents an entry in the {@link LruBlockCache}.
 *
 * <p>
 * Makes the block memory-aware with {@link HeapSize} and Comparable to sort by access time for the
 * LRU. It also takes care of priority by either instantiating as in-memory or handling the
 * transition from single to multiple access.
 */
public class CachedBlock implements HeapSize, Comparable<CachedBlock>, CacheEntry {

  public final static long PER_BLOCK_OVERHEAD = ClassSize
      .align(ClassSize.OBJECT + (3 * ClassSize.REFERENCE) + (2 * SizeConstants.SIZEOF_LONG)
          + ClassSize.STRING + ClassSize.BYTE_BUFFER);

  static enum BlockPriority {
    /**
     * Accessed a single time (used for scan-resistance)
     */
    SINGLE,
    /**
     * Accessed multiple times
     */
    MULTI,
    /**
     * Block from in-memory store
     */
    MEMORY
  }

  private final String blockName;
  private final byte buf[];
  private volatile long accessTime;
  private long size;
  private BlockPriority priority;
  private Object index;

  public CachedBlock(String blockName, byte buf[], long accessTime, boolean inMemory) {
    this.blockName = blockName;
    this.buf = buf;
    this.accessTime = accessTime;
    this.size = ClassSize.align(blockName.length()) + ClassSize.align(buf.length)
        + PER_BLOCK_OVERHEAD;
    if (inMemory) {
      this.priority = BlockPriority.MEMORY;
    } else {
      this.priority = BlockPriority.SINGLE;
    }
  }

  /**
   * Block has been accessed. Update its local access time.
   */
  public void access(long accessTime) {
    this.accessTime = accessTime;
    if (this.priority == BlockPriority.SINGLE) {
      this.priority = BlockPriority.MULTI;
    }
  }

  @Override
  public long heapSize() {
    return size;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(accessTime);
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj
        || (obj != null && obj instanceof CachedBlock && 0 == compareTo((CachedBlock) obj));
  }

  @Override
  public int compareTo(CachedBlock that) {
    if (this.accessTime == that.accessTime)
      return 0;
    return this.accessTime < that.accessTime ? 1 : -1;
  }

  @Override
  public byte[] getBuffer() {
    return this.buf;
  }

  public String getName() {
    return this.blockName;
  }

  public BlockPriority getPriority() {
    return this.priority;
  }

  @Override
  public Object getIndex() {
    return index;
  }

  @Override
  public void setIndex(Object idx) {
    this.index = idx;
  }
}
