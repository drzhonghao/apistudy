import org.apache.lucene.util.mutable.MutableValue;
import org.apache.lucene.util.mutable.*;


import java.util.Date;

/**
 * {@link MutableValue} implementation of type {@link Date}.
 * @see MutableValueLong
 */
public class MutableValueDate extends MutableValueLong {
  @Override
  public Object toObject() {
    return exists ? new Date(value) : null;
  }

  @Override
  public MutableValue duplicate() {
    MutableValueDate v = new MutableValueDate();
    v.value = this.value;
    v.exists = this.exists;
    return v;
  }  
}
