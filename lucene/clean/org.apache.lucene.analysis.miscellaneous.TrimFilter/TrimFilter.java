import org.apache.lucene.analysis.miscellaneous.*;


import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;

/**
 * Trims leading and trailing whitespace from Tokens in the stream.
 */
public final class TrimFilter extends TokenFilter {

  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

  /**
   * Create a new {@link TrimFilter}.
   * @param in            the stream to consume
   */
  public TrimFilter(TokenStream in) {
    super(in);
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (!input.incrementToken()) return false;

    char[] termBuffer = termAtt.buffer();
    int len = termAtt.length();
    //TODO: Is this the right behavior or should we return false?  Currently, "  ", returns true, so I think this should
    //also return true
    if (len == 0){
      return true;
    }
    int start = 0;
    int end = 0;

    // eat the first characters
    for (start = 0; start < len && Character.isWhitespace(termBuffer[start]); start++) {
    }
    // eat the end characters
    for (end = len; end >= start && Character.isWhitespace(termBuffer[end - 1]); end--) {
    }
    if (start > 0 || end < len) {
      if (start < end) {
        termAtt.copyBuffer(termBuffer, start, (end - start));
      } else {
        termAtt.setEmpty();
      }
    }

    return true;
  }
}
