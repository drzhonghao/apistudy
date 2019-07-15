import org.apache.lucene.analysis.miscellaneous.*;



import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;

/**
 * Marks terms as keywords via the {@link KeywordAttribute}.
 * 
 * @see KeywordAttribute
 */
public abstract class KeywordMarkerFilter extends TokenFilter {

  private final KeywordAttribute keywordAttr = addAttribute(KeywordAttribute.class);

  /**
   * Creates a new {@link KeywordMarkerFilter}
   * @param in the input stream
   */
  protected KeywordMarkerFilter(TokenStream in) {
    super(in);
  }

  @Override
  public final boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      if (isKeyword()) { 
        keywordAttr.setKeyword(true);
      }
      return true;
    } else {
      return false;
    }
  }
  
  protected abstract boolean isKeyword();
  
}
