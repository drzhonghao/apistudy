import org.apache.lucene.analysis.tokenattributes.TermFrequencyAttribute;
import org.apache.lucene.analysis.tokenattributes.*;



import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;

/** Default implementation of {@link TermFrequencyAttribute}. */
public class TermFrequencyAttributeImpl extends AttributeImpl implements TermFrequencyAttribute, Cloneable {
  private int termFrequency = 1;
  
  /** Initialize this attribute with term frequencey of 1 */
  public TermFrequencyAttributeImpl() {}

  @Override
  public void setTermFrequency(int termFrequency) {
    if (termFrequency < 1) {
      throw new IllegalArgumentException("Term frequency must be 1 or greater; got " + termFrequency);
    }
    this.termFrequency = termFrequency;
  }

  @Override
  public int getTermFrequency() {
    return termFrequency;
  }

  @Override
  public void clear() {
    this.termFrequency = 1;
  }
  
  @Override
  public void end() {
    this.termFrequency = 1;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    
    if (other instanceof TermFrequencyAttributeImpl) {
      TermFrequencyAttributeImpl _other = (TermFrequencyAttributeImpl) other;
      return termFrequency ==  _other.termFrequency;
    }
 
    return false;
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(termFrequency);
  }
  
  @Override
  public void copyTo(AttributeImpl target) {
    TermFrequencyAttribute t = (TermFrequencyAttribute) target;
    t.setTermFrequency(termFrequency);
  }  

  @Override
  public void reflectWith(AttributeReflector reflector) {
    reflector.reflect(TermFrequencyAttribute.class, "termFrequency", termFrequency);
  }
}
