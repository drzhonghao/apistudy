import org.apache.accumulo.core.metadata.schema.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.accumulo.core.data.Value;

public class DataFileValue {
  private long size;
  private long numEntries;
  private long time = -1;

  public DataFileValue(long size, long numEntries, long time) {
    this.size = size;
    this.numEntries = numEntries;
    this.time = time;
  }

  public DataFileValue(long size, long numEntries) {
    this.size = size;
    this.numEntries = numEntries;
    this.time = -1;
  }

  public DataFileValue(byte[] encodedDFV) {
    String[] ba = new String(encodedDFV, UTF_8).split(",");

    size = Long.parseLong(ba[0]);
    numEntries = Long.parseLong(ba[1]);

    if (ba.length == 3)
      time = Long.parseLong(ba[2]);
    else
      time = -1;
  }

  public long getSize() {
    return size;
  }

  public long getNumEntries() {
    return numEntries;
  }

  public boolean isTimeSet() {
    return time >= 0;
  }

  public long getTime() {
    return time;
  }

  public byte[] encode() {
    return encodeAsString().getBytes(UTF_8);
  }

  public String encodeAsString() {
    if (time >= 0)
      return ("" + size + "," + numEntries + "," + time);
    return ("" + size + "," + numEntries);
  }

  public Value encodeAsValue() {
    return new Value(encode());
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof DataFileValue) {
      DataFileValue odfv = (DataFileValue) o;

      return size == odfv.size && numEntries == odfv.numEntries;
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Long.valueOf(size + numEntries).hashCode();
  }

  @Override
  public String toString() {
    return size + " " + numEntries;
  }

  public void setTime(long time) {
    if (time < 0)
      throw new IllegalArgumentException();
    this.time = time;
  }
}
