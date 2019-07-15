import org.apache.lucene.analysis.id.IndonesianStemmer;
import org.apache.lucene.analysis.id.*;



import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;

/**
 * A {@link TokenFilter} that applies {@link IndonesianStemmer} to stem Indonesian words.
 */
public final class IndonesianStemFilter extends TokenFilter {
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final KeywordAttribute keywordAtt = addAttribute(KeywordAttribute.class);
  private final IndonesianStemmer stemmer = new IndonesianStemmer();
  private final boolean stemDerivational;

  /**
   * Calls {@link #IndonesianStemFilter(TokenStream, boolean) IndonesianStemFilter(input, true)}
   */
  public IndonesianStemFilter(TokenStream input) {
    this(input, true);
  }
  
  /**
   * Create a new IndonesianStemFilter.
   * <p>
   * If <code>stemDerivational</code> is false, 
   * only inflectional suffixes (particles and possessive pronouns) are stemmed.
   */
  public IndonesianStemFilter(TokenStream input, boolean stemDerivational) {
    super(input);
    this.stemDerivational = stemDerivational;
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      if(!keywordAtt.isKeyword()) {
        final int newlen = 
          stemmer.stem(termAtt.buffer(), termAtt.length(), stemDerivational);
        termAtt.setLength(newlen);
      }
      return true;
    } else {
      return false;
    }
  }
}
