import org.apache.accumulo.tserver.*;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.accumulo.core.data.Key;

import com.google.common.annotations.VisibleForTesting;

@VisibleForTesting
public class MemKey extends Key {

  private int kvCount;

  public MemKey(byte[] row, byte[] cf, byte[] cq, byte[] cv, long ts, boolean del, boolean copy,
      int mc) {
    super(row, cf, cq, cv, ts, del, copy);
    this.kvCount = mc;
  }

  public MemKey() {
    super();
    this.kvCount = Integer.MAX_VALUE;
  }

  public MemKey(Key key, int mc) {
    super(key);
    this.kvCount = mc;
  }

  @Override
  public String toString() {
    return super.toString() + " mc=" + kvCount;
  }

  public int getKVCount() {
    return this.kvCount;
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  @Override
  public void write(DataOutput out) throws IOException {
    super.write(out);
    out.writeInt(kvCount);
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    super.readFields(in);
    kvCount = in.readInt();
  }

  @Override
  public int compareTo(Key k) {

    int cmp = super.compareTo(k);

    if (cmp == 0 && k instanceof MemKey) {
      cmp = ((MemKey) k).kvCount - kvCount;
    }

    return cmp;
  }

}
