import org.apache.accumulo.server.replication.*;


import static java.util.Objects.requireNonNull;

import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

/**
 *
 */
public class ReplicaSystemFactory {
  private static final Logger log = LoggerFactory.getLogger(ReplicaSystemFactory.class);

  /**
   * @param value
   *          {@link ReplicaSystem} implementation class name
   * @return A {@link ReplicaSystem} object from the given name
   */
  public ReplicaSystem get(String value) {
    final Entry<String,String> entry = parseReplicaSystemConfiguration(value);

    try {
      Class<?> clz = Class.forName(entry.getKey());

      if (ReplicaSystem.class.isAssignableFrom(clz)) {
        Object o = clz.newInstance();
        ReplicaSystem rs = (ReplicaSystem) o;
        rs.configure(entry.getValue());
        return rs;
      }

      throw new IllegalArgumentException(
          "Class is not assignable to ReplicaSystem: " + entry.getKey());
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      log.error("Error creating ReplicaSystem object", e);
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Parse the configuration value for a peer into its components: {@link ReplicaSystem} class name
   * and configuration string.
   *
   * @param value
   *          The configuration value for a replication peer.
   * @return An entry where the set is the replica system name and the value is the configuration
   *         string.
   */
  public Entry<String,String> parseReplicaSystemConfiguration(String value) {
    requireNonNull(value);

    int index = value.indexOf(',');
    if (-1 == index) {
      throw new IllegalArgumentException(
          "Expected comma separator between replication system name and configuration");
    }

    String name = value.substring(0, index);
    String configuration = value.substring(index + 1);
    return Maps.immutableEntry(name, configuration);
  }

  /**
   * Generate the configuration value for a {@link ReplicaSystem} in the instance properties
   *
   * @param system
   *          The desired ReplicaSystem to use
   * @param configuration
   *          Configuration string for the desired ReplicaSystem
   * @return Value to set for peer configuration in the instance
   */
  public static String getPeerConfigurationValue(Class<? extends ReplicaSystem> system,
      String configuration) {
    String systemName = system.getName() + ",";
    if (null == configuration) {
      return systemName;
    }

    return systemName + configuration;
  }
}
