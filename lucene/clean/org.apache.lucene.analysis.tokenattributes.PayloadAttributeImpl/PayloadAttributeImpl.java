import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.*;



import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;
import org.apache.lucene.util.BytesRef;

/** Default implementation of {@link PayloadAttribute}. */
public class PayloadAttributeImpl extends AttributeImpl implements PayloadAttribute, Cloneable {
  private BytesRef payload;  
  
  /**
   * Initialize this attribute with no payload.
   */
  public PayloadAttributeImpl() {}
  
  /**
   * Initialize this attribute with the given payload. 
   */
  public PayloadAttributeImpl(BytesRef payload) {
    this.payload = payload;
  }
  
  @Override
  public BytesRef getPayload() {
    return this.payload;
  }

  @Override
  public void setPayload(BytesRef payload) {
    this.payload = payload;
  }
  
  @Override
  public void clear() {
    payload = null;
  }

  @Override
  public PayloadAttributeImpl clone()  {
    PayloadAttributeImpl clone = (PayloadAttributeImpl) super.clone();
    if (payload != null) {
      clone.payload = BytesRef.deepCopyOf(payload);
    }
    return clone;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    
    if (other instanceof PayloadAttribute) {
      PayloadAttributeImpl o = (PayloadAttributeImpl) other;
      if (o.payload == null || payload == null) {
        return o.payload == null && payload == null;
      }
      
      return o.payload.equals(payload);
    }
    
    return false;
  }

  @Override
  public int hashCode() {
    return (payload == null) ? 0 : payload.hashCode();
  }

  @Override
  public void copyTo(AttributeImpl target) {
    PayloadAttribute t = (PayloadAttribute) target;
    t.setPayload((payload == null) ? null : BytesRef.deepCopyOf(payload));
  }  

  @Override
  public void reflectWith(AttributeReflector reflector) {
    reflector.reflect(PayloadAttribute.class, "payload", payload);
  }
}
