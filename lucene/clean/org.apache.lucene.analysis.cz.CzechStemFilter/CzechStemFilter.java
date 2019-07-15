import org.apache.lucene.analysis.cz.CzechStemmer;
import org.apache.lucene.analysis.cz.*;


import java.io.IOException;

import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter; // for javadoc
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;


/**
 * A {@link TokenFilter} that applies {@link CzechStemmer} to stem Czech words.
 * <p>
 * To prevent terms from being stemmed use an instance of
 * {@link SetKeywordMarkerFilter} or a custom {@link TokenFilter} that sets
 * the {@link KeywordAttribute} before this {@link TokenStream}.
 * </p>
 * <p><b>NOTE</b>: Input is expected to be in lowercase, 
 * but with diacritical marks</p>
 * @see SetKeywordMarkerFilter
 */
public final class CzechStemFilter extends TokenFilter {
  private final CzechStemmer stemmer = new CzechStemmer();
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final KeywordAttribute keywordAttr = addAttribute(KeywordAttribute.class);
  
  public CzechStemFilter(TokenStream input) {
    super(input);
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      if(!keywordAttr.isKeyword()) {
        final int newlen = stemmer.stem(termAtt.buffer(), termAtt.length());
        termAtt.setLength(newlen);
      }
      return true;
    } else {
      return false;
    }
  }
}
