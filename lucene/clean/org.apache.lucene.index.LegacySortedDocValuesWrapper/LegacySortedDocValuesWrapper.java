import org.apache.lucene.index.LegacySortedDocValues;
import org.apache.lucene.index.*;


import java.io.IOException;

import org.apache.lucene.util.BytesRef;

/**
 * Wraps a {@link LegacySortedDocValues} into a {@link SortedDocValues}.
 *
 * @deprecated Implement {@link SortedDocValues} directly.
 */
@Deprecated
public final class LegacySortedDocValuesWrapper extends SortedDocValues {
  private final LegacySortedDocValues values;
  private final int maxDoc;
  private int docID = -1;
  private int ord;
  
  public LegacySortedDocValuesWrapper(LegacySortedDocValues values, int maxDoc) {
    this.values = values;
    this.maxDoc = maxDoc;
  }

  @Override
  public int docID() {
    return docID;
  }

  @Override
  public int nextDoc() {
    assert docID != NO_MORE_DOCS;
    docID++;
    while (docID < maxDoc) {
      ord = values.getOrd(docID);
      if (ord != -1) {
        return docID;
      }
      docID++;
    }
    docID = NO_MORE_DOCS;
    return NO_MORE_DOCS;
  }

  @Override
  public int advance(int target) {
    if (target < docID) {
      throw new IllegalArgumentException("cannot advance backwards: docID=" + docID + " target=" + target);
    }
    if (target >= maxDoc) {
      this.docID = NO_MORE_DOCS;
    } else {
      this.docID = target-1;
      nextDoc();
    }
    return docID;
  }

  @Override
  public boolean advanceExact(int target) throws IOException {
    docID = target;
    ord = values.getOrd(docID);
    return ord != -1;
  }

  @Override
  public long cost() {
    return 0;
  }

  @Override
  public int ordValue() {
    return ord;
  }

  @Override
  public BytesRef lookupOrd(int ord) {
    return values.lookupOrd(ord);
  }

  @Override
  public int getValueCount() {
    return values.getValueCount();
  }
}
