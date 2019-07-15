import org.apache.lucene.analysis.miscellaneous.*;



import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import java.io.IOException;


/**
 * This TokenFilter emits each incoming token twice once as keyword and once non-keyword, in other words once with
 * {@link KeywordAttribute#setKeyword(boolean)} set to <code>true</code> and once set to <code>false</code>.
 * This is useful if used with a stem filter that respects the {@link KeywordAttribute} to index the stemmed and the
 * un-stemmed version of a term into the same field.
 */
public final class KeywordRepeatFilter extends TokenFilter {

  private final KeywordAttribute keywordAttribute = addAttribute(KeywordAttribute.class);
  private final PositionIncrementAttribute posIncAttr = addAttribute(PositionIncrementAttribute.class);
  private State state;

  /**
   * Construct a token stream filtering the given input.
   */
  public KeywordRepeatFilter(TokenStream input) {
    super(input);
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (state != null) {
      restoreState(state);
      posIncAttr.setPositionIncrement(0);
      keywordAttribute.setKeyword(false);
      state = null;
      return true;
    }
    if (input.incrementToken()) {
      state = captureState();
      keywordAttribute.setKeyword(true);
      return true;
    }
    return false;
  }

  @Override
  public void reset() throws IOException {
    super.reset();
    state = null;
  }
}
