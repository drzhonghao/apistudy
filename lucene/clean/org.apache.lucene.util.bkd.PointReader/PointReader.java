import org.apache.lucene.util.bkd.*;



import java.io.Closeable;
import java.io.IOException;

import org.apache.lucene.util.LongBitSet;

/** One pass iterator through all points previously written with a
 *  {@link PointWriter}, abstracting away whether points a read
 *  from (offline) disk or simple arrays in heap.
 *
 * @lucene.internal */
public abstract class PointReader implements Closeable {

  /** Returns false once iteration is done, else true. */
  public abstract boolean next() throws IOException;

  /** Returns the packed byte[] value */
  public abstract byte[] packedValue();

  /** Point ordinal */
  public abstract long ord();

  /** DocID for this point */
  public abstract int docID();

  /** Iterates through the next {@code count} ords, marking them in the provided {@code ordBitSet}. */
  public void markOrds(long count, LongBitSet ordBitSet) throws IOException {
    for(int i=0;i<count;i++) {
      boolean result = next();
      if (result == false) {
        throw new IllegalStateException("did not see enough points from reader=" + this);
      }
      assert ordBitSet.get(ord()) == false: "ord=" + ord() + " was seen twice from " + this;
      ordBitSet.set(ord());
    }
  }

  /** Splits this reader into left and right partitions */
  public long split(long count, LongBitSet rightTree, PointWriter left, PointWriter right, boolean doClearBits) throws IOException {

    // Partition this source according to how the splitDim split the values:
    long rightCount = 0;
    for (long i=0;i<count;i++) {
      boolean result = next();
      assert result;
      byte[] packedValue = packedValue();
      long ord = ord();
      int docID = docID();
      if (rightTree.get(ord)) {
        right.append(packedValue, ord, docID);
        rightCount++;
        if (doClearBits) {
          rightTree.clear(ord);
        }
      } else {
        left.append(packedValue, ord, docID);
      }
    }

    return rightCount;
  }
}

