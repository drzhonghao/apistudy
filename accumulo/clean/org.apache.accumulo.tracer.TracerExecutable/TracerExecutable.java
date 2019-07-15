import org.apache.accumulo.tracer.TraceServer;
import org.apache.accumulo.tracer.*;


import org.apache.accumulo.start.spi.KeywordExecutable;

import com.google.auto.service.AutoService;

@AutoService(KeywordExecutable.class)
public class TracerExecutable implements KeywordExecutable {

  @Override
  public String keyword() {
    return "tracer";
  }

  @Override
  public void execute(final String[] args) throws Exception {
    TraceServer.main(args);
  }

}
