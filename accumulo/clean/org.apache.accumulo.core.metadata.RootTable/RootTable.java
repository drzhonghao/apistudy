import org.apache.accumulo.core.metadata.*;


import org.apache.accumulo.core.client.impl.Namespaces;
import org.apache.accumulo.core.data.impl.KeyExtent;

/**
 *
 */
public class RootTable {

  public static final String ID = "+r";
  public static final String NAME = Namespaces.ACCUMULO_NAMESPACE + ".root";

  /**
   * DFS location relative to the Accumulo directory
   */
  public static final String ROOT_TABLET_LOCATION = "/root_tablet";

  /**
   * ZK path relative to the instance directory for information about the root tablet
   */
  public static final String ZROOT_TABLET = ROOT_TABLET_LOCATION;
  public static final String ZROOT_TABLET_LOCATION = ZROOT_TABLET + "/location";
  public static final String ZROOT_TABLET_FUTURE_LOCATION = ZROOT_TABLET + "/future_location";
  public static final String ZROOT_TABLET_LAST_LOCATION = ZROOT_TABLET + "/lastlocation";
  public static final String ZROOT_TABLET_WALOGS = ZROOT_TABLET + "/walogs";
  public static final String ZROOT_TABLET_CURRENT_LOGS = ZROOT_TABLET + "/current_logs";
  public static final String ZROOT_TABLET_PATH = ZROOT_TABLET + "/dir";

  public static final KeyExtent EXTENT = new KeyExtent(ID, null, null);
  public static final KeyExtent OLD_EXTENT = new KeyExtent(MetadataTable.ID,
      KeyExtent.getMetadataEntry(MetadataTable.ID, null), null);

}
