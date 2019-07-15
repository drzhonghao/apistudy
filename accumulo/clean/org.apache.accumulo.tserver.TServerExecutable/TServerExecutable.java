import org.apache.accumulo.tserver.TabletServer;
import org.apache.accumulo.tserver.*;


import org.apache.accumulo.start.spi.KeywordExecutable;

import com.google.auto.service.AutoService;

@AutoService(KeywordExecutable.class)
public class TServerExecutable implements KeywordExecutable {

  @Override
  public String keyword() {
    return "tserver";
  }

  @Override
  public void execute(final String[] args) throws Exception {
    TabletServer.main(args);
  }

}
