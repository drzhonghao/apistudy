import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.*;



import java.io.IOException;

/**
 * Abstract decorator class of a DocIdSetIterator
 * implementation that provides on-demand filter/validation
 * mechanism on an underlying DocIdSetIterator.
 */
public abstract class FilteredDocIdSetIterator extends DocIdSetIterator {
  protected DocIdSetIterator _innerIter;
  private int doc;

  /**
   * Constructor.
   * @param innerIter Underlying DocIdSetIterator.
   */
  public FilteredDocIdSetIterator(DocIdSetIterator innerIter) {
    if (innerIter == null) {
      throw new IllegalArgumentException("null iterator");
    }
    _innerIter = innerIter;
    doc = -1;
  }

  /** Return the wrapped {@link DocIdSetIterator}. */
  public DocIdSetIterator getDelegate() {
    return _innerIter;
  }

  /**
   * Validation method to determine whether a docid should be in the result set.
   * @param doc docid to be tested
   * @return true if input docid should be in the result set, false otherwise.
   * @see #FilteredDocIdSetIterator(DocIdSetIterator)
   */
  protected abstract boolean match(int doc);

  @Override
  public int docID() {
    return doc;
  }
  
  @Override
  public int nextDoc() throws IOException {
    while ((doc = _innerIter.nextDoc()) != NO_MORE_DOCS) {
      if (match(doc)) {
        return doc;
      }
    }
    return doc;
  }
  
  @Override
  public int advance(int target) throws IOException {
    doc = _innerIter.advance(target);
    if (doc != NO_MORE_DOCS) {
      if (match(doc)) {
        return doc;
      } else {
        while ((doc = _innerIter.nextDoc()) != NO_MORE_DOCS) {
          if (match(doc)) {
            return doc;
          }
        }
        return doc;
      }
    }
    return doc;
  }

  @Override
  public long cost() {
    return _innerIter.cost();
  }
}
