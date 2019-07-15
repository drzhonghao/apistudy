import org.apache.lucene.analysis.pt.PortugueseMinimalStemmer;
import org.apache.lucene.analysis.pt.*;



import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;

/**
 * A {@link TokenFilter} that applies {@link PortugueseMinimalStemmer} to stem 
 * Portuguese words.
 * <p>
 * To prevent terms from being stemmed use an instance of
 * {@link SetKeywordMarkerFilter} or a custom {@link TokenFilter} that sets
 * the {@link KeywordAttribute} before this {@link TokenStream}.
 * </p>
 */
public final class PortugueseMinimalStemFilter extends TokenFilter {
  private final PortugueseMinimalStemmer stemmer = new PortugueseMinimalStemmer();
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final KeywordAttribute keywordAttr = addAttribute(KeywordAttribute.class);

  public PortugueseMinimalStemFilter(TokenStream input) {
    super(input);
  }
  
  @Override
  public boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      if (!keywordAttr.isKeyword()) {
        final int newlen = stemmer.stem(termAtt.buffer(), termAtt.length());
        termAtt.setLength(newlen);
      }
      return true;
    } else {
      return false;
    }
  }
}
