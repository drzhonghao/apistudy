import org.apache.lucene.analysis.icu.tokenattributes.*;



import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;

import com.ibm.icu.lang.UScript;

/**
 * Implementation of {@link ScriptAttribute} that stores the script
 * as an integer.
 * @lucene.experimental
 */
public class ScriptAttributeImpl extends AttributeImpl implements ScriptAttribute, Cloneable {
  private int code = UScript.COMMON;
  
  /** Initializes this attribute with <code>UScript.COMMON</code> */
  public ScriptAttributeImpl() {}
  
  @Override
  public int getCode() {
    return code;
  }
  
  @Override
  public void setCode(int code) {
    this.code = code;
  }

  @Override
  public String getName() {
    return UScript.getName(code);
  }

  @Override
  public String getShortName() {
    return UScript.getShortName(code);
  }
  
  @Override
  public void clear() {
    code = UScript.COMMON;
  }

  @Override
  public void copyTo(AttributeImpl target) {
    ScriptAttribute t = (ScriptAttribute) target;
    t.setCode(code);
  }
  
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    
    if (other instanceof ScriptAttributeImpl) {
      return ((ScriptAttributeImpl) other).code == code;
    }
    
    return false;
  }

  @Override
  public int hashCode() {
    return code;
  }

  @Override
  public void reflectWith(AttributeReflector reflector) {
    // when wordbreaking CJK, we use the 15924 code Japanese (Han+Hiragana+Katakana) to 
    // mark runs of Chinese/Japanese. our use is correct (as for chinese Han is a subset), 
    // but this is just to help prevent confusion.
    String name = code == UScript.JAPANESE ? "Chinese/Japanese" : getName();
    reflector.reflect(ScriptAttribute.class, "script", name);
  }
}
