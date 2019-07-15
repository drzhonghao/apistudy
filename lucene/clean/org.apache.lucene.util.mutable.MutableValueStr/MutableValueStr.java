import org.apache.lucene.util.mutable.*;


import org.apache.lucene.util.BytesRefBuilder;

/**
 * {@link MutableValue} implementation of type {@link String}.
 * When mutating instances of this object, the caller is responsible for ensuring 
 * that any instance where <code>exists</code> is set to <code>false</code> must also 
 * have a <code>value</code> with a length set to 0.
 */
public class MutableValueStr extends MutableValue {
  public BytesRefBuilder value = new BytesRefBuilder();

  @Override
  public Object toObject() {
    assert exists || 0 == value.length();
    return exists ? value.get().utf8ToString() : null;
  }

  @Override
  public void copy(MutableValue source) {
    MutableValueStr s = (MutableValueStr) source;
    exists = s.exists;
    value.copyBytes(s.value);
  }

  @Override
  public MutableValue duplicate() {
    MutableValueStr v = new MutableValueStr();
    v.value.copyBytes(value);
    v.exists = this.exists;
    return v;
  }

  @Override
  public boolean equalsSameType(Object other) {
    assert exists || 0 == value.length();
    MutableValueStr b = (MutableValueStr)other;
    return value.get().equals(b.value.get()) && exists == b.exists;
  }

  @Override
  public int compareSameType(Object other) {
    assert exists || 0 == value.length();
    MutableValueStr b = (MutableValueStr)other;
    int c = value.get().compareTo(b.value.get());
    if (c != 0) return c;
    if (exists == b.exists) return 0;
    return exists ? 1 : -1;
  }


  @Override
  public int hashCode() {
    assert exists || 0 == value.length();
    return value.get().hashCode();
  }
}
