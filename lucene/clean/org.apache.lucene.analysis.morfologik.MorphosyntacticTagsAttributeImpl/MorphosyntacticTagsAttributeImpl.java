import org.apache.lucene.analysis.morfologik.*;


import java.util.*;

import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;

/**
 * Morphosyntactic annotations for surface forms.
 * @see MorphosyntacticTagsAttribute
 */
public class MorphosyntacticTagsAttributeImpl extends AttributeImpl 
  implements MorphosyntacticTagsAttribute, Cloneable {
  
  /** Initializes this attribute with no tags */
  public MorphosyntacticTagsAttributeImpl() {}
  
  /**
   * A list of potential tag variants for the current token.
   */
  private List<StringBuilder> tags;

  /**
   * Returns the POS tag of the term. If you need a copy of this char sequence, copy
   * its contents (and clone {@link StringBuilder}s) because it changes with 
   * each new term to avoid unnecessary memory allocations.
   */
  @Override
  public List<StringBuilder> getTags() {
    return tags;
  }

  @Override
  public void clear() {
    tags = null;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof MorphosyntacticTagsAttribute) {
      return equal(this.getTags(), ((MorphosyntacticTagsAttribute) other).getTags());
    }
    return false;
  }

  private boolean equal(Object l1, Object l2) {
    return l1 == null ? (l2 == null) : (l1.equals(l2));
  }

  @Override
  public int hashCode() {
    return this.tags == null ? 0 : tags.hashCode();
  }

  /**
   * Sets the internal tags reference to the given list. The contents
   * is not copied. 
   */
  @Override
  public void setTags(List<StringBuilder> tags) {
    this.tags = tags;
  }

  @Override
  public void copyTo(AttributeImpl target) {
    List<StringBuilder> cloned = null;
    if (tags != null) {
      cloned = new ArrayList<>(tags.size());
      for (StringBuilder b : tags) {
        cloned.add(new StringBuilder(b));
      }
    }
    ((MorphosyntacticTagsAttribute) target).setTags(cloned);
  }

  @Override
  public MorphosyntacticTagsAttributeImpl clone() {
    MorphosyntacticTagsAttributeImpl cloned = new MorphosyntacticTagsAttributeImpl();
    this.copyTo(cloned);
    return cloned;
  }

  @Override
  public void reflectWith(AttributeReflector reflector) {
    reflector.reflect(MorphosyntacticTagsAttribute.class, "tags", tags);
  }
}
