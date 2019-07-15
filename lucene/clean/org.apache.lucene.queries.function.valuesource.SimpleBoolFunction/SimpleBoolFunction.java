import org.apache.lucene.queries.function.valuesource.*;


import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.docvalues.BoolDocValues;
import org.apache.lucene.search.IndexSearcher;

import java.io.IOException;
import java.util.Map;

/**
 * {@link BoolFunction} implementation which applies an extendible boolean
 * function to the values of a single wrapped {@link ValueSource}.
 *
 * Functions this can be used for include whether a field has a value or not,
 * or inverting the boolean value of the wrapped ValueSource.
 */
public abstract class SimpleBoolFunction extends BoolFunction {
  protected final ValueSource source;

  public SimpleBoolFunction(ValueSource source) {
    this.source = source;
  }

  protected abstract String name();

  protected abstract boolean func(int doc, FunctionValues vals) throws IOException;

  @Override
  public BoolDocValues getValues(Map context, LeafReaderContext readerContext) throws IOException {
    final FunctionValues vals =  source.getValues(context, readerContext);
    return new BoolDocValues(this) {
      @Override
      public boolean boolVal(int doc) throws IOException {
        return func(doc, vals);
      }
      @Override
      public String toString(int doc) throws IOException {
        return name() + '(' + vals.toString(doc) + ')';
      }
    };
  }

  @Override
  public String description() {
    return name() + '(' + source.description() + ')';
  }

  @Override
  public int hashCode() {
    return source.hashCode() + name().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this.getClass() != o.getClass()) return false;
    SimpleBoolFunction other = (SimpleBoolFunction)o;
    return this.source.equals(other.source);
  }

  @Override
  public void createWeight(Map context, IndexSearcher searcher) throws IOException {
    source.createWeight(context, searcher);
  }
}
