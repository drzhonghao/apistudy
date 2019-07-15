import org.apache.lucene.index.*;


import org.apache.lucene.util.Bits;
import org.apache.lucene.util.FutureObjects;


/**
 * Exposes a slice of an existing Bits as a new Bits.
 *
 * @lucene.internal
 */
final class BitsSlice implements Bits {
  private final Bits parent;
  private final int start;
  private final int length;

  // start is inclusive; end is exclusive (length = end-start)
  public BitsSlice(Bits parent, ReaderSlice slice) {
    this.parent = parent;
    this.start = slice.start;
    this.length = slice.length;
    assert length >= 0: "length=" + length;
  }
    
  @Override
  public boolean get(int doc) {
    FutureObjects.checkIndex(doc, length);
    return parent.get(doc+start);
  }

  @Override
  public int length() {
    return length;
  }
}
