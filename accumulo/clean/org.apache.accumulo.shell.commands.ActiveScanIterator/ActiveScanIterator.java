import org.apache.accumulo.shell.commands.*;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.accumulo.core.client.admin.ActiveScan;
import org.apache.accumulo.core.client.admin.InstanceOperations;
import org.apache.accumulo.core.client.admin.ScanType;
import org.apache.accumulo.core.util.Duration;

class ActiveScanIterator implements Iterator<String> {

  private InstanceOperations instanceOps;
  private Iterator<String> tsIter;
  private Iterator<String> scansIter;

  private void readNext() {
    final List<String> scans = new ArrayList<>();

    while (tsIter.hasNext()) {

      final String tserver = tsIter.next();
      try {
        final List<ActiveScan> asl = instanceOps.getActiveScans(tserver);

        for (ActiveScan as : asl) {
          scans.add(String.format(
              "%21s |%21s |%9s |%9s |%7s |%6s |%8s |%8s |%10s |%20s |%10s |%20s |%10s | %s",
              tserver, as.getClient(), Duration.format(as.getAge(), "", "-"),
              Duration.format(as.getLastContactTime(), "", "-"), as.getState(), as.getType(),
              as.getUser(), as.getTable(), as.getColumns(), as.getAuthorizations(),
              (as.getType() == ScanType.SINGLE ? as.getTablet() : "N/A"), as.getScanid(),
              as.getSsiList(), as.getSsio()));
        }
      } catch (Exception e) {
        scans.add(tserver + " ERROR " + e.getMessage());
      }

      if (scans.size() > 0) {
        break;
      }
    }

    scansIter = scans.iterator();
  }

  ActiveScanIterator(List<String> tservers, InstanceOperations instanceOps) {
    this.instanceOps = instanceOps;
    this.tsIter = tservers.iterator();

    final String header = String.format(
        " %-21s| %-21s| %-9s| %-9s| %-7s| %-6s|"
            + " %-8s| %-8s| %-10s| %-20s| %-10s| %-10s | %-20s | %s",
        "TABLET SERVER", "CLIENT", "AGE", "LAST", "STATE", "TYPE", "USER", "TABLE", "COLUMNS",
        "AUTHORIZATIONS", "TABLET", "SCAN ID", "ITERATORS", "ITERATOR OPTIONS");

    scansIter = Collections.singletonList(header).iterator();
  }

  @Override
  public boolean hasNext() {
    return scansIter.hasNext();
  }

  @Override
  public String next() {
    final String next = scansIter.next();

    if (!scansIter.hasNext())
      readNext();

    return next;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

}
