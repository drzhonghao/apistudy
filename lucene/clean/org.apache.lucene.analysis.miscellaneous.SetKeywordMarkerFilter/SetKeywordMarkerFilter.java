import org.apache.lucene.analysis.miscellaneous.*;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.CharArraySet;

/**
 * Marks terms as keywords via the {@link KeywordAttribute}. Each token
 * contained in the provided set is marked as a keyword by setting
 * {@link KeywordAttribute#setKeyword(boolean)} to <code>true</code>.
 */
public final class SetKeywordMarkerFilter extends KeywordMarkerFilter {
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final CharArraySet keywordSet;

  /**
   * Create a new KeywordSetMarkerFilter, that marks the current token as a
   * keyword if the tokens term buffer is contained in the given set via the
   * {@link KeywordAttribute}.
   * 
   * @param in
   *          TokenStream to filter
   * @param keywordSet
   *          the keywords set to lookup the current termbuffer
   */
  public SetKeywordMarkerFilter(final TokenStream in, final CharArraySet keywordSet) {
    super(in);
    this.keywordSet = keywordSet;
  }

  @Override
  protected boolean isKeyword() {
    return keywordSet.contains(termAtt.buffer(), 0, termAtt.length());
  }
  
}
