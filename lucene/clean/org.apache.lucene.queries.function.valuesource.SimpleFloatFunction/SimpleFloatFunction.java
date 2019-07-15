import org.apache.lucene.queries.function.valuesource.*;


import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.docvalues.FloatDocValues;

import java.io.IOException;
import java.util.Map;

/** A simple float function with a single argument
 */
 public abstract class SimpleFloatFunction extends SingleFunction {
  public SimpleFloatFunction(ValueSource source) {
    super(source);
  }

  protected abstract float func(int doc, FunctionValues vals) throws IOException;

  @Override
  public FunctionValues getValues(Map context, LeafReaderContext readerContext) throws IOException {
    final FunctionValues vals =  source.getValues(context, readerContext);
    return new FloatDocValues(this) {
      @Override
      public float floatVal(int doc) throws IOException {
        return func(doc, vals);
      }
      @Override
      public String toString(int doc) throws IOException {
        return name() + '(' + vals.toString(doc) + ')';
      }
    };
  }
}
