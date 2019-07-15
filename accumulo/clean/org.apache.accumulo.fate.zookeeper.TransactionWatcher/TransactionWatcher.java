import org.apache.accumulo.fate.zookeeper.*;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionWatcher {

  public interface Arbitrator {
    boolean transactionAlive(String type, long tid) throws Exception;

    boolean transactionComplete(String type, long tid) throws Exception;
  }

  private static final Logger log = LoggerFactory.getLogger(TransactionWatcher.class);
  final private Map<Long,AtomicInteger> counts = new HashMap<>();
  final private Arbitrator arbitrator;

  public TransactionWatcher(Arbitrator arbitrator) {
    this.arbitrator = arbitrator;
  }

  public <T> T run(String ztxBulk, long tid, Callable<T> callable) throws Exception {
    synchronized (counts) {
      if (!arbitrator.transactionAlive(ztxBulk, tid)) {
        throw new Exception("Transaction " + tid + " of type " + ztxBulk + " is no longer active");
      }
      AtomicInteger count = counts.get(tid);
      if (count == null)
        counts.put(tid, count = new AtomicInteger());
      count.incrementAndGet();
    }
    try {
      return callable.call();
    } finally {
      synchronized (counts) {
        AtomicInteger count = counts.get(tid);
        if (count == null) {
          log.error("unexpected missing count for transaction" + tid);
        } else {
          if (count.decrementAndGet() == 0)
            counts.remove(tid);
        }
      }
    }
  }

  public boolean isActive(long tid) {
    synchronized (counts) {
      log.debug("Transactions in progress " + counts);
      AtomicInteger count = counts.get(tid);
      return count != null && count.get() > 0;
    }
  }

}
