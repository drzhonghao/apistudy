import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.*;


import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.ClientConfiguration.ClientProperty;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.ConnectorImpl;
import org.apache.accumulo.core.client.impl.Credentials;
import org.apache.accumulo.core.client.impl.InstanceOperationsImpl;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.DefaultConfiguration;
import org.apache.accumulo.core.metadata.RootTable;
import org.apache.accumulo.core.util.ByteBufferUtil;
import org.apache.accumulo.core.util.OpTimer;
import org.apache.accumulo.core.util.TextUtil;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.fate.zookeeper.ZooCache;
import org.apache.accumulo.fate.zookeeper.ZooCacheFactory;
import org.apache.commons.configuration.Configuration;
import org.apache.hadoop.io.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of instance that looks in zookeeper to find information needed to connect to an
 * instance of accumulo.
 *
 * <p>
 * The advantage of using zookeeper to obtain information about accumulo is that zookeeper is highly
 * available, very responsive, and supports caching.
 *
 * <p>
 * Because it is possible for multiple instances of accumulo to share a single set of zookeeper
 * servers, all constructors require an accumulo instance name.
 *
 * If you do not know the instance names then run accumulo
 * org.apache.accumulo.server.util.ListInstances on an accumulo server.
 *
 */

public class ZooKeeperInstance implements Instance {

  private static final Logger log = LoggerFactory.getLogger(ZooKeeperInstance.class);

  private String instanceId = null;
  private String instanceName = null;

  private final ZooCache zooCache;

  private final String zooKeepers;

  private final int zooKeepersSessionTimeOut;

  private AccumuloConfiguration conf;
  private ClientConfiguration clientConf;

  /**
   *
   * @param instanceName
   *          The name of specific accumulo instance. This is set at initialization time.
   * @param zooKeepers
   *          A comma separated list of zoo keeper server locations. Each location can contain an
   *          optional port, of the format host:port.
   */
  public ZooKeeperInstance(String instanceName, String zooKeepers) {
    this(ClientConfiguration.loadDefault().withInstance(instanceName).withZkHosts(zooKeepers));
  }

  /**
   *
   * @param instanceName
   *          The name of specific accumulo instance. This is set at initialization time.
   * @param zooKeepers
   *          A comma separated list of zoo keeper server locations. Each location can contain an
   *          optional port, of the format host:port.
   * @param sessionTimeout
   *          zoo keeper session time out in milliseconds.
   * @deprecated since 1.6.0; Use {@link #ZooKeeperInstance(ClientConfiguration)} instead.
   */
  @Deprecated
  public ZooKeeperInstance(String instanceName, String zooKeepers, int sessionTimeout) {
    this(ClientConfiguration.loadDefault().withInstance(instanceName).withZkHosts(zooKeepers)
        .withZkTimeout(sessionTimeout));
  }

  /**
   *
   * @param instanceId
   *          The UUID that identifies the accumulo instance you want to connect to.
   * @param zooKeepers
   *          A comma separated list of zoo keeper server locations. Each location can contain an
   *          optional port, of the format host:port.
   * @deprecated since 1.6.0; Use {@link #ZooKeeperInstance(ClientConfiguration)} instead.
   */
  @Deprecated
  public ZooKeeperInstance(UUID instanceId, String zooKeepers) {
    this(ClientConfiguration.loadDefault().withInstance(instanceId).withZkHosts(zooKeepers));
  }

  /**
   *
   * @param instanceId
   *          The UUID that identifies the accumulo instance you want to connect to.
   * @param zooKeepers
   *          A comma separated list of zoo keeper server locations. Each location can contain an
   *          optional port, of the format host:port.
   * @param sessionTimeout
   *          zoo keeper session time out in milliseconds.
   * @deprecated since 1.6.0; Use {@link #ZooKeeperInstance(ClientConfiguration)} instead.
   */
  @Deprecated
  public ZooKeeperInstance(UUID instanceId, String zooKeepers, int sessionTimeout) {
    this(ClientConfiguration.loadDefault().withInstance(instanceId).withZkHosts(zooKeepers)
        .withZkTimeout(sessionTimeout));
  }

  /**
   * @param config
   *          Client configuration for specifying connection options. See
   *          {@link ClientConfiguration} which extends Configuration with convenience methods
   *          specific to Accumulo.
   * @since 1.6.0
   * @deprecated since 1.9.0; will be removed in 2.0.0 to eliminate commons config leakage into
   *             Accumulo API; use {@link #ZooKeeperInstance(ClientConfiguration)} instead.
   */
  @Deprecated
  public ZooKeeperInstance(Configuration config) {
    this(config, new ZooCacheFactory());
  }

  ZooKeeperInstance(Configuration config, ZooCacheFactory zcf) {
    checkArgument(config != null, "config is null");
    if (config instanceof ClientConfiguration) {
      this.clientConf = (ClientConfiguration) config;
    } else {
      @SuppressWarnings("deprecation")
      ClientConfiguration cliConf = new ClientConfiguration(config);
      this.clientConf = cliConf;
    }
    this.instanceId = clientConf.get(ClientProperty.INSTANCE_ID);
    this.instanceName = clientConf.get(ClientProperty.INSTANCE_NAME);
    if ((instanceId == null) == (instanceName == null))
      throw new IllegalArgumentException(
          "Expected exactly one of instanceName and instanceId to be set");
    this.zooKeepers = clientConf.get(ClientProperty.INSTANCE_ZK_HOST);
    this.zooKeepersSessionTimeOut = (int) AccumuloConfiguration
        .getTimeInMillis(clientConf.get(ClientProperty.INSTANCE_ZK_TIMEOUT));
    zooCache = zcf.getZooCache(zooKeepers, zooKeepersSessionTimeOut);
    if (null != instanceName) {
      // Validates that the provided instanceName actually exists
      getInstanceID();
    }
  }

