import org.apache.lucene.analysis.payloads.AbstractEncoder;
import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.analysis.payloads.*;


import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;


/**
 *  Encode a character array Integer as a {@link BytesRef}.
 * <p>
 * See {@link org.apache.lucene.analysis.payloads.PayloadHelper#encodeInt(int, byte[], int)}.
 *
 **/
public class IntegerEncoder extends AbstractEncoder implements PayloadEncoder {

  @Override
  public BytesRef encode(char[] buffer, int offset, int length) {
    int payload = ArrayUtil.parseInt(buffer, offset, length);//TODO: improve this so that we don't have to new Strings
    byte[] bytes = PayloadHelper.encodeInt(payload);
    BytesRef result = new BytesRef(bytes);
    return result;
  }
}
