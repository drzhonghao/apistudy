import org.apache.lucene.search.*;


import java.io.IOException;

/**
 * Per-segment, per-document double values, which can be calculated at search-time
 */
public abstract class DoubleValues {

  /**
   * Get the double value for the current document
   */
  public abstract double doubleValue() throws IOException;

  /**
   * Advance this instance to the given document id
   * @return true if there is a value for this document
   */
  public abstract boolean advanceExact(int doc) throws IOException;

  /**
   * Wrap a DoubleValues instance, returning a default if the wrapped instance has no value
   */
  public static DoubleValues withDefault(DoubleValues in, double missingValue) {
    return new DoubleValues() {

      boolean hasValue = false;

      @Override
      public double doubleValue() throws IOException {
        return hasValue ? in.doubleValue() : missingValue;
      }

      @Override
      public boolean advanceExact(int doc) throws IOException {
        hasValue = in.advanceExact(doc);
        return true;
      }
    };
  }

  /**
   * An empty DoubleValues instance that always returns {@code false} from {@link #advanceExact(int)}
   */
  public static final DoubleValues EMPTY = new DoubleValues() {
    @Override
    public double doubleValue() throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean advanceExact(int doc) throws IOException {
      return false;
    }
  };

}
