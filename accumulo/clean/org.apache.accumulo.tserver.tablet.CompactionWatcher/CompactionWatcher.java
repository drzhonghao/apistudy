import org.apache.accumulo.tserver.tablet.*;


import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.server.util.time.SimpleTimer;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class CompactionWatcher implements Runnable {
  private final Map<List<Long>,ObservedCompactionInfo> observedCompactions = new HashMap<>();
  private final AccumuloConfiguration config;
  private static boolean watching = false;

  private static class ObservedCompactionInfo {
    CompactionInfo compactionInfo;
    long firstSeen;
    boolean loggedWarning;

    ObservedCompactionInfo(CompactionInfo ci, long time) {
      this.compactionInfo = ci;
      this.firstSeen = time;
    }
  }

  public CompactionWatcher(AccumuloConfiguration config) {
    this.config = config;
  }

  @Override
  public void run() {
    List<CompactionInfo> runningCompactions = Compactor.getRunningCompactions();

    Set<List<Long>> newKeys = new HashSet<>();

    long time = System.currentTimeMillis();

    for (CompactionInfo ci : runningCompactions) {
      List<Long> compactionKey = Arrays.asList(ci.getID(), ci.getEntriesRead(),
          ci.getEntriesWritten());
      newKeys.add(compactionKey);

      if (!observedCompactions.containsKey(compactionKey)) {
        observedCompactions.put(compactionKey, new ObservedCompactionInfo(ci, time));
      }
    }

    // look for compactions that finished or made progress and logged a warning
    HashMap<List<Long>,ObservedCompactionInfo> copy = new HashMap<>(observedCompactions);
    copy.keySet().removeAll(newKeys);

    for (ObservedCompactionInfo oci : copy.values()) {
      if (oci.loggedWarning) {
        LoggerFactory.getLogger(CompactionWatcher.class)
            .info("Compaction of " + oci.compactionInfo.getExtent() + " is no longer stuck");
      }
    }

    // remove any compaction that completed or made progress
    observedCompactions.keySet().retainAll(newKeys);

    long warnTime = config.getTimeInMillis(Property.TSERV_COMPACTION_WARN_TIME);

    // check for stuck compactions
    for (ObservedCompactionInfo oci : observedCompactions.values()) {
      if (time - oci.firstSeen > warnTime && !oci.loggedWarning) {
        Thread compactionThread = oci.compactionInfo.getThread();
        if (compactionThread != null) {
          StackTraceElement[] trace = compactionThread.getStackTrace();
          Exception e = new Exception(
              "Possible stack trace of compaction stuck on " + oci.compactionInfo.getExtent());
          e.setStackTrace(trace);
          LoggerFactory.getLogger(CompactionWatcher.class)
              .warn("Compaction of " + oci.compactionInfo.getExtent() + " to "
                  + oci.compactionInfo.getOutputFile() + " has not made progress for at least "
                  + (time - oci.firstSeen) + "ms", e);
          oci.loggedWarning = true;
        }
      }
    }
  }

  public static synchronized void startWatching(AccumuloConfiguration config) {
    if (!watching) {
      SimpleTimer.getInstance(config).schedule(new CompactionWatcher(config), 10000, 10000);
      watching = true;
    }
  }

}
