import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.analysis.payloads.*;



import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.BytesRef;


/**
 * Assigns a payload to a token based on the {@link org.apache.lucene.analysis.tokenattributes.TypeAttribute}
 **/
public class NumericPayloadTokenFilter extends TokenFilter {

  private String typeMatch;
  private BytesRef thePayload;

  private final PayloadAttribute payloadAtt = addAttribute(PayloadAttribute.class);
  private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

  public NumericPayloadTokenFilter(TokenStream input, float payload, String typeMatch) {
    super(input);
    if (typeMatch == null) {
      throw new IllegalArgumentException("typeMatch must not be null");
    }
    //Need to encode the payload
    thePayload = new BytesRef(PayloadHelper.encodeFloat(payload));
    this.typeMatch = typeMatch;
  }

  @Override
  public final boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      if (typeAtt.type().equals(typeMatch))
        payloadAtt.setPayload(thePayload);
      return true;
    } else {
      return false;
    }
  }
}
