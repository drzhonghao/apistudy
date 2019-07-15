import org.apache.accumulo.core.replication.*;


import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.data.impl.KeyExtent;

/**
 * Encapsulates configuration semantics around replication
 */
public class ReplicationConfigurationUtil {

  /**
   * Determines if the replication is enabled for the given {@link KeyExtent}
   *
   * @param extent
   *          The {@link KeyExtent} for the Tablet in question
   * @param conf
   *          The {@link AccumuloConfiguration} for that Tablet (table or namespace)
   * @return True if this extent is a candidate for replication at the given point in time.
   */
  public static boolean isEnabled(KeyExtent extent, AccumuloConfiguration conf) {
    if (extent.isMeta() || extent.isRootTablet()) {
      return false;
    }

    return conf.getBoolean(Property.TABLE_REPLICATION);
  }

}
