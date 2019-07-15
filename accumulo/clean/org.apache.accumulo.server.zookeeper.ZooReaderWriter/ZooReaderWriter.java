import org.apache.accumulo.server.zookeeper.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.conf.SiteConfiguration;

public class ZooReaderWriter extends org.apache.accumulo.fate.zookeeper.ZooReaderWriter {
  private static final String SCHEME = "digest";
  private static final String USER = "accumulo";
  private static ZooReaderWriter instance = null;

  public ZooReaderWriter(String string, int timeInMillis, String secret) {
    super(string, timeInMillis, SCHEME, (USER + ":" + secret).getBytes(UTF_8));
  }

  public static synchronized ZooReaderWriter getInstance() {
    if (instance == null) {
      AccumuloConfiguration conf = SiteConfiguration.getInstance();
      instance = new ZooReaderWriter(conf.get(Property.INSTANCE_ZK_HOST),
          (int) conf.getTimeInMillis(Property.INSTANCE_ZK_TIMEOUT),
          conf.get(Property.INSTANCE_SECRET));
    }
    return instance;
  }

}
