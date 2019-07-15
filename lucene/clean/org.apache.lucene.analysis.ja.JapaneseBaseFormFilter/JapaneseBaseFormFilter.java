import org.apache.lucene.analysis.ja.*;



import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ja.tokenattributes.BaseFormAttribute;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;

/**
 * Replaces term text with the {@link BaseFormAttribute}.
 * <p>
 * This acts as a lemmatizer for verbs and adjectives.
 * <p>
 * To prevent terms from being stemmed use an instance of
 * {@link SetKeywordMarkerFilter} or a custom {@link TokenFilter} that sets
 * the {@link KeywordAttribute} before this {@link TokenStream}.
 * </p>
 */
public final class JapaneseBaseFormFilter extends TokenFilter {
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final BaseFormAttribute basicFormAtt = addAttribute(BaseFormAttribute.class);
  private final KeywordAttribute keywordAtt = addAttribute(KeywordAttribute.class);

  public JapaneseBaseFormFilter(TokenStream input) {
    super(input);
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      if (!keywordAtt.isKeyword()) {
        String baseForm = basicFormAtt.getBaseForm();
        if (baseForm != null) {
          termAtt.setEmpty().append(baseForm);
        }
      }
      return true;
    } else {
      return false;
    }
  }
}
