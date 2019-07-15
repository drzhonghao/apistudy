import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.analysis.payloads.*;



import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.util.BytesRef;


/**
 * Adds the {@link OffsetAttribute#startOffset()}
 * and {@link OffsetAttribute#endOffset()}
 * First 4 bytes are the start
 *
 **/
public class TokenOffsetPayloadTokenFilter extends TokenFilter {
  private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
  private final PayloadAttribute payAtt = addAttribute(PayloadAttribute.class);

  public TokenOffsetPayloadTokenFilter(TokenStream input) {
    super(input);
  }

  @Override
  public final boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      byte[] data = new byte[8];
      PayloadHelper.encodeInt(offsetAtt.startOffset(), data, 0);
      PayloadHelper.encodeInt(offsetAtt.endOffset(), data, 4);
      BytesRef payload = new BytesRef(data);
      payAtt.setPayload(payload);
      return true;
    } else {
    return false;
    }
  }
}
