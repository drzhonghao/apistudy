import org.apache.lucene.queries.function.docvalues.*;


import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.ValueSourceScorer;
import org.apache.lucene.util.mutable.MutableValue;
import org.apache.lucene.util.mutable.MutableValueInt;

/**
 * Abstract {@link FunctionValues} implementation which supports retrieving int values.
 * Implementations can control how the int values are loaded through {@link #intVal(int)}
 */
public abstract class IntDocValues extends FunctionValues {
  protected final ValueSource vs;

  public IntDocValues(ValueSource vs) {
    this.vs = vs;
  }

  @Override
  public byte byteVal(int doc) throws IOException {
    return (byte)intVal(doc);
  }

  @Override
  public short shortVal(int doc) throws IOException {
    return (short)intVal(doc);
  }

  @Override
  public float floatVal(int doc) throws IOException {
    return (float)intVal(doc);
  }

  @Override
  public abstract int intVal(int doc) throws IOException;

  @Override
  public long longVal(int doc) throws IOException {
    return (long)intVal(doc);
  }

  @Override
  public double doubleVal(int doc) throws IOException {
    return (double)intVal(doc);
  }

  @Override
  public String strVal(int doc) throws IOException {
    return Integer.toString(intVal(doc));
  }

  @Override
  public Object objectVal(int doc) throws IOException {
    return exists(doc) ? intVal(doc) : null;
  }

  @Override
  public String toString(int doc) throws IOException {
    return vs.description() + '=' + strVal(doc);
  }
  
  @Override
  public ValueSourceScorer getRangeScorer(LeafReaderContext readerContext, String lowerVal, String upperVal, boolean includeLower, boolean includeUpper) {
    int lower,upper;

    // instead of using separate comparison functions, adjust the endpoints.

    if (lowerVal==null) {
      lower = Integer.MIN_VALUE;
    } else {
      lower = Integer.parseInt(lowerVal);
      if (!includeLower && lower < Integer.MAX_VALUE) lower++;
    }

     if (upperVal==null) {
      upper = Integer.MAX_VALUE;
    } else {
      upper = Integer.parseInt(upperVal);
      if (!includeUpper && upper > Integer.MIN_VALUE) upper--;
    }

    final int ll = lower;
    final int uu = upper;

    return new ValueSourceScorer(readerContext, this) {
      @Override
      public boolean matches(int doc) throws IOException {
        if (!exists(doc)) return false;
        int val = intVal(doc);
        return val >= ll && val <= uu;
      }
    };
  }

  @Override
  public ValueFiller getValueFiller() {
    return new ValueFiller() {
      private final MutableValueInt mval = new MutableValueInt();

      @Override
      public MutableValue getValue() {
        return mval;
      }

      @Override
      public void fillValue(int doc) throws IOException {
        mval.value = intVal(doc);
        mval.exists = exists(doc);
      }
    };
  }
}
