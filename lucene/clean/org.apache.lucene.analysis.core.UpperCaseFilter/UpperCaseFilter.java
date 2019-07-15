import org.apache.lucene.analysis.core.*;



import java.io.IOException;

import org.apache.lucene.analysis.CharacterUtils;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Normalizes token text to UPPER CASE.
 * 
 * <p><b>NOTE:</b> In Unicode, this transformation may lose information when the
 * upper case character represents more than one lower case character. Use this filter
 * when you require uppercase tokens.  Use the {@link LowerCaseFilter} for 
 * general search matching
 */
public final class UpperCaseFilter extends TokenFilter {
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  
  /**
   * Create a new UpperCaseFilter, that normalizes token text to upper case.
   * 
   * @param in TokenStream to filter
   */
  public UpperCaseFilter(TokenStream in) {
    super(in);
  }
  
  @Override
  public final boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      CharacterUtils.toUpperCase(termAtt.buffer(), 0, termAtt.length());
      return true;
    } else
      return false;
  }
}
