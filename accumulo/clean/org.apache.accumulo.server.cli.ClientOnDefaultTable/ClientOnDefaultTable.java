import org.apache.accumulo.server.cli.*;


import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.util.DeprecationUtil;
import org.apache.accumulo.server.client.HdfsZooInstance;

public class ClientOnDefaultTable extends org.apache.accumulo.core.cli.ClientOnDefaultTable {
  {
    setPrincipal("root");
  }

  @Override
  synchronized public Instance getInstance() {
    if (cachedInstance != null)
      return cachedInstance;

    if (mock)
      return cachedInstance = DeprecationUtil.makeMockInstance(instance);
    if (instance == null) {
      return cachedInstance = HdfsZooInstance.getInstance();
    }
    return cachedInstance = new ZooKeeperInstance(this.getClientConfiguration());
  }

  public ClientOnDefaultTable(String table) {
    super(table);
  }
}
