import org.apache.lucene.index.LegacyNumericDocValues;
import org.apache.lucene.index.*;


import java.io.IOException;

import org.apache.lucene.util.Bits;

/**
 * Wraps a {@link LegacyNumericDocValues} into a {@link NumericDocValues}.
 *
 * @deprecated Implement {@link NumericDocValues} directly.
 */
@Deprecated
public final class LegacyNumericDocValuesWrapper extends NumericDocValues {
  private final Bits docsWithField;
  private final LegacyNumericDocValues values;
  private final int maxDoc;
  private int docID = -1;
  private long value;
  
  public LegacyNumericDocValuesWrapper(Bits docsWithField, LegacyNumericDocValues values) {
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
      value = values.get(docID);
      if (value != 0 || docsWithField.get(docID)) {
        return docID;
      }
      docID++;
    }
    docID = NO_MORE_DOCS;
    return NO_MORE_DOCS;
  }

  @Override
  public int advance(int target) {
    assert target >= docID: "target=" + target + " docID=" + docID;
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
    value = values.get(docID);
    return value != 0 || docsWithField.get(docID);
  }

  @Override
  public long cost() {
    // TODO
    return 0;
  }

  @Override
  public long longValue() {
    return value;
  }

  @Override
  public String toString() {
    return "LegacyNumericDocValuesWrapper(" + values + ")";
  }
}
