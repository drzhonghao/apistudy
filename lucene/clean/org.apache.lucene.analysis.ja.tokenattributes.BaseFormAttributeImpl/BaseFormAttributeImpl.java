import org.apache.lucene.analysis.ja.tokenattributes.*;



import org.apache.lucene.analysis.ja.Token;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;

/**
 * Attribute for {@link Token#getBaseForm()}.
 */
public class BaseFormAttributeImpl extends AttributeImpl implements BaseFormAttribute, Cloneable {
  private Token token;
  
  @Override
  public String getBaseForm() {
    return token == null ? null : token.getBaseForm();
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
    BaseFormAttribute t = (BaseFormAttribute) target;
    t.setToken(token);
  }
  
  @Override
  public void reflectWith(AttributeReflector reflector) {
    reflector.reflect(BaseFormAttribute.class, "baseForm", getBaseForm());
  }
}
