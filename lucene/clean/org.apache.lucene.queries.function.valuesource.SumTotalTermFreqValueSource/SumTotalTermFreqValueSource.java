import org.apache.lucene.queries.function.valuesource.*;


import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.docvalues.LongDocValues;
import org.apache.lucene.search.IndexSearcher;

import java.io.IOException;
import java.util.Map;

/**
 * <code>SumTotalTermFreqValueSource</code> returns the number of tokens.
 * (sum of term freqs across all documents, across all terms).
 * Returns -1 if frequencies were omitted for the field, or if 
 * the codec doesn't support this statistic.
 * @lucene.internal
 */
public class SumTotalTermFreqValueSource extends ValueSource {
  protected final String indexedField;

  public SumTotalTermFreqValueSource(String indexedField) {
    this.indexedField = indexedField;
  }

  public String name() {
    return "sumtotaltermfreq";
  }

  @Override
  public String description() {
    return name() + '(' + indexedField + ')';
  }

  @Override
  public FunctionValues getValues(Map context, LeafReaderContext readerContext) throws IOException {
    return (FunctionValues)context.get(this);
  }

  @Override
  public void createWeight(Map context, IndexSearcher searcher) throws IOException {
    long sumTotalTermFreq = 0;
    for (LeafReaderContext readerContext : searcher.getTopReaderContext().leaves()) {
      Terms terms = readerContext.reader().terms(indexedField);
      if (terms == null) continue;
      long v = terms.getSumTotalTermFreq();
      if (v == -1) {
        sumTotalTermFreq = -1;
        break;
      } else {
        sumTotalTermFreq += v;
      }
    }
    final long ttf = sumTotalTermFreq;
    context.put(this, new LongDocValues(this) {
      @Override
      public long longVal(int doc) {
        return ttf;
      }
    });
  }

  @Override
  public int hashCode() {
    return getClass().hashCode() + indexedField.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this.getClass() != o.getClass()) return false;
    SumTotalTermFreqValueSource other = (SumTotalTermFreqValueSource)o;
    return this.indexedField.equals(other.indexedField);
  }
}
