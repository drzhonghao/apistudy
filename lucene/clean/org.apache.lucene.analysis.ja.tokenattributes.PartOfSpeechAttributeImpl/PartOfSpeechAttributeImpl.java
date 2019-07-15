import org.apache.lucene.analysis.ja.tokenattributes.PartOfSpeechAttribute;
import org.apache.lucene.analysis.ja.tokenattributes.*;



import org.apache.lucene.analysis.ja.Token;
import org.apache.lucene.analysis.ja.util.ToStringUtil;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;

/**
 * Attribute for {@link Token#getPartOfSpeech()}.
 */
public class PartOfSpeechAttributeImpl extends AttributeImpl implements PartOfSpeechAttribute, Cloneable {
  private Token token;
  
  @Override
  public String getPartOfSpeech() {
    return token == null ? null : token.getPartOfSpeech();
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
    PartOfSpeechAttribute t = (PartOfSpeechAttribute) target;
    t.setToken(token);
  }
  
  @Override
  public void reflectWith(AttributeReflector reflector) {
    String partOfSpeech = getPartOfSpeech();
    String partOfSpeechEN = partOfSpeech == null ? null : ToStringUtil.getPOSTranslation(partOfSpeech);
    reflector.reflect(PartOfSpeechAttribute.class, "partOfSpeech", partOfSpeech);
    reflector.reflect(PartOfSpeechAttribute.class, "partOfSpeech (en)", partOfSpeechEN);
  }
}
