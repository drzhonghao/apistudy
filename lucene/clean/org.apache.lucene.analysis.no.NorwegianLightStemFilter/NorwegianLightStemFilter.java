import org.apache.lucene.analysis.no.NorwegianLightStemmer;
import org.apache.lucene.analysis.no.*;



import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;

/**
 * A {@link TokenFilter} that applies {@link NorwegianLightStemmer} to stem Norwegian
 * words.
 * <p>
 * To prevent terms from being stemmed use an instance of
 * {@link SetKeywordMarkerFilter} or a custom {@link TokenFilter} that sets
 * the {@link KeywordAttribute} before this {@link TokenStream}.
 * </p>
 */
public final class NorwegianLightStemFilter extends TokenFilter {
  private final NorwegianLightStemmer stemmer;
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final KeywordAttribute keywordAttr = addAttribute(KeywordAttribute.class);
  
  /** 
   * Calls {@link #NorwegianLightStemFilter(TokenStream, int) 
   * NorwegianLightStemFilter(input, BOKMAAL)}
   */
  public NorwegianLightStemFilter(TokenStream input) {
    this(input, NorwegianLightStemmer.BOKMAAL);
  }
  
  /** 
   * Creates a new NorwegianLightStemFilter
   * @param flags set to {@link NorwegianLightStemmer#BOKMAAL}, 
   *                     {@link NorwegianLightStemmer#NYNORSK}, or both.
   */
  public NorwegianLightStemFilter(TokenStream input, int flags) {
    super(input);
    stemmer = new NorwegianLightStemmer(flags);
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
