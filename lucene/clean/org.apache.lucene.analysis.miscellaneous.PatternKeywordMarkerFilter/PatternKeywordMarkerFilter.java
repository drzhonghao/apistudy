import org.apache.lucene.analysis.miscellaneous.KeywordMarkerFilter;
import org.apache.lucene.analysis.miscellaneous.*;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;

/**
 * Marks terms as keywords via the {@link KeywordAttribute}. Each token
 * that matches the provided pattern is marked as a keyword by setting
 * {@link KeywordAttribute#setKeyword(boolean)} to <code>true</code>.
 */
public final class PatternKeywordMarkerFilter extends KeywordMarkerFilter {
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final Matcher matcher;
  
  /**
   * Create a new {@link PatternKeywordMarkerFilter}, that marks the current
   * token as a keyword if the tokens term buffer matches the provided
   * {@link Pattern} via the {@link KeywordAttribute}.
   * 
   * @param in
   *          TokenStream to filter
   * @param pattern
   *          the pattern to apply to the incoming term buffer
   **/
  public PatternKeywordMarkerFilter(TokenStream in, Pattern pattern) {
    super(in);
    this.matcher = pattern.matcher("");
  }
  
  @Override
  protected boolean isKeyword() {
    matcher.reset(termAtt);
    return matcher.matches();
  }
  
}
