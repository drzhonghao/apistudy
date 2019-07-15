import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.*;



/** Acts like forever growing T[], but internally uses a
 *  circular buffer to reuse instances of T.
 * 
 *  @lucene.internal */
public abstract class RollingBuffer<T extends RollingBuffer.Resettable> {

  /**
   * Implement to reset an instance
   */
  public static interface Resettable {
    public void reset();
  }

  @SuppressWarnings("unchecked") private T[] buffer = (T[]) new RollingBuffer.Resettable[8];

  // Next array index to write to:
  private int nextWrite;

  // Next position to write:
  private int nextPos;

  // How many valid Position are held in the
  // array:
  private int count;

  public RollingBuffer() {
    for(int idx=0;idx<buffer.length;idx++) {
      buffer[idx] = newInstance();
    }
  }

  protected abstract T newInstance();

  public void reset() {
    nextWrite--;
    while (count > 0) {
      if (nextWrite == -1) {
        nextWrite = buffer.length - 1;
      }
      buffer[nextWrite--].reset();
      count--;
    }
    nextWrite = 0;
    nextPos = 0;
    count = 0;
  }

  // For assert:
  private boolean inBounds(int pos) {
    return pos < nextPos && pos >= nextPos - count;
  }

  private int getIndex(int pos) {
    int index = nextWrite - (nextPos - pos);
    if (index < 0) {
      index += buffer.length;
    }
    return index;
  }

  /** Get T instance for this absolute position;
   *  this is allowed to be arbitrarily far "in the
   *  future" but cannot be before the last freeBefore. */
  public T get(int pos) {
    //System.out.println("RA.get pos=" + pos + " nextPos=" + nextPos + " nextWrite=" + nextWrite + " count=" + count);
    while (pos >= nextPos) {
      if (count == buffer.length) {
        @SuppressWarnings("unchecked") T[] newBuffer = (T[]) new Resettable[ArrayUtil.oversize(1+count, RamUsageEstimator.NUM_BYTES_OBJECT_REF)];
        //System.out.println("  grow length=" + newBuffer.length);
        System.arraycopy(buffer, nextWrite, newBuffer, 0, buffer.length-nextWrite);
        System.arraycopy(buffer, 0, newBuffer, buffer.length-nextWrite, nextWrite);
        for(int i=buffer.length;i<newBuffer.length;i++) {
          newBuffer[i] = newInstance();
        }
        nextWrite = buffer.length;
        buffer = newBuffer;
      }
      if (nextWrite == buffer.length) {
        nextWrite = 0;
      }
      // Should have already been reset:
      nextWrite++;
      nextPos++;
      count++;
    }
    assert inBounds(pos): "pos=" + pos + " nextPos=" + nextPos + " count=" + count;
    final int index = getIndex(pos);
    //System.out.println("  pos=" + pos + " nextPos=" + nextPos + " -> index=" + index);
    //assert buffer[index].pos == pos;
    return buffer[index];
  }

  /** Returns the maximum position looked up, or -1 if no
  *   position has been looked up since reset/init.  */
  public int getMaxPos() {
    return nextPos-1;
  }

  /** Returns how many active positions are in the buffer. */
  public int getBufferSize() {
    return count;
  }

  public void freeBefore(int pos) {
    final int toFree = count - (nextPos - pos);
    assert toFree >= 0;
    assert toFree <= count: "toFree=" + toFree + " count=" + count;
    int index = nextWrite - count;
    if (index < 0) {
      index += buffer.length;
    }
    for(int i=0;i<toFree;i++) {
      if (index == buffer.length) {
        index = 0;
      }
      //System.out.println("  fb idx=" + index);
      buffer[index].reset();
      index++;
    }
    count -= toFree;
  }
}
