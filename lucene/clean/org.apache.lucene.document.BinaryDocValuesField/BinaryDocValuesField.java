import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.*;


import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.util.BytesRef;

/**
 * Field that stores a per-document {@link BytesRef} value.  
 * <p>
 * The values are stored directly with no sharing, which is a good fit when
 * the fields don't share (many) values, such as a title field.  If values 
 * may be shared and sorted it's better to use {@link SortedDocValuesField}.  
 * Here's an example usage:
 * 
 * <pre class="prettyprint">
 *   document.add(new BinaryDocValuesField(name, new BytesRef("hello")));
 * </pre>
 * 
 * <p>
 * If you also need to store the value, you should add a
 * separate {@link StoredField} instance.
 * 
 * @see BinaryDocValues
 * */
public class BinaryDocValuesField extends Field {
  
  /**
   * Type for straight bytes DocValues.
   */
  public static final FieldType TYPE = new FieldType();
  static {
    TYPE.setDocValuesType(DocValuesType.BINARY);
    TYPE.freeze();
  }
  
  /**
   * Create a new binary DocValues field.
   * @param name field name
   * @param value binary content
   * @throws IllegalArgumentException if the field name is null
   */
  public BinaryDocValuesField(String name, BytesRef value) {
    super(name, TYPE);
    fieldsData = value;
  }
}
