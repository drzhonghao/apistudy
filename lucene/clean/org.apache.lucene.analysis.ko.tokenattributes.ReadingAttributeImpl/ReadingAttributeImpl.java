import org.apache.lucene.analysis.ko.tokenattributes.ReadingAttribute;
import org.apache.lucene.analysis.ko.tokenattributes.*;


import org.apache.lucene.analysis.ko.Token;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;

/**
 * Attribute for Korean reading data
 * @lucene.experimental
 */
public class ReadingAttributeImpl extends AttributeImpl implements ReadingAttribute, Cloneable {
  private Token token;
  
  @Override
  public String getReading() {
    return token == null ? null : token.getReading();
  }

  @Override
  public void setToken(Token token) {
    this.token = token;
  }

  @Override
  public void clear() {
    token = null;
  }

  @Override
  public void copyTo(AttributeImpl target) {
    ReadingAttribute t = (ReadingAttribute) target;
    t.setToken(token);
  }
  
  @Override
  public void reflectWith(AttributeReflector reflector) {
    reflector.reflect(ReadingAttribute.class, "reading", getReading());
  }
}
