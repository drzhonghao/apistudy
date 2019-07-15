import org.apache.accumulo.core.util.*;


import java.util.Set;

import org.apache.accumulo.core.client.impl.ThriftTransportPool;
import org.apache.accumulo.fate.zookeeper.ZooSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class CleanUp {

  private static final Logger log = LoggerFactory.getLogger(CleanUp.class);

  /**
   * kills all threads created by internal Accumulo singleton resources. After this method is
   * called, no accumulo client will work in the current classloader.
   */
  public static void shutdownNow() {
    ThriftTransportPool.getInstance().shutdown();
    ZooSession.shutdown();
    waitForZooKeeperClientThreads();
  }

  /**
   * As documented in https://issues.apache.org/jira/browse/ZOOKEEPER-1816, ZooKeeper.close() is a
   * non-blocking call. This method will wait on the ZooKeeper internal threads to exit.
   */
  private static void waitForZooKeeperClientThreads() {
    Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
    for (Thread thread : threadSet) {
      // find ZooKeeper threads that were created in the same ClassLoader as the current thread.
      if (thread.getClass().getName().startsWith("org.apache.zookeeper.ClientCnxn") && thread
          .getContextClassLoader().equals(Thread.currentThread().getContextClassLoader())) {

        // wait for the thread the die
        while (thread.isAlive()) {
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            log.error("{}", e.getMessage(), e);
          }
        }
      }
    }
  }
}
