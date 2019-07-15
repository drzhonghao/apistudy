import org.apache.lucene.analysis.ckb.SoraniNormalizer;
import org.apache.lucene.analysis.ckb.*;



import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * A {@link TokenFilter} that applies {@link SoraniNormalizer} to normalize the
 * orthography.
 */
public final class SoraniNormalizationFilter extends TokenFilter {
  private final SoraniNormalizer normalizer = new SoraniNormalizer();
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

  public SoraniNormalizationFilter(TokenStream input) {
    super(input);
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      final int newlen = normalizer.normalize(termAtt.buffer(), termAtt.length());
      termAtt.setLength(newlen);
      return true;
    } 
    return false;
  }
}
