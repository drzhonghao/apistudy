import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.*;


import org.apache.lucene.index.IndexReader; // javadocs
import org.apache.lucene.search.IndexSearcher; // javadocs
import org.apache.lucene.util.BytesRef;

/** A field whose value is stored so that {@link
 *  IndexSearcher#doc} and {@link IndexReader#document IndexReader.document()} will
 *  return the field and its value. */
public class StoredField extends Field {

  /**
   * Type for a stored-only field.
   */
  public final static FieldType TYPE;
  static {
    TYPE = new FieldType();
    TYPE.setStored(true);
    TYPE.freeze();
  }

  /**
   * Expert: allows you to customize the {@link
   * FieldType}.
   * @param name field name
   * @param type custom {@link FieldType} for this field
   * @throws IllegalArgumentException if the field name is null.
   */
  protected StoredField(String name, FieldType type) {
    super(name, type);
  }
  
  /**
   * Expert: allows you to customize the {@link
   * FieldType}.
   * <p>NOTE: the provided byte[] is not copied so be sure
   * not to change it until you're done with this field.
   * @param name field name
   * @param bytes byte array pointing to binary content (not copied)
   * @param type custom {@link FieldType} for this field
   * @throws IllegalArgumentException if the field name is null.
   */
  public StoredField(String name, BytesRef bytes, FieldType type) {
    super(name, bytes, type);
  }
  
  /**
   * Create a stored-only field with the given binary value.
   * <p>NOTE: the provided byte[] is not copied so be sure
   * not to change it until you're done with this field.
   * @param name field name
   * @param value byte array pointing to binary content (not copied)
   * @throws IllegalArgumentException if the field name is null.
   */
  public StoredField(String name, byte[] value) {
    super(name, value, TYPE);
  }
  
  /**
   * Create a stored-only field with the given binary value.
   * <p>NOTE: the provided byte[] is not copied so be sure
   * not to change it until you're done with this field.
   * @param name field name
   * @param value byte array pointing to binary content (not copied)
   * @param offset starting position of the byte array
   * @param length valid length of the byte array
   * @throws IllegalArgumentException if the field name is null.
   */
  public StoredField(String name, byte[] value, int offset, int length) {
    super(name, value, offset, length, TYPE);
  }

  /**
   * Create a stored-only field with the given binary value.
   * <p>NOTE: the provided BytesRef is not copied so be sure
   * not to change it until you're done with this field.
   * @param name field name
   * @param value BytesRef pointing to binary content (not copied)
   * @throws IllegalArgumentException if the field name is null.
   */
  public StoredField(String name, BytesRef value) {
    super(name, value, TYPE);
  }

  /**
   * Create a stored-only field with the given string value.
   * @param name field name
   * @param value string value
   * @throws IllegalArgumentException if the field name or value is null.
   */
  public StoredField(String name, String value) {
    super(name, value, TYPE);
  }
  
  /**
   * Expert: allows you to customize the {@link
   * FieldType}.
   * @param name field name
   * @param value string value
   * @param type custom {@link FieldType} for this field
   * @throws IllegalArgumentException if the field name or value is null.
   */
  public StoredField(String name, String value, FieldType type) {
    super(name, value, type);
  }

  // TODO: not great but maybe not a big problem?
  /**
   * Create a stored-only field with the given integer value.
   * @param name field name
   * @param value integer value
   * @throws IllegalArgumentException if the field name is null.
   */
  public StoredField(String name, int value) {
    super(name, TYPE);
    fieldsData = value;
  }

  /**
   * Create a stored-only field with the given float value.
   * @param name field name
   * @param value float value
   * @throws IllegalArgumentException if the field name is null.
   */
  public StoredField(String name, float value) {
    super(name, TYPE);
    fieldsData = value;
  }

  /**
   * Create a stored-only field with the given long value.
   * @param name field name
   * @param value long value
   * @throws IllegalArgumentException if the field name is null.
   */
  public StoredField(String name, long value) {
    super(name, TYPE);
    fieldsData = value;
  }

  /**
   * Create a stored-only field with the given double value.
   * @param name field name
   * @param value double value
   * @throws IllegalArgumentException if the field name is null.
   */
  public StoredField(String name, double value) {
    super(name, TYPE);
    fieldsData = value;
  }
}
