import org.apache.lucene.queries.function.valuesource.*;


import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.docvalues.DoubleDocValues;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.mutable.MutableValue;
import org.apache.lucene.util.mutable.MutableValueDouble;

/**
 * Obtains double field values from {@link org.apache.lucene.index.LeafReader#getNumericDocValues} and makes
 * those values available as other numeric types, casting as needed.
 */
public class DoubleFieldSource extends FieldCacheSource {

  public DoubleFieldSource(String field) {
    super(field);
  }

  @Override
  public String description() {
    return "double(" + field + ')';
  }

  @Override
  public SortField getSortField(boolean reverse) {
    return new SortField(field, Type.DOUBLE, reverse);
  }
  
  @Override
  public FunctionValues getValues(Map context, LeafReaderContext readerContext) throws IOException {

    final NumericDocValues values = getNumericDocValues(context, readerContext);

    return new DoubleDocValues(this) {
      int lastDocID;

      private double getValueForDoc(int doc) throws IOException {
        if (doc < lastDocID) {
          throw new IllegalArgumentException("docs were sent out-of-order: lastDocID=" + lastDocID + " vs docID=" + doc);
        }
        lastDocID = doc;
        int curDocID = values.docID();
        if (doc > curDocID) {
          curDocID = values.advance(doc);
        }
        if (doc == curDocID) {
          return Double.longBitsToDouble(values.longValue());
        } else {
          return 0.0;
        }
      }
      
      @Override
      public double doubleVal(int doc) throws IOException {
        return getValueForDoc(doc);
      }

      @Override
      public boolean exists(int doc) throws IOException {
        getValueForDoc(doc);
        return doc == values.docID();
      }

      @Override
      public ValueFiller getValueFiller() {
        return new ValueFiller() {
          private final MutableValueDouble mval = new MutableValueDouble();

          @Override
          public MutableValue getValue() {
            return mval;
          }

          @Override
          public void fillValue(int doc) throws IOException {
            mval.value = getValueForDoc(doc);
            mval.exists = exists(doc);
          }
        };
      }
    };
  }

  protected NumericDocValues getNumericDocValues(Map context, LeafReaderContext readerContext) throws IOException {
    return DocValues.getNumeric(readerContext.reader(), field);
  }

  @Override
  public boolean equals(Object o) {
    if (o.getClass() != DoubleFieldSource.class) return false;
    DoubleFieldSource other = (DoubleFieldSource) o;
    return super.equals(other);
  }

  @Override
  public int hashCode() {
    int h = Double.class.hashCode();
    h += super.hashCode();
    return h;
  }
}
