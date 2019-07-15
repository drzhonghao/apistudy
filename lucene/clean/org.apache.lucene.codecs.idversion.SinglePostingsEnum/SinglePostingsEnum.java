import org.apache.lucene.codecs.idversion.IDVersionPostingsFormat;
import org.apache.lucene.codecs.idversion.*;


import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.util.BytesRef;

class SinglePostingsEnum extends PostingsEnum {
  private int doc;
  private int pos;
  private int singleDocID;
  private long version;
  private final BytesRef payload;

  public SinglePostingsEnum() {
    payload = new BytesRef(8);
    payload.length = 8;
  }

  /** For reuse */
  public void reset(int singleDocID, long version) {
    doc = -1;
    this.singleDocID = singleDocID;
    this.version = version;
  }

  @Override
  public int nextDoc() {
    if (doc == -1) {
      doc = singleDocID;
    } else {
      doc = NO_MORE_DOCS;
    }
    pos = -1;
    
    return doc;
  }

  @Override
  public int docID() {
    return doc;
  }

  @Override
  public int advance(int target) {
    if (doc == -1 && target <= singleDocID) {
      doc = singleDocID;
      pos = -1;
    } else {
      doc = NO_MORE_DOCS;
    }
    return doc;
  }

  @Override
  public long cost() {
    return 1;
  }

  @Override
  public int freq() {
    return 1;
  }

  @Override
  public int nextPosition() {
    assert pos == -1;
    pos = 0;
    IDVersionPostingsFormat.longToBytes(version, payload);
    return pos;
  }

  @Override
  public BytesRef getPayload() {
    return payload;
  }

  @Override
  public int startOffset() {
    return -1;
  }

  @Override
  public int endOffset() {
    return -1;
  }
}
