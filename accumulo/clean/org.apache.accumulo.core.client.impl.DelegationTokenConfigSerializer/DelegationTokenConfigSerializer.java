import org.apache.accumulo.core.client.impl.*;


import java.util.concurrent.TimeUnit;

import org.apache.accumulo.core.client.admin.DelegationTokenConfig;
import org.apache.accumulo.core.security.thrift.TDelegationTokenConfig;

/**
 * Handles serialization of {@link DelegationTokenConfig}
 */
public class DelegationTokenConfigSerializer {

  /**
   * Serialize the delegation token config into the thrift variant
   *
   * @param config
   *          The configuration
   */
  public static TDelegationTokenConfig serialize(DelegationTokenConfig config) {
    TDelegationTokenConfig tconfig = new TDelegationTokenConfig();
    tconfig.setLifetime(config.getTokenLifetime(TimeUnit.MILLISECONDS));
    return tconfig;
  }

  /**
   * Deserialize the Thrift delegation token config into the non-thrift variant
   *
   * @param tconfig
   *          The thrift configuration
   */
  public static DelegationTokenConfig deserialize(TDelegationTokenConfig tconfig) {
    DelegationTokenConfig config = new DelegationTokenConfig();
    if (tconfig.isSetLifetime()) {
      config.setTokenLifetime(tconfig.getLifetime(), TimeUnit.MILLISECONDS);
    }
    return config;
  }
}
