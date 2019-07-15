import org.apache.accumulo.server.util.*;


import static org.apache.accumulo.fate.util.UtilWaitThread.sleepUninterruptibly;

import java.util.concurrent.TimeUnit;

import org.apache.accumulo.core.util.Daemon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Halt {
  static private final Logger log = LoggerFactory.getLogger(Halt.class);

  public static void halt(final String msg) {
    // ACCUMULO-3651 Changed level to error and added FATAL to message for slf4j compatibility
    halt(0, new Runnable() {
      @Override
      public void run() {
        log.error("FATAL {}", msg);
      }
    });
  }

  public static void halt(final String msg, int status) {
    halt(status, new Runnable() {
      @Override
      public void run() {
        log.error("FATAL {}", msg);
      }
    });
  }

  public static void halt(final int status, Runnable runnable) {
    try {
      // give ourselves a little time to try and do something
      new Daemon() {
        @Override
        public void run() {
          sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
          Runtime.getRuntime().halt(status);
        }
      }.start();

      if (runnable != null)
        runnable.run();
      Runtime.getRuntime().halt(status);
    } finally {
      // In case something else decides to throw a Runtime exception
      Runtime.getRuntime().halt(-1);
    }
  }

}
