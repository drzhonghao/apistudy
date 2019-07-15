import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.tokenattributes.*;



import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;

/** Default implementation of {@link TypeAttribute}. */
public class TypeAttributeImpl extends AttributeImpl implements TypeAttribute, Cloneable {
  private String type;
  
  /** Initialize this attribute with {@link TypeAttribute#DEFAULT_TYPE} */
  public TypeAttributeImpl() {
    this(DEFAULT_TYPE); 
  }
  
  /** Initialize this attribute with <code>type</code> */
  public TypeAttributeImpl(String type) {
    this.type = type;
  }
  
  @Override
  public String type() {
    return type;
  }

  @Override
  public void setType(String type) {
    this.type = type;
  }

  @Override
  public void clear() {
    type = DEFAULT_TYPE;    
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    
    if (other instanceof TypeAttributeImpl) {
      final TypeAttributeImpl o = (TypeAttributeImpl) other;
      return (this.type == null ? o.type == null : this.type.equals(o.type));
    }
    
    return false;
  }

  @Override
  public int hashCode() {
    return (type == null) ? 0 : type.hashCode();
  }
  
  @Override
  public void copyTo(AttributeImpl target) {
    TypeAttribute t = (TypeAttribute) target;
    t.setType(type);
  }

  @Override
  public void reflectWith(AttributeReflector reflector) {
    reflector.reflect(TypeAttribute.class, "type", type);
  }
}
