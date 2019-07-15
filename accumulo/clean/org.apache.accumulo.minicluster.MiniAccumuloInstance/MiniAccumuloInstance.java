import org.apache.accumulo.minicluster.*;


import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;

import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.conf.Property;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.hadoop.conf.Configuration;

/**
 *
 * @since 1.6.0
 */
public class MiniAccumuloInstance extends ZooKeeperInstance {

  /**
   * Construct an {@link Instance} entry point to Accumulo using a {@link MiniAccumuloCluster}
   * directory
   */
  public MiniAccumuloInstance(String instanceName, File directory) throws FileNotFoundException {
    super(ClientConfiguration.fromFile(new File(new File(directory, "conf"), "client.conf"))
        .withInstance(instanceName).withZkHosts(getZooKeepersFromDir(directory)));
  }

  /**
   * @deprecated since 1.9.0; will be removed in 2.0.0 to eliminate commons config leakage into
   *             Accumulo API
   */
  @Deprecated
  public static PropertiesConfiguration getConfigProperties(File directory) {
    try {
      PropertiesConfiguration conf = new PropertiesConfiguration();
      conf.setListDelimiter('\0');
      conf.load(new File(new File(directory, "conf"), "client.conf"));
      return conf;
    } catch (ConfigurationException e) {
      // this should never happen since we wrote the config file ourselves
      throw new IllegalArgumentException(e);
    }
  }

  // Keep this private to avoid bringing it into the public API
  private static String getZooKeepersFromDir(File directory) throws FileNotFoundException {
    if (!directory.isDirectory())
      throw new IllegalArgumentException("Not a directory " + directory.getPath());
    File configFile = new File(new File(directory, "conf"), "accumulo-site.xml");
    Configuration conf = new Configuration(false);
    try {
      conf.addResource(configFile.toURI().toURL());
    } catch (MalformedURLException e) {
      throw new FileNotFoundException("Missing file: " + configFile.getPath());
    }
    return conf.get(Property.INSTANCE_ZK_HOST.getKey());
  }
}
