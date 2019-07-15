import org.apache.lucene.analysis.tokenattributes.FlagsAttribute;
import org.apache.lucene.analysis.tokenattributes.*;



import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;

/** Default implementation of {@link FlagsAttribute}. */
public class FlagsAttributeImpl extends AttributeImpl implements FlagsAttribute, Cloneable {
  private int flags = 0;
  
  /** Initialize this attribute with no bits set */
  public FlagsAttributeImpl() {}
  
  @Override
  public int getFlags() {
    return flags;
  }

  @Override
  public void setFlags(int flags) {
    this.flags = flags;
  }
  
  @Override
  public void clear() {
    flags = 0;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    
    if (other instanceof FlagsAttributeImpl) {
      return ((FlagsAttributeImpl) other).flags == flags;
    }
    
    return false;
  }

  @Override
  public int hashCode() {
    return flags;
  }
  
  @Override
  public void copyTo(AttributeImpl target) {
    FlagsAttribute t = (FlagsAttribute) target;
    t.setFlags(flags);
  }

  @Override
  public void reflectWith(AttributeReflector reflector) {
    reflector.reflect(FlagsAttribute.class, "flags", flags);
  }
}
