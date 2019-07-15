import org.apache.accumulo.server.conf.ServerConfigurationFactory;
import org.apache.accumulo.server.conf.*;


import org.apache.accumulo.server.client.HdfsZooInstance;
import org.apache.accumulo.start.spi.KeywordExecutable;

import com.google.auto.service.AutoService;

@AutoService(KeywordExecutable.class)
public class ConfigSanityCheck implements KeywordExecutable {

  public static void main(String[] args) {
    new ServerConfigurationFactory(HdfsZooInstance.getInstance()).getConfiguration();
  }

  @Override
  public String keyword() {
    return "check-server-config";
  }

  @Override
  public void execute(String[] args) throws Exception {
    main(args);
  }

}
