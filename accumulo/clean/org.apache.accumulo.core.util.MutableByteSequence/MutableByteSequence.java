import org.apache.accumulo.core.util.*;


import org.apache.accumulo.core.data.ArrayByteSequence;
import org.apache.accumulo.core.data.ByteSequence;

public class MutableByteSequence extends ArrayByteSequence {
  private static final long serialVersionUID = 1L;

  public MutableByteSequence(byte[] data, int offset, int length) {
    super(data, offset, length);
  }

  public MutableByteSequence(ByteSequence bs) {
    super(new byte[Math.max(64, bs.length())]);
    System.arraycopy(bs.getBackingArray(), bs.offset(), data, 0, bs.length());
    this.length = bs.length();
    this.offset = 0;
  }

  public void setArray(byte[] data, int offset, int len) {
    this.data = data;
    this.offset = offset;
    this.length = len;
  }

  public void setLength(int len) {
    this.length = len;
  }
}
