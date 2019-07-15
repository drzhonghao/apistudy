import org.apache.lucene.search.join.*;


import java.io.IOException;

import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.SimpleCollector;

abstract class DocValuesTermsCollector<DV> extends SimpleCollector {
  
  @FunctionalInterface
  static interface Function<R> {
    R apply(LeafReader t) throws IOException;
  }
  
  protected DV docValues;
  private final Function<DV> docValuesCall;
  
  public DocValuesTermsCollector(Function<DV> docValuesCall) {
    this.docValuesCall = docValuesCall;
  }

  @Override
  protected final void doSetNextReader(LeafReaderContext context) throws IOException {
    docValues = docValuesCall.apply(context.reader());
  }
  
  static Function<BinaryDocValues> binaryDocValues(String field) {
    return (ctx) -> DocValues.getBinary(ctx, field);
  }

  static Function<SortedSetDocValues> sortedSetDocValues(String field) {
    return (ctx) -> DocValues.getSortedSet(ctx, field);
  }
}
