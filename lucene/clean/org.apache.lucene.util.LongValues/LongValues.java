import org.apache.lucene.util.*;


/** Abstraction over an array of longs.
 *  @lucene.internal */
public abstract class LongValues  {

  /** An instance that returns the provided value. */
  public static final LongValues IDENTITY = new LongValues() {

    @Override
    public long get(long index) {
      return index;
    }

  };

  public static final LongValues ZEROES = new LongValues() {

    @Override
    public long get(long index) {
      return 0;
    }

  };

  /** Get value at <code>index</code>. */
  public abstract long get(long index);

}
