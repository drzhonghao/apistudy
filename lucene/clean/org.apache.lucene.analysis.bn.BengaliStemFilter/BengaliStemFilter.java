import org.apache.lucene.analysis.bn.BengaliStemmer;
import org.apache.lucene.analysis.bn.*;



import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;

import java.io.IOException;

/**
 * A {@link TokenFilter} that applies {@link BengaliStemmer} to stem Bengali words.
 */
public final class BengaliStemFilter extends TokenFilter {
  private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);
  private final KeywordAttribute keywordAttribute = addAttribute(KeywordAttribute.class);
  private final BengaliStemmer bengaliStemmer = new BengaliStemmer();
  
  public BengaliStemFilter(TokenStream input) {
    super(input);
  }
  
  @Override
  public boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      if (!keywordAttribute.isKeyword())
        termAttribute.setLength(bengaliStemmer.stem(termAttribute.buffer(), termAttribute.length()));
      return true;
    } else {
      return false;
    }
  }
}
