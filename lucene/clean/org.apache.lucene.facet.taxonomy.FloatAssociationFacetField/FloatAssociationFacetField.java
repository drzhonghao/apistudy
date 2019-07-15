import org.apache.lucene.facet.taxonomy.AssociationFacetField;
import org.apache.lucene.facet.taxonomy.IntAssociationFacetField;
import org.apache.lucene.facet.taxonomy.*;


import java.util.Arrays;

import org.apache.lucene.document.Document;
import org.apache.lucene.util.BytesRef;

/** Add an instance of this to your {@link Document} to add
 *  a facet label associated with a float.  Use {@link
 *  TaxonomyFacetSumFloatAssociations} to aggregate float values
 *  per facet label at search time.
 * 
 *  @lucene.experimental */
public class FloatAssociationFacetField extends AssociationFacetField {

  /** Creates this from {@code dim} and {@code path} and a
   *  float association */
  public FloatAssociationFacetField(float assoc, String dim, String... path) {
    super(floatToBytesRef(assoc), dim, path);
  }

  /** Encodes a {@code float} as a 4-byte {@link BytesRef}. */
  public static BytesRef floatToBytesRef(float v) {
    return IntAssociationFacetField.intToBytesRef(Float.floatToIntBits(v));
  }

  /** Decodes a previously encoded {@code float}. */
  public static float bytesRefToFloat(BytesRef b) {
    return Float.intBitsToFloat(IntAssociationFacetField.bytesRefToInt(b));
  }

  @Override
  public String toString() {
    return "FloatAssociationFacetField(dim=" + dim + " path=" + Arrays.toString(path) + " value=" + bytesRefToFloat(assoc) + ")";
  }
}
