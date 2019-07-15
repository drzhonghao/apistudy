import org.apache.lucene.queries.function.valuesource.*;


import org.apache.lucene.queries.function.ValueSource;

/**
 * A base class for ValueSource implementations that retrieve values for
 * a single field from DocValues.
 *
 *
 */
public abstract class FieldCacheSource extends ValueSource {
  protected final String field;

  public FieldCacheSource(String field) {
    this.field=field;
  }

  public String getField() {
    return field;
  }

  @Override
  public String description() {
    return field;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof FieldCacheSource)) return false;
    FieldCacheSource other = (FieldCacheSource)o;
    return this.field.equals(other.field);
  }

  @Override
  public int hashCode() {
    return field.hashCode();
  }

}
