import org.apache.lucene.analysis.core.*;



import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.StemmerUtil;

/**
 * Folds all Unicode digits in {@code [:General_Category=Decimal_Number:]}
 * to Basic Latin digits ({@code 0-9}). 
 */
public final class DecimalDigitFilter extends TokenFilter {
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

  /** 
   * Creates a new DecimalDigitFilter over {@code input}
   */
  public DecimalDigitFilter(TokenStream input) {
    super(input);
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      char buffer[] = termAtt.buffer();
      int length = termAtt.length();
      
      for (int i = 0; i < length; i++) {
        int ch = Character.codePointAt(buffer, i, length);
        // look for digits outside of basic latin
        if (ch > 0x7F && Character.isDigit(ch)) {
          // replace with equivalent basic latin digit
          buffer[i] = (char) ('0' + Character.getNumericValue(ch));
          // if the original was supplementary, shrink the string
          if (ch > 0xFFFF) {
            length = StemmerUtil.delete(buffer, i+1, length);
            termAtt.setLength(length);
          }
        }
      }
      
      return true;
    } else {
      return false;
    }
  }
}
