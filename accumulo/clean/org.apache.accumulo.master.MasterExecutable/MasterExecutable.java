import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.*;


import org.apache.accumulo.start.spi.KeywordExecutable;

import com.google.auto.service.AutoService;

@AutoService(KeywordExecutable.class)
public class MasterExecutable implements KeywordExecutable {

  @Override
  public String keyword() {
    return "master";
  }

  @Override
  public void execute(final String[] args) throws Exception {
    Master.main(args);
  }

}
