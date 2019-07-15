import org.apache.accumulo.core.util.Daemon;
import org.apache.accumulo.core.util.*;


import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.accumulo.fate.util.LoggingRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamingThreadFactory implements ThreadFactory {
  private static final Logger log = LoggerFactory.getLogger(NamingThreadFactory.class);

  private AtomicInteger threadNum = new AtomicInteger(1);
  private String name;

  public NamingThreadFactory(String name) {
    this.name = name;
  }

  @Override
  public Thread newThread(Runnable r) {
    return new Daemon(new LoggingRunnable(log, r), name + " " + threadNum.getAndIncrement());
  }

}
