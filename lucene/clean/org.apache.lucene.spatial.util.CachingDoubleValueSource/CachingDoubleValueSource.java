import org.apache.lucene.spatial.util.*;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;

/**
 * Caches the doubleVal of another value source in a HashMap
 * so that it is computed only once.
 * @lucene.internal
 */
public class CachingDoubleValueSource extends DoubleValuesSource {

  final DoubleValuesSource source;
  final Map<Integer, Double> cache;

  public CachingDoubleValueSource(DoubleValuesSource source) {
    this.source = source;
    cache = new HashMap<>();
  }

  @Override
  public String toString() {
    return "Cached["+source.toString()+"]";
  }

  @Override
  public DoubleValues getValues(LeafReaderContext readerContext, DoubleValues scores) throws IOException {
    final int base = readerContext.docBase;
    final DoubleValues vals = source.getValues(readerContext, scores);
    return new DoubleValues() {

      @Override
      public double doubleValue() throws IOException {
        int key = base + doc;
        Double v = cache.get(key);
        if (v == null) {
          v = vals.doubleValue();
          cache.put(key, v);
        }
        return v;
      }

      @Override
      public boolean advanceExact(int doc) throws IOException {
        this.doc = doc;
        return vals.advanceExact(doc);
      }

      int doc = -1;

    };
  }

  @Override
  public boolean needsScores() {
    return false;
  }

  @Override
  public boolean isCacheable(LeafReaderContext ctx) {
    return source.isCacheable(ctx);
  }

  @Override
  public Explanation explain(LeafReaderContext ctx, int docId, Explanation scoreExplanation) throws IOException {
    return source.explain(ctx, docId, scoreExplanation);
  }

  @Override
  public DoubleValuesSource rewrite(IndexSearcher searcher) throws IOException {
    return new CachingDoubleValueSource(source.rewrite(searcher));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CachingDoubleValueSource that = (CachingDoubleValueSource) o;

    if (source != null ? !source.equals(that.source) : that.source != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return source != null ? source.hashCode() : 0;
  }
}
