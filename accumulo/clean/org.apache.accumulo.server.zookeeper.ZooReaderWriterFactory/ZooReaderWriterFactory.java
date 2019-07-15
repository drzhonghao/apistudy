import org.apache.accumulo.server.zookeeper.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.conf.SiteConfiguration;
import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
import org.apache.accumulo.fate.zookeeper.ZooReaderWriter;

/**
 * A factory for {@link ZooReaderWriter} objects.
 */
public class ZooReaderWriterFactory {
  private static final String SCHEME = "digest";
  private static final String USER = "accumulo";
  private static IZooReaderWriter instance = null;

  /**
   * Gets a new reader/writer.
   *
   * @param string
   *          ZooKeeper connection string
   * @param timeInMillis
   *          session timeout in milliseconds
   * @param secret
   *          instance secret
   * @return reader/writer
   */
  public IZooReaderWriter getZooReaderWriter(String string, int timeInMillis, String secret) {
    return new ZooReaderWriter(string, timeInMillis, SCHEME, (USER + ":" + secret).getBytes(UTF_8));
  }

  /**
   * Gets a reader/writer, retrieving ZooKeeper information from the site configuration. The same
   * instance may be returned for multiple calls.
   *
   * @return reader/writer
   */
  public IZooReaderWriter getInstance() {
    synchronized (ZooReaderWriterFactory.class) {
      if (instance == null) {
        AccumuloConfiguration conf = SiteConfiguration.getInstance();
        instance = getZooReaderWriter(conf.get(Property.INSTANCE_ZK_HOST),
            (int) conf.getTimeInMillis(Property.INSTANCE_ZK_TIMEOUT),
            conf.get(Property.INSTANCE_SECRET));
      }
      return instance;
    }
  }
}
