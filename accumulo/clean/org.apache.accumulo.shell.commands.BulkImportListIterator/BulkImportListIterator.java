import org.apache.accumulo.shell.commands.*;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.accumulo.core.master.thrift.BulkImportStatus;
import org.apache.accumulo.core.master.thrift.MasterMonitorInfo;
import org.apache.accumulo.core.master.thrift.TabletServerStatus;
import org.apache.accumulo.core.util.Duration;

public class BulkImportListIterator implements Iterator<String> {

  private final Iterator<String> iter;

  public BulkImportListIterator(List<String> tservers, MasterMonitorInfo stats) {
    List<String> result = new ArrayList<>();
    for (BulkImportStatus status : stats.bulkImports) {
      result.add(format(status));
    }
    if (!tservers.isEmpty()) {
      for (TabletServerStatus tserver : stats.tServerInfo) {
        if (tservers.contains(tserver.name)) {
          result.add(tserver.name + ":");
          for (BulkImportStatus status : tserver.bulkImports) {
            result.add(format(status));
          }
        }
      }
    }
    iter = result.iterator();
  }

  private String format(BulkImportStatus status) {
    long diff = System.currentTimeMillis() - status.startTime;
    return String.format("%25s | %4s | %s", status.filename, Duration.format(diff, " ", "-"),
        status.state);
  }

  @Override
  public boolean hasNext() {
    return iter.hasNext();
  }

  @Override
  public String next() {
    return iter.next();
  }

  @Override
  public void remove() {
    iter.remove();
  }

}
