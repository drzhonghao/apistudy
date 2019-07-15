import org.apache.lucene.analysis.hi.HindiNormalizer;
import org.apache.lucene.analysis.hi.*;



import java.io.IOException;

import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter; // javadoc @link
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * A {@link TokenFilter} that applies {@link HindiNormalizer} to normalize the
 * orthography.
 * <p>
 * In some cases the normalization may cause unrelated terms to conflate, so
 * to prevent terms from being normalized use an instance of
 * {@link SetKeywordMarkerFilter} or a custom {@link TokenFilter} that sets
 * the {@link KeywordAttribute} before this {@link TokenStream}.
 * </p>
 * @see HindiNormalizer
 */
public final class HindiNormalizationFilter extends TokenFilter {

  private final HindiNormalizer normalizer = new HindiNormalizer();
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final KeywordAttribute keywordAtt = addAttribute(KeywordAttribute.class);
  
  public HindiNormalizationFilter(TokenStream input) {
    super(input);
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      if (!keywordAtt.isKeyword())
        termAtt.setLength(normalizer.normalize(termAtt.buffer(), 
            termAtt.length()));
      return true;
    } 
    return false;
  }
}
