import org.apache.lucene.facet.*;


/** Single label and its value, usually contained in a
 *  {@link FacetResult}. */
public final class LabelAndValue {
  /** Facet's label. */
  public final String label;

  /** Value associated with this label. */
  public final Number value;

  /** Sole constructor. */
  public LabelAndValue(String label, Number value) {
    this.label = label;
    this.value = value;
  }

  @Override
  public String toString() {
    return label + " (" + value + ")";
  }

  @Override
  public boolean equals(Object _other) {
    if ((_other instanceof LabelAndValue) == false) {
      return false;
    }
    LabelAndValue other = (LabelAndValue) _other;
    return label.equals(other.label) && value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return label.hashCode() + 1439 * value.hashCode();
  }
}
