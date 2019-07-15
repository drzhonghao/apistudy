import org.apache.lucene.queries.function.valuesource.*;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.docvalues.StrDocValues;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;

import java.util.Map;
import java.io.IOException;


/**
 * Pass a the field value through as a String, no matter the type // Q: doesn't this mean it's a "string"?
 *
 **/
public class LiteralValueSource extends ValueSource {
  protected final String string;
  protected final BytesRef bytesRef;

  public LiteralValueSource(String string) {
    this.string = string;
    this.bytesRef = new BytesRef(string);
  }

  /** returns the literal value */
  public String getValue() {
    return string;
  }

  @Override
  public FunctionValues getValues(Map context, LeafReaderContext readerContext) throws IOException {

    return new StrDocValues(this) {
      @Override
      public String strVal(int doc) {
        return string;
      }

      @Override
      public boolean bytesVal(int doc, BytesRefBuilder target) {
        target.copyBytes(bytesRef);
        return true;
      }

      @Override
      public String toString(int doc) {
        return string;
      }
    };
  }

  @Override
  public String description() {
    return "literal(" + string + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof LiteralValueSource)) return false;

    LiteralValueSource that = (LiteralValueSource) o;

    return string.equals(that.string);

  }

  public static final int hash = LiteralValueSource.class.hashCode();
  @Override
  public int hashCode() {
    return hash + string.hashCode();
  }
}
