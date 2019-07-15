import org.apache.lucene.spatial.prefix.*;


import java.io.IOException;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.BytesTermAttribute;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefIterator;

/**
 * A TokenStream used internally by {@link org.apache.lucene.spatial.prefix.PrefixTreeStrategy}.
 *
 * @lucene.internal
 */
class BytesRefIteratorTokenStream extends TokenStream {

  public BytesRefIterator getBytesRefIterator() {
    return bytesIter;
  }

  public BytesRefIteratorTokenStream setBytesRefIterator(BytesRefIterator iter) {
    this.bytesIter = iter;
    return this;
  }

  @Override
  public void reset() throws IOException {
    if (bytesIter == null)
      throw new IllegalStateException("call setBytesRefIterator() before usage");
  }

  @Override
  public final boolean incrementToken() throws IOException {
    if (bytesIter == null)
      throw new IllegalStateException("call setBytesRefIterator() before usage");

    // get next
    BytesRef bytes = bytesIter.next();
    if (bytes == null) {
      return false;
    } else {
      clearAttributes();
      bytesAtt.setBytesRef(bytes);
      //note: we don't bother setting posInc or type attributes.  There's no point to it.
      return true;
    }
  }

  //members
  private final BytesTermAttribute bytesAtt = addAttribute(BytesTermAttribute.class);

  private BytesRefIterator bytesIter = null; // null means not initialized

}
