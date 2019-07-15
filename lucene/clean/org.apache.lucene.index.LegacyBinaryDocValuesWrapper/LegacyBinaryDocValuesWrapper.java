import org.apache.lucene.index.LegacyBinaryDocValues;
import org.apache.lucene.index.*;


import java.io.IOException;

import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;

/**
 * Wraps a {@link LegacyBinaryDocValues} into a {@link BinaryDocValues}.
 *
 * @deprecated Implement {@link BinaryDocValues} directly.
 */
@Deprecated
public final class LegacyBinaryDocValuesWrapper extends BinaryDocValues {
  private final Bits docsWithField;
  private final LegacyBinaryDocValues values;
  private final int maxDoc;
  private int docID = -1;
  
  public LegacyBinaryDocValuesWrapper(Bits docsWithField, LegacyBinaryDocValues values) {
    this.docsWithField = docsWithField;
    this.values = values;
    this.maxDoc = docsWithField.length();
  }

  @Override
  public int docID() {
    return docID;
  }

  @Override
  public int nextDoc() {
    docID++;
    while (docID < maxDoc) {
      if (docsWithField.get(docID)) {
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
    if (target == NO_MORE_DOCS) {
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
    return docsWithField.get(target);
  }

  @Override
  public long cost() {
    return 0;
  }

  @Override
  public BytesRef binaryValue() {
    return values.get(docID);
  }
}
