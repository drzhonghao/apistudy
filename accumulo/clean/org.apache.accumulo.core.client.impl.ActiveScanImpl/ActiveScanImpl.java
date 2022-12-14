import org.apache.accumulo.core.client.impl.*;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.ActiveScan;
import org.apache.accumulo.core.client.admin.ScanState;
import org.apache.accumulo.core.client.admin.ScanType;
import org.apache.accumulo.core.data.Column;
import org.apache.accumulo.core.data.TabletId;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.data.impl.TabletIdImpl;
import org.apache.accumulo.core.data.thrift.IterInfo;
import org.apache.accumulo.core.data.thrift.TColumn;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.hadoop.io.Text;

/**
 * A class that contains information about an ActiveScan
 *
 * @since 1.6.0
 */
public class ActiveScanImpl extends ActiveScan {

  private long scanId;
  private String client;
  private String tableName;
  private long age;
  private long idle;
  private ScanType type;
  private ScanState state;
  private KeyExtent extent;
  private List<Column> columns;
  private List<String> ssiList;
  private Map<String,Map<String,String>> ssio;
  private String user;
  private Authorizations authorizations;

  ActiveScanImpl(Instance instance,
      org.apache.accumulo.core.tabletserver.thrift.ActiveScan activeScan)
      throws TableNotFoundException {
    this.scanId = activeScan.scanId;
    this.client = activeScan.client;
    this.user = activeScan.user;
    this.age = activeScan.age;
    this.idle = activeScan.idleTime;
    this.tableName = Tables.getTableName(instance, activeScan.tableId);
    this.type = ScanType.valueOf(activeScan.getType().name());
    this.state = ScanState.valueOf(activeScan.state.name());
    this.extent = new KeyExtent(activeScan.extent);
    this.authorizations = new Authorizations(activeScan.authorizations);

    this.columns = new ArrayList<>(activeScan.columns.size());

    for (TColumn tcolumn : activeScan.columns)
      this.columns.add(new Column(tcolumn));

    this.ssiList = new ArrayList<>();
    for (IterInfo ii : activeScan.ssiList) {
      this.ssiList.add(ii.iterName + "=" + ii.priority + "," + ii.className);
    }
    this.ssio = activeScan.ssio;
  }

  @Override
  public long getScanid() {
    return scanId;
  }

  @Override
  public String getClient() {
    return client;
  }

  @Override
  public String getUser() {
    return user;
  }

  @Override
  public String getTable() {
    return tableName;
  }

  @Override
  public long getAge() {
    return age;
  }

  @Override
  public long getLastContactTime() {
    return idle;
  }

  @Override
  public ScanType getType() {
    return type;
  }

  @Override
  public ScanState getState() {
    return state;
  }

  @Override
  @Deprecated
  public org.apache.accumulo.core.data.KeyExtent getExtent() {
    return new org.apache.accumulo.core.data.KeyExtent(new Text(extent.getTableId()),
        extent.getEndRow(), extent.getPrevEndRow());
  }

  @Override
  public TabletId getTablet() {
    return new TabletIdImpl(extent);
  }

  @Override
  public List<Column> getColumns() {
    return columns;
  }

  @Override
  public List<String> getSsiList() {
    return ssiList;
  }

  @Override
  public Map<String,Map<String,String>> getSsio() {
    return ssio;
  }

  @Override
  public Authorizations getAuthorizations() {
    return authorizations;
  }

  @Override
  public long getIdleTime() {
    return idle;
  }
}
