import org.apache.accumulo.core.metadata.*;


import java.util.SortedMap;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.data.impl.KeyExtent;

/**
 * A metadata servicer for the root table.<br>
 * The root table's metadata is serviced in zookeeper.
 */
class ServicerForRootTable extends MetadataServicer {

  private final Instance instance;

  public ServicerForRootTable(ClientContext context) {
    this.instance = context.getInstance();
  }

  @Override
  public String getServicedTableId() {
    return RootTable.ID;
  }

  @Override
  public void getTabletLocations(SortedMap<KeyExtent,String> tablets)
      throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
    tablets.put(RootTable.EXTENT, instance.getRootTabletLocation());
  }
}
