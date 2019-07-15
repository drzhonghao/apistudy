import org.apache.lucene.analysis.miscellaneous.*;



import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.FilteringTokenFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Removes words that are too long or too short from the stream.
 * <p>
 * Note: Length is calculated as the number of UTF-16 code units.
 * </p>
 */
public final class LengthFilter extends FilteringTokenFilter {

  private final int min;
  private final int max;

  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

  /**
   * Create a new {@link LengthFilter}. This will filter out tokens whose
   * {@link CharTermAttribute} is either too short ({@link CharTermAttribute#length()}
   * &lt; min) or too long ({@link CharTermAttribute#length()} &gt; max).
   * @param in      the {@link TokenStream} to consume
   * @param min     the minimum length
   * @param max     the maximum length
   */
  public LengthFilter(TokenStream in, int min, int max) {
    super(in);
    if (min < 0) {
      throw new IllegalArgumentException("minimum length must be greater than or equal to zero");
    }
    if (min > max) {
      throw new IllegalArgumentException("maximum length must not be greater than minimum length");
    }
    this.min = min;
    this.max = max;
  }

  @Override
  public boolean accept() {
    final int len = termAtt.length();
    return (len >= min && len <= max);
  }
}
