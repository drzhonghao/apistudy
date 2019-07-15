import org.apache.lucene.analysis.payloads.*;


import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;


/**
 * Characters before the delimiter are the "token", those after are the payload.
 * <p>
 * For example, if the delimiter is '|', then for the string "foo|bar", foo is the token
 * and "bar" is a payload.
 * <p>
 * Note, you can also include a {@link org.apache.lucene.analysis.payloads.PayloadEncoder} to convert the payload in an appropriate way (from characters to bytes).
 * <p>
 * Note make sure your Tokenizer doesn't split on the delimiter, or this won't work
 *
 * @see PayloadEncoder
 */
public final class DelimitedPayloadTokenFilter extends TokenFilter {
  public static final char DEFAULT_DELIMITER = '|';
  private final char delimiter;
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final PayloadAttribute payAtt = addAttribute(PayloadAttribute.class);
  private final PayloadEncoder encoder;


  public DelimitedPayloadTokenFilter(TokenStream input, char delimiter, PayloadEncoder encoder) {
    super(input);
    this.delimiter = delimiter;
    this.encoder = encoder;
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      final char[] buffer = termAtt.buffer();
      final int length = termAtt.length();
      for (int i = 0; i < length; i++) {
        if (buffer[i] == delimiter) {
          payAtt.setPayload(encoder.encode(buffer, i + 1, (length - (i + 1))));
          termAtt.setLength(i); // simply set a new length
          return true;
        }
      }
      // we have not seen the delimiter
      payAtt.setPayload(null);
      return true;
    } else return false;
  }
}
