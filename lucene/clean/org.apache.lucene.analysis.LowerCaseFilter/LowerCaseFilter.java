import org.apache.lucene.analysis.*;



import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.CharacterUtils;

/**
 * Normalizes token text to lower case.
 */
public class LowerCaseFilter extends TokenFilter {
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  
  /**
   * Create a new LowerCaseFilter, that normalizes token text to lower case.
   * 
   * @param in TokenStream to filter
   */
  public LowerCaseFilter(TokenStream in) {
    super(in);
  }
  
  @Override
  public final boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      CharacterUtils.toLowerCase(termAtt.buffer(), 0, termAtt.length());
      return true;
    } else
      return false;
  }
}
