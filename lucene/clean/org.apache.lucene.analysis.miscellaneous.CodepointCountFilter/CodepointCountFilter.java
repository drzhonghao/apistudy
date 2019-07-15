import org.apache.lucene.analysis.miscellaneous.*;



import org.apache.lucene.analysis.FilteringTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Removes words that are too long or too short from the stream.
 * <p>
 * Note: Length is calculated as the number of Unicode codepoints.
 * </p>
 */
public final class CodepointCountFilter extends FilteringTokenFilter {

  private final int min;
  private final int max;
  
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

  /**
   * Create a new {@link CodepointCountFilter}. This will filter out tokens whose
   * {@link CharTermAttribute} is either too short ({@link Character#codePointCount(char[], int, int)}
   * &lt; min) or too long ({@link Character#codePointCount(char[], int, int)} &gt; max).
   * @param in      the {@link TokenStream} to consume
   * @param min     the minimum length
   * @param max     the maximum length
   */
  public CodepointCountFilter(TokenStream in, int min, int max) {
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
    final int max32 = termAtt.length();
    final int min32 = max32 >> 1;
    if (min32 >= min && max32 <= max) {
      // definitely within range
      return true;
    } else if (min32 > max || max32 < min) {
      // definitely not
      return false;
    } else {
      // we must count to be sure
      int len = Character.codePointCount(termAtt.buffer(), 0, termAtt.length());
      return (len >= min && len <= max);
    }
  }
}
