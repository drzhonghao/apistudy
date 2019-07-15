import org.apache.accumulo.core.util.*;


import org.apache.accumulo.start.Main;
import org.apache.accumulo.start.spi.KeywordExecutable;

import com.google.auto.service.AutoService;

@AutoService(KeywordExecutable.class)
public class Classpath implements KeywordExecutable {
  @Override
  public String keyword() {
    return "classpath";
  }

  @Override
  public void execute(final String[] args) throws Exception {
    Main.getVFSClassLoader().getMethod("printClassPath").invoke(Main.getVFSClassLoader());
  }
}
