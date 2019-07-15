import org.apache.lucene.codecs.blocktree.*;



import java.io.IOException;

import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BitSet;
import org.apache.lucene.util.BitSetIterator;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FixedBitSet; // javadocs

/** Takes a {@link FixedBitSet} and creates a DOCS {@link PostingsEnum} from it. */

class BitSetPostingsEnum extends PostingsEnum {
  private final BitSet bits;
  private DocIdSetIterator in;
  
  BitSetPostingsEnum(BitSet bits) {
    this.bits = bits;
    reset();
  }

  @Override
  public int freq() throws IOException {
    return 1;
  }

  @Override
  public int docID() {
    if (in == null) {
      return -1;
    } else {
      return in.docID();
    }
  }

  @Override
  public int nextDoc() throws IOException {
    if (in == null) {
      in = new BitSetIterator(bits, 0);
    }
    return in.nextDoc();
  }

  @Override
  public int advance(int target) throws IOException {
    return in.advance(target);
  }

  @Override
  public long cost() {
    return in.cost();
  }
  
  void reset() {
    in = null;
  }

  @Override
  public BytesRef getPayload() {
    return null;
  }

  @Override
  public int nextPosition() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int startOffset() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int endOffset() {
    throw new UnsupportedOperationException();
  }
}
