import org.apache.lucene.queries.function.docvalues.*;


import java.io.IOException;

import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.util.mutable.MutableValue;
import org.apache.lucene.util.mutable.MutableValueFloat;

/**
 * Abstract {@link FunctionValues} implementation which supports retrieving float values.
 * Implementations can control how the float values are loaded through {@link #floatVal(int)}}
 */
public abstract class FloatDocValues extends FunctionValues {
  protected final ValueSource vs;

  public FloatDocValues(ValueSource vs) {
    this.vs = vs;
  }

  @Override
  public byte byteVal(int doc) throws IOException {
    return (byte)floatVal(doc);
  }

  @Override
  public short shortVal(int doc) throws IOException {
    return (short)floatVal(doc);
  }

  @Override
  public abstract float floatVal(int doc) throws IOException;

  @Override
  public int intVal(int doc) throws IOException {
    return (int)floatVal(doc);
  }

  @Override
  public long longVal(int doc) throws IOException {
    return (long)floatVal(doc);
  }

  @Override
  public boolean boolVal(int doc) throws IOException {
    return floatVal(doc) != 0.0f;
  }

  @Override
  public double doubleVal(int doc) throws IOException {
    return (double)floatVal(doc);
  }

  @Override
  public String strVal(int doc) throws IOException {
    return Float.toString(floatVal(doc));
  }

  @Override
  public Object objectVal(int doc) throws IOException {
    return exists(doc) ? floatVal(doc) : null;
  }

  @Override
  public String toString(int doc) throws IOException {
    return vs.description() + '=' + strVal(doc);
  }

  @Override
  public ValueFiller getValueFiller() {
    return new ValueFiller() {
      private final MutableValueFloat mval = new MutableValueFloat();

      @Override
      public MutableValue getValue() {
        return mval;
      }

      @Override
      public void fillValue(int doc) throws IOException {
        mval.value = floatVal(doc);
        mval.exists = exists(doc);
      }
    };
  }
}
