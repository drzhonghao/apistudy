import org.apache.accumulo.core.iterators.aggregation.*;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.accumulo.core.data.Value;
import org.apache.hadoop.io.WritableUtils;

/**
 * @deprecated since 1.4, replaced by
 *             {@link org.apache.accumulo.core.iterators.user.SummingCombiner} with
 *             {@link org.apache.accumulo.core.iterators.LongCombiner.Type#VARLEN}
 */
@Deprecated
public class NumSummation implements Aggregator {
  long sum = 0l;

  @Override
  public Value aggregate() {
    try {
      return new Value(NumSummation.longToBytes(sum));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void collect(Value value) {
    long l;
    try {
      l = NumSummation.bytesToLong(value.get());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    sum = NumSummation.safeAdd(sum, l);
  }

  public static byte[] longToBytes(long l) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);

    WritableUtils.writeVLong(dos, l);

    return baos.toByteArray();
  }

  public static long bytesToLong(byte[] b) throws IOException {
    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(b));
    return WritableUtils.readVLong(dis);
  }

  public static long safeAdd(long a, long b) {
    long aSign = Long.signum(a);
    long bSign = Long.signum(b);
    if ((aSign != 0) && (bSign != 0) && (aSign == bSign)) {
      if (aSign > 0) {
        if (Long.MAX_VALUE - a < b)
          return Long.MAX_VALUE;
      } else {
        if (Long.MIN_VALUE - a > b)
          return Long.MIN_VALUE;
      }
    }
    return a + b;
  }

  @Override
  public void reset() {
    sum = 0l;
  }

}
