import org.apache.accumulo.core.cli.ClientOpts;
import org.apache.accumulo.server.cli.*;


import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.util.DeprecationUtil;
import org.apache.accumulo.server.client.HdfsZooInstance;

public class ClientOpts extends org.apache.accumulo.core.cli.ClientOpts {

  {
    setPrincipal("root");
  }

  @Override
  public Instance getInstance() {
    if (mock)
      return DeprecationUtil.makeMockInstance(instance);
    if (instance == null) {
      return HdfsZooInstance.getInstance();
    }
    return new ZooKeeperInstance(this.getClientConfiguration());
  }
}
