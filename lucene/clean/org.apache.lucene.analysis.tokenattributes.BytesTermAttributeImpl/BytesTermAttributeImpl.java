import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.analysis.tokenattributes.*;



import java.util.Objects;

import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;
import org.apache.lucene.util.BytesRef;

/** Implementation class for {@link BytesTermAttribute}.
 * @lucene.internal
 */
public class BytesTermAttributeImpl extends AttributeImpl implements BytesTermAttribute, TermToBytesRefAttribute {
  private BytesRef bytes;

  /** Initialize this attribute with no bytes. */
  public BytesTermAttributeImpl() {}

  @Override
  public BytesRef getBytesRef() {
    return bytes;
  }

  @Override
  public void setBytesRef(BytesRef bytes) {
    this.bytes = bytes;
  }

  @Override
  public void clear() {
    this.bytes = null;
  }

  @Override
  public void copyTo(AttributeImpl target) {
    BytesTermAttributeImpl other = (BytesTermAttributeImpl) target;
    other.bytes = bytes == null ? null : BytesRef.deepCopyOf(bytes);
  }

  @Override
  public AttributeImpl clone() {
    BytesTermAttributeImpl c = (BytesTermAttributeImpl)super.clone();
    copyTo(c);
    return c;
  }

  @Override
  public void reflectWith(AttributeReflector reflector) {
    reflector.reflect(TermToBytesRefAttribute.class, "bytes", bytes);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BytesTermAttributeImpl)) return false;
    BytesTermAttributeImpl that = (BytesTermAttributeImpl) o;
    return Objects.equals(bytes, that.bytes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bytes);
  }
}
