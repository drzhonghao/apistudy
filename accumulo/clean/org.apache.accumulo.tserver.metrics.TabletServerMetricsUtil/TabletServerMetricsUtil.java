import org.apache.accumulo.tserver.metrics.*;


import org.apache.accumulo.tserver.TabletServer;
import org.apache.accumulo.tserver.tablet.Tablet;

/**
 * Wrapper around extracting metrics from a TabletServer instance
 *
 * Necessary to support both old custom JMX metrics and Hadoop Metrics2
 */
public class TabletServerMetricsUtil {

  private final TabletServer tserver;

  public TabletServerMetricsUtil(TabletServer tserver) {
    this.tserver = tserver;
  }

  public long getEntries() {
    long result = 0;
    for (Tablet tablet : tserver.getOnlineTablets()) {
      result += tablet.getNumEntries();
    }
    return result;
  }

  public long getEntriesInMemory() {
    long result = 0;
    for (Tablet tablet : tserver.getOnlineTablets()) {
      result += tablet.getNumEntriesInMemory();
    }
    return result;
  }

  public double getIngest() {
    double result = 0;
    for (Tablet tablet : tserver.getOnlineTablets()) {
      result += tablet.ingestRate();
    }
    return result;
  }

  public double getIngestByteRate() {
    double result = 0;
    for (Tablet tablet : tserver.getOnlineTablets()) {
      result += tablet.ingestByteRate();
    }
    return result;
  }

  public double getQueryRate() {
    double result = 0;
    for (Tablet tablet : tserver.getOnlineTablets()) {
      result += tablet.queryRate();
    }
    return result;
  }

  public double getQueryByteRate() {
    double result = 0;
    for (Tablet tablet : tserver.getOnlineTablets()) {
      result += tablet.queryByteRate();
    }
    return result;
  }

  public double getScannedRate() {
    double result = 0;
    for (Tablet tablet : tserver.getOnlineTablets()) {
      result += tablet.scanRate();
    }
    return result;
  }

  public int getMajorCompactions() {
    int result = 0;
    for (Tablet tablet : tserver.getOnlineTablets()) {
      if (tablet.isMajorCompactionRunning())
        result++;
    }
    return result;
  }

  public int getMajorCompactionsQueued() {
    int result = 0;
    for (Tablet tablet : tserver.getOnlineTablets()) {
      if (tablet.isMajorCompactionQueued())
        result++;
    }
    return result;
  }

  public int getMinorCompactions() {
    int result = 0;
    for (Tablet tablet : tserver.getOnlineTablets()) {
      if (tablet.isMinorCompactionRunning())
        result++;
    }
    return result;
  }

  public int getMinorCompactionsQueued() {
    int result = 0;
    for (Tablet tablet : tserver.getOnlineTablets()) {
      if (tablet.isMinorCompactionQueued())
        result++;
    }
    return result;
  }

  public int getOnlineCount() {
    return tserver.getOnlineTablets().size();
  }

  public int getOpeningCount() {
    return tserver.getOpeningCount();
  }

  public long getQueries() {
    long result = 0;
    for (Tablet tablet : tserver.getOnlineTablets()) {
      result += tablet.totalQueries();
    }
    return result;
  }

  public int getUnopenedCount() {
    return tserver.getUnopenedCount();
  }

  public String getName() {
    return tserver.getClientAddressString();
  }

  public long getTotalMinorCompactions() {
    return tserver.getTotalMinorCompactions();
  }

  public double getHoldTime() {
    return tserver.getHoldTimeMillis() / 1000.;
  }

  public double getAverageFilesPerTablet() {
    int count = 0;
    long result = 0;
    for (Tablet tablet : tserver.getOnlineTablets()) {
      result += tablet.getDatafiles().size();
      count++;
    }
    if (count == 0)
      return 0;
    return result / (double) count;
  }
}
