import org.apache.lucene.analysis.fa.*;



import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * A {@link TokenFilter} that applies {@link PersianNormalizer} to normalize the
 * orthography.
 * 
 */

public final class PersianNormalizationFilter extends TokenFilter {
  private final PersianNormalizer normalizer = new PersianNormalizer();
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

  public PersianNormalizationFilter(TokenStream input) {
    super(input);
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      final int newlen = normalizer.normalize(termAtt.buffer(), 
          termAtt.length());
      termAtt.setLength(newlen);
      return true;
    } 
    return false;
  }
}
