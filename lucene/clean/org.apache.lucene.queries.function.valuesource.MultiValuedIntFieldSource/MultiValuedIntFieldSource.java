import org.apache.lucene.queries.function.valuesource.*;


import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSelector;
import org.apache.lucene.search.SortedNumericSelector.Type;
import org.apache.lucene.search.SortedNumericSortField;

/**
 * Obtains int field values from {@link org.apache.lucene.index.LeafReader#getSortedNumericDocValues} and using a 
 * {@link org.apache.lucene.search.SortedNumericSelector} it gives a single-valued ValueSource view of a field.
 */
public class MultiValuedIntFieldSource extends IntFieldSource {

  protected final SortedNumericSelector.Type selector;

  public MultiValuedIntFieldSource(String field, Type selector) {
    super(field);
    this.selector = selector;
    Objects.requireNonNull(field, "Field is required to create a MultiValuedIntFieldSource");
    Objects.requireNonNull(selector, "SortedNumericSelector is required to create a MultiValuedIntFieldSource");
  }
  
  @Override
  public SortField getSortField(boolean reverse) {
    return new SortedNumericSortField(field, SortField.Type.INT, reverse, selector);
  }
  
  @Override
  public String description() {
    return "int(" + field + ',' + selector.name() + ')';
  }
  
  @Override
  protected NumericDocValues getNumericDocValues(Map context, LeafReaderContext readerContext) throws IOException {
    SortedNumericDocValues sortedDv = DocValues.getSortedNumeric(readerContext.reader(), field);
    return SortedNumericSelector.wrap(sortedDv, selector, SortField.Type.INT);
  }

  @Override
  public boolean equals(Object o) {
    if (o.getClass() !=  MultiValuedIntFieldSource.class) return false;
    MultiValuedIntFieldSource other = (MultiValuedIntFieldSource)o;
    if (this.selector != other.selector) return false;
    return this.field.equals(other.field);
  }

  @Override
  public int hashCode() {
    int h = super.hashCode();
    h += selector.hashCode();
    return h;
  }

}