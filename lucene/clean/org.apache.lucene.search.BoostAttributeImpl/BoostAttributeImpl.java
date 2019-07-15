import org.apache.lucene.search.*;



import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;

/** Implementation class for {@link BoostAttribute}.
 * @lucene.internal
 */
public final class BoostAttributeImpl extends AttributeImpl implements BoostAttribute {
  private float boost = 1.0f;

  @Override
  public void setBoost(float boost) {
    this.boost = boost;
  }
  
  @Override
  public float getBoost() {
    return boost;
  }

  @Override
  public void clear() {
    boost = 1.0f;
  }
  
  @Override
  public void copyTo(AttributeImpl target) {
    ((BoostAttribute) target).setBoost(boost);
  }

  @Override
  public void reflectWith(AttributeReflector reflector) {
    reflector.reflect(BoostAttribute.class, "boost", boost);
  }
}
