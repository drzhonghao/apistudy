import org.apache.accumulo.tserver.*;


import org.apache.accumulo.core.data.Value;

/**
 *
 */
public class MemValue {

  Value value;
  int kvCount;

  public MemValue(Value value, int kv) {
    this.value = value;
    this.kvCount = kv;
  }

  public static Value encode(Value value, int kv) {
    byte[] combinedBytes = new byte[value.getSize() + 4];
    System.arraycopy(value.get(), 0, combinedBytes, 4, value.getSize());
    combinedBytes[0] = (byte) (kv >>> 24);
    combinedBytes[1] = (byte) (kv >>> 16);
    combinedBytes[2] = (byte) (kv >>> 8);
    combinedBytes[3] = (byte) (kv);
    return new Value(combinedBytes);
  }

  public static MemValue decode(Value v) {
    byte[] originalBytes = new byte[v.getSize() - 4];
    byte[] combined = v.get();
    System.arraycopy(combined, 4, originalBytes, 0, originalBytes.length);
    int kv = (combined[0] << 24) + ((combined[1] & 0xFF) << 16) + ((combined[2] & 0xFF) << 8)
        + (combined[3] & 0xFF);

    return new MemValue(new Value(originalBytes), kv);
  }
}
