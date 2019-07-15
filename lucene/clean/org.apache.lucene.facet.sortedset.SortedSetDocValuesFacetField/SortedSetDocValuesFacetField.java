import org.apache.lucene.facet.sortedset.*;


import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.index.IndexOptions;

/** Add an instance of this to your Document for every facet
 *  label to be indexed via SortedSetDocValues. */
public class SortedSetDocValuesFacetField extends Field {
  
  /** Indexed {@link FieldType}. */
  public static final FieldType TYPE = new FieldType();
  static {
    // NOTE: we don't actually use these index options, because this field is "processed" by FacetsConfig.build()
    TYPE.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
    TYPE.freeze();
  }

  /** Dimension. */
  public final String dim;

  /** Label. */
  public final String label;

  /** Sole constructor. */
  public SortedSetDocValuesFacetField(String dim, String label) {
    super("dummy", TYPE);
    FacetField.verifyLabel(label);
    FacetField.verifyLabel(dim);
    this.dim = dim;
    this.label = label;
  }

  @Override
  public String toString() {
    return "SortedSetDocValuesFacetField(dim=" + dim + " label=" + label + ")";
  }
}
