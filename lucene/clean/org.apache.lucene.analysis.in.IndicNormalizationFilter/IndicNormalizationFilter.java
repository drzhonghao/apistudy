import org.apache.lucene.analysis.in.IndicNormalizer;
import org.apache.lucene.analysis.in.*;



import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * A {@link TokenFilter} that applies {@link IndicNormalizer} to normalize text
 * in Indian Languages.
 */
public final class IndicNormalizationFilter extends TokenFilter {
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final IndicNormalizer normalizer = new IndicNormalizer();
  
  public IndicNormalizationFilter(TokenStream input) {
    super(input);
  }
  
  @Override
  public boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      termAtt.setLength(normalizer.normalize(termAtt.buffer(), termAtt.length()));
      return true;
    } else {
      return false;
    }
  }
}
