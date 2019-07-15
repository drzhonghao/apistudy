import org.apache.accumulo.tserver.tablet.*;


import java.util.HashSet;
import java.util.Set;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.server.zookeeper.TransactionWatcher.ZooArbitrator;
import org.apache.accumulo.tserver.TabletServer;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BulkImportCacheCleaner implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(BulkImportCacheCleaner.class);
  private final TabletServer server;

  public BulkImportCacheCleaner(TabletServer server) {
    this.server = server;
  }

  @Override
  public void run() {
    // gather the list of transactions the tablets have cached
    final Set<Long> tids = new HashSet<>();
    for (Tablet tablet : server.getOnlineTablets()) {
      tids.addAll(tablet.getBulkIngestedFiles().keySet());
    }
    try {
      // get the current transactions from ZooKeeper
      final Set<Long> allTransactionsAlive = ZooArbitrator
          .allTransactionsAlive(Constants.BULK_ARBITRATOR_TYPE);
      // remove any that are still alive
      tids.removeAll(allTransactionsAlive);
      // cleanup any memory of these transactions
      for (Tablet tablet : server.getOnlineTablets()) {
        tablet.cleanupBulkLoadedFiles(tids);
      }
    } catch (KeeperException | InterruptedException e) {
      // we'll just clean it up again later
      log.debug("Error reading bulk import live transactions {}", e.toString());
    }
  }

}
