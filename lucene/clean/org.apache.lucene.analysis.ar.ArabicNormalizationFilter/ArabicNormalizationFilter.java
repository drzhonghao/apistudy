import org.apache.lucene.analysis.ar.ArabicNormalizer;
import org.apache.lucene.analysis.ar.*;



import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * A {@link TokenFilter} that applies {@link ArabicNormalizer} to normalize the orthography.
 * 
 */

public final class ArabicNormalizationFilter extends TokenFilter {
  private final ArabicNormalizer normalizer = new ArabicNormalizer();
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  
  public ArabicNormalizationFilter(TokenStream input) {
    super(input);
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      int newlen = normalizer.normalize(termAtt.buffer(), termAtt.length());
      termAtt.setLength(newlen);
      return true;
    }
    return false;
  }
}
