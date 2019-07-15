import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.*;



import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;

/** Default implementation of {@link KeywordAttribute}. */
public final class KeywordAttributeImpl extends AttributeImpl implements
    KeywordAttribute {
  private boolean keyword;
  
  /** Initialize this attribute with the keyword value as false. */
  public KeywordAttributeImpl() {}

  @Override
  public void clear() {
    keyword = false;
  }

  @Override
  public void copyTo(AttributeImpl target) {
    KeywordAttribute attr = (KeywordAttribute) target;
    attr.setKeyword(keyword);
  }

  @Override
  public int hashCode() {
    return keyword ? 31 : 37;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (getClass() != obj.getClass())
      return false;
    final KeywordAttributeImpl other = (KeywordAttributeImpl) obj;
    return keyword == other.keyword;
  }

  @Override
  public boolean isKeyword() {
    return keyword;
  }

  @Override
  public void setKeyword(boolean isKeyword) {
    keyword = isKeyword;
  }

  @Override
  public void reflectWith(AttributeReflector reflector) {
    reflector.reflect(KeywordAttribute.class, "keyword", keyword);
  }
}
