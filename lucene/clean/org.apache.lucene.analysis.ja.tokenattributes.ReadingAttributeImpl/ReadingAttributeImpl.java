import org.apache.lucene.analysis.ja.tokenattributes.ReadingAttribute;
import org.apache.lucene.analysis.ja.tokenattributes.*;



import org.apache.lucene.analysis.ja.Token;
import org.apache.lucene.analysis.ja.util.ToStringUtil;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;

/**
 * Attribute for Kuromoji reading data
 */
public class ReadingAttributeImpl extends AttributeImpl implements ReadingAttribute, Cloneable {
  private Token token;
  
  @Override
  public String getReading() {
    return token == null ? null : token.getReading();
  }
  
  @Override
  public String getPronunciation() {
    return token == null ? null : token.getPronunciation();
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
    String reading = getReading();
    String readingEN = reading == null ? null : ToStringUtil.getRomanization(reading);
    String pronunciation = getPronunciation();
    String pronunciationEN = pronunciation == null ? null : ToStringUtil.getRomanization(pronunciation);
    reflector.reflect(ReadingAttribute.class, "reading", reading);
    reflector.reflect(ReadingAttribute.class, "reading (en)", readingEN);
    reflector.reflect(ReadingAttribute.class, "pronunciation", pronunciation);
    reflector.reflect(ReadingAttribute.class, "pronunciation (en)", pronunciationEN);
  }
}
