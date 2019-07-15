import org.apache.accumulo.gc.SimpleGarbageCollector;
import org.apache.accumulo.gc.*;


import java.io.IOException;

import org.apache.accumulo.start.spi.KeywordExecutable;

import com.google.auto.service.AutoService;

@AutoService(KeywordExecutable.class)
public class GCExecutable implements KeywordExecutable {
  @Override
  public String keyword() {
    return "gc";
  }

  @Override
  public void execute(final String[] args) throws IOException {
    SimpleGarbageCollector.main(args);
  }
}
