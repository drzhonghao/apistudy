import org.apache.accumulo.minicluster.impl.*;


import org.apache.accumulo.minicluster.MiniAccumuloRunner;
import org.apache.accumulo.start.spi.KeywordExecutable;

import com.google.auto.service.AutoService;

@AutoService(KeywordExecutable.class)
public class MiniClusterExecutable implements KeywordExecutable {

  @Override
  public String keyword() {
    return "minicluster";
  }

  @Override
  public void execute(final String[] args) throws Exception {
    MiniAccumuloRunner.main(args);
  }

}
