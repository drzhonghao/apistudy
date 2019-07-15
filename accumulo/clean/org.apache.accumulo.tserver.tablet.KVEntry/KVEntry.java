import org.apache.accumulo.tserver.tablet.*;


import java.util.Arrays;

import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.KeyValue;
import org.apache.accumulo.core.data.Value;

public class KVEntry extends KeyValue {
  private static final long serialVersionUID = 1L;

  public KVEntry(Key k, Value v) {
    super(new Key(k), Arrays.copyOf(v.get(), v.get().length));
  }

  int numBytes() {
    return getKey().getSize() + getValue().get().length;
  }

  int estimateMemoryUsed() {
    return getKey().getSize() + getValue().get().length + (9 * 32); // overhead is 32 per object
  }
}
