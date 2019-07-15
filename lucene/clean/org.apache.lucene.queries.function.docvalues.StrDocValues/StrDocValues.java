import org.apache.lucene.queries.function.docvalues.*;


import java.io.IOException;

import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.util.mutable.MutableValue;
import org.apache.lucene.util.mutable.MutableValueStr;

/**
 * Abstract {@link FunctionValues} implementation which supports retrieving String values.
 * Implementations can control how the String values are loaded through {@link #strVal(int)}}
 */
public abstract class StrDocValues extends FunctionValues {
  protected final ValueSource vs;

  public StrDocValues(ValueSource vs) {
    this.vs = vs;
  }

  @Override
  public abstract String strVal(int doc) throws IOException;

  @Override
  public Object objectVal(int doc) throws IOException {
    return exists(doc) ? strVal(doc) : null;
  }

  @Override
  public boolean boolVal(int doc) throws IOException {
    return exists(doc);
  }

  @Override
  public String toString(int doc) throws IOException {
    return vs.description() + "='" + strVal(doc) + "'";
  }

  @Override
  public ValueFiller getValueFiller() {
    return new ValueFiller() {
      private final MutableValueStr mval = new MutableValueStr();

      @Override
      public MutableValue getValue() {
        return mval;
      }

      @Override
      public void fillValue(int doc) throws IOException {
        mval.exists = bytesVal(doc, mval.value);
      }
    };
  }
}
