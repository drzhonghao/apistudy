import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.*;


import java.io.IOException;

import org.apache.lucene.util.BytesRef;

/**
 * Delegates all methods to a wrapped {@link BinaryDocValues}.
 */
public abstract class FilterBinaryDocValues extends BinaryDocValues {

  /** Wrapped values */
  protected final BinaryDocValues in;
  
  /** Sole constructor */
  protected FilterBinaryDocValues(BinaryDocValues in) {
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
  public BytesRef binaryValue() throws IOException {
    return in.binaryValue();
  }
}
