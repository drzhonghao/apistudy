import org.apache.accumulo.core.util.*;


import org.apache.accumulo.start.Main;
import org.apache.accumulo.start.spi.KeywordExecutable;

import com.google.auto.service.AutoService;

@AutoService(KeywordExecutable.class)
public class Version implements KeywordExecutable {

  @Override
  public String keyword() {
    return "version";
  }

  @Override
  public void execute(final String[] args) throws Exception {
    Class<?> runTMP = Main.getClassLoader().loadClass("org.apache.accumulo.core.Constants");
    System.out.println(runTMP.getField("VERSION").get(null));
  }

}
