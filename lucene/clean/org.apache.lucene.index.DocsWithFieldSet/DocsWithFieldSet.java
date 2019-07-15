import org.apache.lucene.index.*;


import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BitSetIterator;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.RamUsageEstimator;

/** Accumulator for documents that have a value for a field. This is optimized
 *  for the case that all documents have a value. */
final class DocsWithFieldSet extends DocIdSet {

  private static long BASE_RAM_BYTES_USED = RamUsageEstimator.shallowSizeOfInstance(DocsWithFieldSet.class);

  private FixedBitSet set;
  private int cost = 0;
  private int lastDocId = -1;

  void add(int docID) {
    if (docID <= lastDocId) {
      throw new IllegalArgumentException("Out of order doc ids: last=" + lastDocId + ", next=" + docID);
    }
    if (set != null) {
      set = FixedBitSet.ensureCapacity(set, docID);
      set.set(docID);
    } else if (docID != cost) {
      // migrate to a sparse encoding using a bit set
      set = new FixedBitSet(docID + 1);
      set.set(0, cost);
      set.set(docID);
    }
    lastDocId = docID;
    cost++;
  }

  @Override
  public long ramBytesUsed() {
    return BASE_RAM_BYTES_USED + (set == null ? 0 : set.ramBytesUsed());
  }

  @Override
  public DocIdSetIterator iterator() {
    return set != null ? new BitSetIterator(set, cost) : DocIdSetIterator.all(cost);
  }

}
