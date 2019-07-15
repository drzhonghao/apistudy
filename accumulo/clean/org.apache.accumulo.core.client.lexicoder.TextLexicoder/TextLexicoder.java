import org.apache.accumulo.core.client.lexicoder.*;


import org.apache.accumulo.core.client.lexicoder.impl.AbstractLexicoder;
import org.apache.accumulo.core.util.TextUtil;
import org.apache.hadoop.io.Text;

/**
 * A lexicoder that preserves a Text's native sort order. It can be combined with other encoders
 * like the {@link ReverseLexicoder} to flip the default sort order.
 *
 * @since 1.6.0
 */

public class TextLexicoder extends AbstractLexicoder<Text> {

  @Override
  public byte[] encode(Text data) {
    return TextUtil.getBytes(data);
  }

  @Override
  public Text decode(byte[] b) {
    // This concrete implementation is provided for binary compatibility with 1.6; it can be removed
    // in 2.0. See ACCUMULO-3789.
    return super.decode(b);
  }

  @Override
  protected Text decodeUnchecked(byte[] data, int offset, int len) {
    Text text = new Text();
    text.set(data, offset, len);
    return text;
  }

}
