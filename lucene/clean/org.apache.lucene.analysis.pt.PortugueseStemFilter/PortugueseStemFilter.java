import org.apache.lucene.analysis.pt.PortugueseStemmer;
import org.apache.lucene.analysis.pt.*;



import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;

/**
 * A {@link TokenFilter} that applies {@link PortugueseStemmer} to stem 
 * Portuguese words.
 * <p>
 * To prevent terms from being stemmed use an instance of
 * {@link SetKeywordMarkerFilter} or a custom {@link TokenFilter} that sets
 * the {@link KeywordAttribute} before this {@link TokenStream}.
 * </p>
 */
public final class PortugueseStemFilter extends TokenFilter {
  private final PortugueseStemmer stemmer = new PortugueseStemmer();
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final KeywordAttribute keywordAttr = addAttribute(KeywordAttribute.class);

  public PortugueseStemFilter(TokenStream input) {
    super(input);
  }
  
  @Override
  public boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      if (!keywordAttr.isKeyword()) {
        // this stemmer increases word length by 1: worst case '*ã' -> '*ão'
        final int len = termAtt.length();
        final int newlen = stemmer.stem(termAtt.resizeBuffer(len+1), len);
        termAtt.setLength(newlen);
      }
      return true;
    } else {
      return false;
    }
  }
}
