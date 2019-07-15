import org.apache.lucene.search.highlight.*;


import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

/**
 * This TokenFilter limits the number of tokens while indexing by adding up the
 * current offset.
 */
public final class OffsetLimitTokenFilter extends TokenFilter {
  
  private int offsetCount;
  private OffsetAttribute offsetAttrib = getAttribute(OffsetAttribute.class);
  private int offsetLimit;
  
  public OffsetLimitTokenFilter(TokenStream input, int offsetLimit) {
    super(input);
    this.offsetLimit = offsetLimit;
  }
  
  @Override
  public boolean incrementToken() throws IOException {
    if (offsetCount < offsetLimit && input.incrementToken()) {
      int offsetLength = offsetAttrib.endOffset() - offsetAttrib.startOffset();
      offsetCount += offsetLength;
      return true;
    }
    return false;
  }
  
  @Override
  public void reset() throws IOException {
    super.reset();
    offsetCount = 0;
  }
  
}
