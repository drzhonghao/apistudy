import org.apache.accumulo.monitor.Monitor;
import org.apache.accumulo.monitor.*;


import org.apache.accumulo.start.spi.KeywordExecutable;

import com.google.auto.service.AutoService;

@AutoService(KeywordExecutable.class)
public class MonitorExecutable implements KeywordExecutable {

  @Override
  public String keyword() {
    return "monitor";
  }

  @Override
  public void execute(final String[] args) throws Exception {
    Monitor.main(args);
  }

}
