import org.apache.lucene.index.*;



import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.codecs.FieldsProducer;

/**
 * Provides a {@link Terms} index for fields that have it, and lists which fields do.
 * This is primarily an internal/experimental API (see {@link FieldsProducer}),
 * although it is also used to expose the set of term vectors per document.
 *
 * @lucene.experimental
 */
public abstract class Fields implements Iterable<String> {

  /** Sole constructor. (For invocation by subclass 
   *  constructors, typically implicit.) */
  protected Fields() {
  }

  /** Returns an iterator that will step through all fields
   *  names.  This will not return null.  */
  @Override
  public abstract Iterator<String> iterator();

  /** Get the {@link Terms} for this field.  This will return
   *  null if the field does not exist. */
  public abstract Terms terms(String field) throws IOException;

  /** Returns the number of fields or -1 if the number of
   * distinct field names is unknown. If &gt;= 0,
   * {@link #iterator} will return as many field names. */
  public abstract int size();
  
  /** Zero-length {@code Fields} array. */
  public final static Fields[] EMPTY_ARRAY = new Fields[0];
}
