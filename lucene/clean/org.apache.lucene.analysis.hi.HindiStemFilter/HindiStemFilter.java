import org.apache.lucene.analysis.hi.HindiStemmer;
import org.apache.lucene.analysis.hi.*;



import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * A {@link TokenFilter} that applies {@link HindiStemmer} to stem Hindi words.
 */
public final class HindiStemFilter extends TokenFilter {
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final KeywordAttribute keywordAtt = addAttribute(KeywordAttribute.class);
  private final HindiStemmer stemmer = new HindiStemmer();
  
  public HindiStemFilter(TokenStream input) {
    super(input);
  }
  
  @Override
  public boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      if (!keywordAtt.isKeyword())
        termAtt.setLength(stemmer.stem(termAtt.buffer(), termAtt.length()));
      return true;
    } else {
      return false;
    }
  }
}
