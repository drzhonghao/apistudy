import org.apache.lucene.analysis.bn.BengaliNormalizer;
import org.apache.lucene.analysis.bn.*;



import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;

import java.io.IOException;

/**
 * A {@link TokenFilter} that applies {@link BengaliNormalizer} to normalize the
 * orthography.
 * <p>
 * In some cases the normalization may cause unrelated terms to conflate, so
 * to prevent terms from being normalized use an instance of
 * {@link SetKeywordMarkerFilter} or a custom {@link TokenFilter} that sets
 * the {@link KeywordAttribute} before this {@link TokenStream}.
 * </p>
 * @see BengaliNormalizer
 */
public final class BengaliNormalizationFilter extends TokenFilter {

  private final BengaliNormalizer normalizer = new BengaliNormalizer();
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final KeywordAttribute keywordAtt = addAttribute(KeywordAttribute.class);
  
  public BengaliNormalizationFilter(TokenStream input) {
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
