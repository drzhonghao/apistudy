import org.apache.lucene.queries.function.valuesource.*;


import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.search.IndexSearcher;

import java.io.IOException;
import java.util.Map;

/** A function with a single argument
 */
 public abstract class SingleFunction extends ValueSource {
  protected final ValueSource source;

  public SingleFunction(ValueSource source) {
    this.source = source;
  }

  protected abstract String name();

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
    SingleFunction other = (SingleFunction)o;
    return this.name().equals(other.name())
         && this.source.equals(other.source);
  }

  @Override
  public void createWeight(Map context, IndexSearcher searcher) throws IOException {
    source.createWeight(context, searcher);
  }
}
