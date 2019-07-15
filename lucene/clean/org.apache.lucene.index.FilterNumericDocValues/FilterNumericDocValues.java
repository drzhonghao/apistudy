import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.*;


import java.io.IOException;

/**
 * Delegates all methods to a wrapped {@link NumericDocValues}.
 */
public abstract class FilterNumericDocValues extends NumericDocValues {

  /** Wrapped values */
  protected final NumericDocValues in;
  
  /** Sole constructor */
  protected FilterNumericDocValues(NumericDocValues in) {
    this.in = in;
  }

  @Override
  public int docID() {
    return in.docID();
  }
  
  @Override
  public int nextDoc() throws IOException {
    return in.nextDoc();
  }

  @Override
  public int advance(int target) throws IOException {
    return in.advance(target);
  }
  
  @Override
  public boolean advanceExact(int target) throws IOException {
    return in.advanceExact(target);
  }
  
  @Override
  public long cost() {
    return in.cost();
  }

  @Override
  public long longValue() throws IOException {
    return in.longValue();
  }
}
