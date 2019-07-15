import org.apache.lucene.search.*;



import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;
import org.apache.lucene.util.BytesRef;

/** Implementation class for {@link MaxNonCompetitiveBoostAttribute}.
 * @lucene.internal
 */
public final class MaxNonCompetitiveBoostAttributeImpl extends AttributeImpl implements MaxNonCompetitiveBoostAttribute {
  private float maxNonCompetitiveBoost = Float.NEGATIVE_INFINITY;
  private BytesRef competitiveTerm = null;

  @Override
  public void setMaxNonCompetitiveBoost(final float maxNonCompetitiveBoost) {
    this.maxNonCompetitiveBoost = maxNonCompetitiveBoost;
  }
  
  @Override
  public float getMaxNonCompetitiveBoost() {
    return maxNonCompetitiveBoost;
  }

  @Override
  public void setCompetitiveTerm(final BytesRef competitiveTerm) {
    this.competitiveTerm = competitiveTerm;
  }
  
  @Override
  public BytesRef getCompetitiveTerm() {
    return competitiveTerm;
  }

  @Override
  public void clear() {
    maxNonCompetitiveBoost = Float.NEGATIVE_INFINITY;
    competitiveTerm = null;
  }
  
  @Override
  public void copyTo(AttributeImpl target) {
    final MaxNonCompetitiveBoostAttributeImpl t = (MaxNonCompetitiveBoostAttributeImpl) target;
    t.setMaxNonCompetitiveBoost(maxNonCompetitiveBoost);
    t.setCompetitiveTerm(competitiveTerm);
  }

  @Override
  public void reflectWith(AttributeReflector reflector) {
    reflector.reflect(MaxNonCompetitiveBoostAttribute.class, "maxNonCompetitiveBoost", maxNonCompetitiveBoost);
    reflector.reflect(MaxNonCompetitiveBoostAttribute.class, "competitiveTerm", competitiveTerm);
  }
}
