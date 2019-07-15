import org.apache.lucene.index.LegacySortedSetDocValues;
import org.apache.lucene.index.*;


import java.io.IOException;

import org.apache.lucene.util.BytesRef;

/**
 * Wraps a {@link LegacySortedSetDocValues} into a {@link SortedSetDocValues}.
 *
 * @deprecated Implement {@link SortedSetDocValues} directly.
 */
@Deprecated
public final class LegacySortedSetDocValuesWrapper extends SortedSetDocValues {
  private final LegacySortedSetDocValues values;
  private final int maxDoc;
  private int docID = -1;
  private long ord;
  
  public LegacySortedSetDocValuesWrapper(LegacySortedSetDocValues values, int maxDoc) {
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
      values.setDocument(docID);
      ord = values.nextOrd();
      if (ord != NO_MORE_ORDS) {
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
    values.setDocument(docID);
    ord = values.nextOrd();
    return ord != NO_MORE_ORDS;
  }

  @Override
  public long cost() {
    return 0;
  }

  @Override
  public long nextOrd() {
    long result = ord;
    if (result != NO_MORE_ORDS) {
      ord = values.nextOrd();
    }
    return result;
  }

  @Override
  public BytesRef lookupOrd(long ord) {
    return values.lookupOrd((int) ord);
  }

  @Override
  public long getValueCount() {
    return values.getValueCount();
  }

  @Override
  public String toString() {
    return "LegacySortedSetDocValuesWrapper(" + values + ")";
  }
}