  /**
   * @param config
   *          Client configuration for specifying connection options. See
   *          {@link ClientConfiguration} which extends Configuration with convenience methods
   *          specific to Accumulo.
   * @since 1.9.0
   */
  public ZooKeeperInstance(ClientConfiguration config) {
    this(config, new ZooCacheFactory());
  }

  @Override
  public String getInstanceID() {
    if (instanceId == null) {
      // want the instance id to be stable for the life of this instance object,
      // so only get it once
      String instanceNamePath = Constants.ZROOT + Constants.ZINSTANCES + "/" + instanceName;
      byte[] iidb = zooCache.get(instanceNamePath);
      if (iidb == null) {
        throw new RuntimeException(
            "Instance name " + instanceName + " does not exist in zookeeper. "
                + "Run \"accumulo org.apache.accumulo.server.util.ListInstances\" to see a list.");
      }
      instanceId = new String(iidb, UTF_8);
    }

    if (zooCache.get(Constants.ZROOT + "/" + instanceId) == null) {
      if (instanceName == null)
        throw new RuntimeException("Instance id " + instanceId + " does not exist in zookeeper");
      throw new RuntimeException("Instance id " + instanceId + " pointed to by the name "
          + instanceName + " does not exist in zookeeper");
    }

    return instanceId;
  }

  @Override
  public List<String> getMasterLocations() {
    String masterLocPath = ZooUtil.getRoot(this) + Constants.ZMASTER_LOCK;

    OpTimer timer = null;

    if (log.isTraceEnabled()) {
      log.trace("tid={} Looking up master location in zookeeper.", Thread.currentThread().getId());
      timer = new OpTimer().start();
    }

    byte[] loc = ZooUtil.getLockData(zooCache, masterLocPath);

    if (timer != null) {
      timer.stop();
      log.trace("tid={} Found master at {} in {}", Thread.currentThread().getId(),
          (loc == null ? "null" : new String(loc, UTF_8)));
    }

    if (loc == null) {
      return Collections.emptyList();
    }

    return Collections.singletonList(new String(loc, UTF_8));
  }

  @Override
  public String getRootTabletLocation() {
    String zRootLocPath = ZooUtil.getRoot(this) + RootTable.ZROOT_TABLET_LOCATION;

    OpTimer timer = null;

    if (log.isTraceEnabled()) {
      log.trace("tid={} Looking up root tablet location in zookeeper.",
          Thread.currentThread().getId());
      timer = new OpTimer().start();
    }

    byte[] loc = zooCache.get(zRootLocPath);

    if (timer != null) {
      timer.stop();
      log.trace("tid={} Found root tablet at {} in {}", Thread.currentThread().getId(),
          (loc == null ? "null" : new String(loc, UTF_8)));
    }

    if (loc == null) {
      return null;
    }

    return new String(loc, UTF_8).split("\\|")[0];
  }

  @Override
  public String getInstanceName() {
    if (instanceName == null)
      instanceName = InstanceOperationsImpl.lookupInstanceName(zooCache,
          UUID.fromString(getInstanceID()));

    return instanceName;
  }

  @Override
  public String getZooKeepers() {
    return zooKeepers;
  }

  @Override
  public int getZooKeepersSessionTimeOut() {
    return zooKeepersSessionTimeOut;
  }

  @Override
  @Deprecated
  public Connector getConnector(String user, CharSequence pass)
      throws AccumuloException, AccumuloSecurityException {
    return getConnector(user, TextUtil.getBytes(new Text(pass.toString())));
  }

  @Override
  @Deprecated
  public Connector getConnector(String user, ByteBuffer pass)
      throws AccumuloException, AccumuloSecurityException {
    return getConnector(user, ByteBufferUtil.toBytes(pass));
  }

  @Override
  public Connector getConnector(String principal, AuthenticationToken token)
      throws AccumuloException, AccumuloSecurityException {
    return new ConnectorImpl(
        new ClientContext(this, new Credentials(principal, token), clientConf));
  }

  @Override
  @Deprecated
  public Connector getConnector(String principal, byte[] pass)
      throws AccumuloException, AccumuloSecurityException {
    return getConnector(principal, new PasswordToken(pass));
  }

  @Override
  @Deprecated
  public AccumuloConfiguration getConfiguration() {
    return conf = conf == null ? DefaultConfiguration.getInstance()
        : ClientContext.convertClientConfig(clientConf);
  }

  @Override
  @Deprecated
  public void setConfiguration(AccumuloConfiguration conf) {
    this.conf = conf;
  }

  /**
   * Given a zooCache and instanceId, look up the instance name.
   *
   * @deprecated since 1.7.0 {@link ZooCache} is not part of the public API, but its a parameter to
   *             this method. Therefore code that uses this method is not guaranteed to be stable.
   *             This method was deprecated to discourage its use.
   */
  @Deprecated
  public static String lookupInstanceName(ZooCache zooCache, UUID instanceId) {
    return InstanceOperationsImpl.lookupInstanceName(zooCache, instanceId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(64);
    sb.append("ZooKeeperInstance: ").append(getInstanceName()).append(" ").append(getZooKeepers());
    return sb.toString();
  }
}
