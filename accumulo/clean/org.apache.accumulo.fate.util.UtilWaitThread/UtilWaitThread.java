import org.apache.accumulo.fate.util.*;


import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UtilWaitThread {
  private static final Logger log = LoggerFactory.getLogger(UtilWaitThread.class);

  public static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      log.error("{}", e.getMessage(), e);
    }
  }

  /**
   * Copied from Guava release 23. The Uniterruptibles class was annotated as Beta by Google,
   * therefore unstable to use. The following javadoc was copied from
   * com.google.common.util.concurrent.Uninterruptibles:
   *
   * Utilities for treating interruptible operations as uninterruptible. In all cases, if a thread
   * is interrupted during such a call, the call continues to block until the result is available or
   * the timeout elapses, and only then re-interrupts the thread.
   *
   */
  public static void sleepUninterruptibly(long sleepFor, TimeUnit unit) {
    boolean interrupted = false;
    try {
      long remainingNanos = unit.toNanos(sleepFor);
      long end = System.nanoTime() + remainingNanos;
      while (true) {
        try {
          // TimeUnit.sleep() treats negative timeouts just like zero.
          NANOSECONDS.sleep(remainingNanos);
          return;
        } catch (InterruptedException e) {
          interrupted = true;
          remainingNanos = end - System.nanoTime();
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
