import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.proxy.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Random;

import org.apache.accumulo.proxy.thrift.IteratorSetting;
import org.apache.accumulo.proxy.thrift.Key;

public class Util {

  private static Random random = new Random(0);

  public static String randString(int numbytes) {
    return new BigInteger(numbytes * 5, random).toString(32);
  }

  public static ByteBuffer randStringBuffer(int numbytes) {
    return ByteBuffer.wrap(new BigInteger(numbytes * 5, random).toString(32).getBytes(UTF_8));
  }

  public static IteratorSetting iteratorSetting2ProxyIteratorSetting(
      org.apache.accumulo.core.client.IteratorSetting is) {
    return new IteratorSetting(is.getPriority(), is.getName(), is.getIteratorClass(),
        is.getOptions());
  }

  public static Key toThrift(org.apache.accumulo.core.data.Key key) {
    Key pkey = new Key(ByteBuffer.wrap(key.getRow().getBytes()),
        ByteBuffer.wrap(key.getColumnFamily().getBytes()),
        ByteBuffer.wrap(key.getColumnQualifier().getBytes()),
        ByteBuffer.wrap(key.getColumnVisibility().getBytes()));
    pkey.setTimestamp(key.getTimestamp());
    return pkey;
  }

  public static org.apache.accumulo.core.data.Key fromThrift(Key pkey) {
    if (pkey == null)
      return null;
    return new org.apache.accumulo.core.data.Key(deNullify(pkey.getRow()),
        deNullify(pkey.getColFamily()), deNullify(pkey.getColQualifier()),
        deNullify(pkey.getColVisibility()), pkey.getTimestamp());
  }

  protected static byte[] deNullify(byte[] in) {
    if (in == null)
      return new byte[0];
    else
      return in;
  }
}
