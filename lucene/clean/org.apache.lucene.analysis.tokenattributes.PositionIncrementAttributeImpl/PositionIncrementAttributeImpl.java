import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.*;



import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;

/** Default implementation of {@link PositionIncrementAttribute}. */
public class PositionIncrementAttributeImpl extends AttributeImpl implements PositionIncrementAttribute, Cloneable {
  private int positionIncrement = 1;
  
  /** Initialize this attribute with position increment of 1 */
  public PositionIncrementAttributeImpl() {}

  @Override
  public void setPositionIncrement(int positionIncrement) {
    if (positionIncrement < 0) {
      throw new IllegalArgumentException("Position increment must be zero or greater; got " + positionIncrement);
    }
    this.positionIncrement = positionIncrement;
  }

  @Override
  public int getPositionIncrement() {
    return positionIncrement;
  }

  @Override
  public void clear() {
    this.positionIncrement = 1;
  }
  
  @Override
  public void end() {
    this.positionIncrement = 0;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    
    if (other instanceof PositionIncrementAttributeImpl) {
      PositionIncrementAttributeImpl _other = (PositionIncrementAttributeImpl) other;
      return positionIncrement ==  _other.positionIncrement;
    }
 
    return false;
  }

  @Override
  public int hashCode() {
    return positionIncrement;
  }
  
  @Override
  public void copyTo(AttributeImpl target) {
    PositionIncrementAttribute t = (PositionIncrementAttribute) target;
    t.setPositionIncrement(positionIncrement);
  }  

  @Override
  public void reflectWith(AttributeReflector reflector) {
    reflector.reflect(PositionIncrementAttribute.class, "positionIncrement", positionIncrement);
  }
}
