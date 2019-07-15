import org.apache.lucene.analysis.payloads.*;



import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.BytesRef;


/**
 * Makes the {@link TypeAttribute} a payload.
 *
 * Encodes the type using {@link String#getBytes(String)} with "UTF-8" as the encoding
 *
 **/
public class TypeAsPayloadTokenFilter extends TokenFilter {
  private final PayloadAttribute payloadAtt = addAttribute(PayloadAttribute.class);
  private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

  public TypeAsPayloadTokenFilter(TokenStream input) {
    super(input);
  }


  @Override
  public final boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      String type = typeAtt.type();
      if (type != null && !type.isEmpty()) {
        payloadAtt.setPayload(new BytesRef(type));
      }
      return true;
    } else {
      return false;
    }
  }
}
