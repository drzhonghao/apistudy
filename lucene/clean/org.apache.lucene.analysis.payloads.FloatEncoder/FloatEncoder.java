import org.apache.lucene.analysis.payloads.AbstractEncoder;
import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.analysis.payloads.*;


import org.apache.lucene.util.BytesRef;


/**
 * Encode a character array Float as a {@link BytesRef}.
 * @see org.apache.lucene.analysis.payloads.PayloadHelper#encodeFloat(float, byte[], int)
 *
 **/
public class FloatEncoder extends AbstractEncoder implements PayloadEncoder {

  @Override
  public BytesRef encode(char[] buffer, int offset, int length) {
    float payload = Float.parseFloat(new String(buffer, offset, length));//TODO: improve this so that we don't have to new Strings
    byte[] bytes = PayloadHelper.encodeFloat(payload);
    BytesRef result = new BytesRef(bytes);
    return result;
  }
}
