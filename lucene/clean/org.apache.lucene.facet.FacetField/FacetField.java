import org.apache.lucene.facet.*;


import java.util.Arrays;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;

/**
 * Add an instance of this to your {@link Document} for every facet label.
 * 
 * <p>
 * <b>NOTE:</b> you must call {@link FacetsConfig#build(Document)} before
 * you add the document to IndexWriter.
 */
public class FacetField extends Field {
  
  /** Field type used for storing facet values: docs, freqs, and positions. */
  public static final FieldType TYPE = new FieldType();
  static {
    TYPE.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
    TYPE.freeze();
  }

  /** Dimension for this field. */
  public final String dim;

  /** Path for this field. */
  public final String[] path;

  /** Creates the this from {@code dim} and
   *  {@code path}. */
  public FacetField(String dim, String... path) {
    super("dummy", TYPE);
    verifyLabel(dim);
    for(String label : path) {
      verifyLabel(label);
    }
    this.dim = dim;
    if (path.length == 0) {
      throw new IllegalArgumentException("path must have at least one element");
    }
    this.path = path;
  }

  @Override
  public String toString() {
    return "FacetField(dim=" + dim + " path=" + Arrays.toString(path) + ")";
  }

  /** Verifies the label is not null or empty string.
   * 
   *  @lucene.internal */
  public static void verifyLabel(String label) {
    if (label == null || label.isEmpty()) {
      throw new IllegalArgumentException("empty or null components not allowed; got: " + label);
    }
  }
}
