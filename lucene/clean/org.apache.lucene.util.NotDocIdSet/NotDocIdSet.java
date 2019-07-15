import org.apache.lucene.util.*;



import java.io.IOException;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;

/**
 * This {@link DocIdSet} encodes the negation of another {@link DocIdSet}.
 * It is cacheable and supports random-access if the underlying set is
 * cacheable and supports random-access.
 * @lucene.internal
 */
public final class NotDocIdSet extends DocIdSet {

  private static final long BASE_RAM_BYTES_USED = RamUsageEstimator.shallowSizeOfInstance(NotDocIdSet.class);

  private final int maxDoc;
  private final DocIdSet in;

  /** Sole constructor. */
  public NotDocIdSet(int maxDoc, DocIdSet in) {
    this.maxDoc = maxDoc;
    this.in = in;
  }

  @Override
  public Bits bits() throws IOException {
    final Bits inBits = in.bits();
    if (inBits == null) {
      return null;
    }
    return new Bits() {

      @Override
      public boolean get(int index) {
        return !inBits.get(index);
      }

      @Override
      public int length() {
        return inBits.length();
      }

    };
  }

  @Override
  public long ramBytesUsed() {
    return BASE_RAM_BYTES_USED + in.ramBytesUsed();
  }

  @Override
  public DocIdSetIterator iterator() throws IOException {
    final DocIdSetIterator inIterator = in.iterator();
    return new DocIdSetIterator() {

      int doc = -1;
      int nextSkippedDoc = -1;

      @Override
      public int nextDoc() throws IOException {
        return advance(doc + 1);
      }

      @Override
      public int advance(int target) throws IOException {
        doc = target;
        if (doc > nextSkippedDoc) {
          nextSkippedDoc = inIterator.advance(doc);
        }
        while (true) {
          if (doc >= maxDoc) {
            return doc = NO_MORE_DOCS;
          }
          assert doc <= nextSkippedDoc;
          if (doc != nextSkippedDoc) {
            return doc;
          }
          doc += 1;
          nextSkippedDoc = inIterator.nextDoc();
        }
      }

      @Override
      public int docID() {
        return doc;
      }

      @Override
      public long cost() {
        // even if there are few docs in this set, iterating over all documents
        // costs O(maxDoc) in all cases
        return maxDoc;
      }
    };
  }

}
