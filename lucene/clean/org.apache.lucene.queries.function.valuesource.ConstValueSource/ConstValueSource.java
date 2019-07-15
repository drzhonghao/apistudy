import org.apache.lucene.queries.function.valuesource.ConstNumberSource;
import org.apache.lucene.queries.function.valuesource.*;


import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.docvalues.FloatDocValues;

import java.io.IOException;
import java.util.Map;

/**
 * <code>ConstValueSource</code> returns a constant for all documents
 */
public class ConstValueSource extends ConstNumberSource {
  final float constant;
  private final double dv;

  public ConstValueSource(float constant) {
    this.constant = constant;
    this.dv = constant;
  }

  @Override
  public String description() {
    return "const(" + constant + ")";
  }

  @Override
  public FunctionValues getValues(Map context, LeafReaderContext readerContext) throws IOException {
    return new FloatDocValues(this) {
      @Override
      public float floatVal(int doc) {
        return constant;
      }
      @Override
      public int intVal(int doc) {
        return (int)constant;
      }
      @Override
      public long longVal(int doc) {
        return (long)constant;
      }
      @Override
      public double doubleVal(int doc) {
        return dv;
      }
      @Override
      public String toString(int doc) {
        return description();
      }
      @Override
      public Object objectVal(int doc) {
        return constant;
      }
      @Override
      public boolean boolVal(int doc) {
        return constant != 0.0f;
      }
    };
  }

  @Override
  public int hashCode() {
    return Float.floatToIntBits(constant) * 31;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ConstValueSource)) return false;
    ConstValueSource other = (ConstValueSource)o;
    return  this.constant == other.constant;
  }

  @Override
  public int getInt() {
    return (int)constant;
  }

  @Override
  public long getLong() {
    return (long)constant;
  }

  @Override
  public float getFloat() {
    return constant;
  }

  @Override
  public double getDouble() {
    return dv;
  }

  @Override
  public Number getNumber() {
    return constant;
  }

  @Override
  public boolean getBool() {
    return constant != 0.0f;
  }
}
