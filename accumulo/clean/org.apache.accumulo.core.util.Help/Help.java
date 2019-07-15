import org.apache.accumulo.core.util.*;


import org.apache.accumulo.start.Main;
import org.apache.accumulo.start.spi.KeywordExecutable;

import com.google.auto.service.AutoService;

@AutoService(KeywordExecutable.class)
public class Help implements KeywordExecutable {
  @Override
  public String keyword() {
    return "help";
  }

  @Override
  public void execute(final String[] args) throws Exception {
    Main.printUsage();
  }
}
