import org.apache.accumulo.monitor.servlets.*;


import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.accumulo.core.master.thrift.TabletServerStatus;
import org.apache.accumulo.core.util.HostAndPort;
import org.apache.accumulo.monitor.Monitor;
import org.apache.accumulo.monitor.Monitor.ScanStats;
import org.apache.accumulo.monitor.util.Table;
import org.apache.accumulo.monitor.util.TableRow;
import org.apache.accumulo.monitor.util.celltypes.DurationType;
import org.apache.accumulo.monitor.util.celltypes.PreciseNumberType;
import org.apache.accumulo.monitor.util.celltypes.TServerLinkType;

public class ScanServlet extends BasicServlet {

  private static final long serialVersionUID = 1L;

  @Override
  protected String getTitle(HttpServletRequest req) {
    return "Scans";
  }

  @Override
  protected void pageBody(HttpServletRequest req, HttpServletResponse response, StringBuilder sb)
      throws IOException {
    Map<HostAndPort,ScanStats> scans = Monitor.getScans();
    Table scanTable = new Table("scanStatus", "Scan&nbsp;Status");
    scanTable.addSortableColumn("Server", new TServerLinkType(), null);
    scanTable.addSortableColumn("#", new PreciseNumberType(0, 20, 0, 100),
        "Number of scans presently running");
    scanTable.addSortableColumn("Oldest&nbsp;Age", new DurationType(0l, 5 * 60 * 1000l),
        "The age of the oldest scan on this server.");
    for (TabletServerStatus tserverInfo : Monitor.getMmi().getTServerInfo()) {
      ScanStats stats = scans.get(HostAndPort.fromString(tserverInfo.name));
      if (stats != null) {
        TableRow row = scanTable.prepareRow();
        row.add(tserverInfo);
        row.add(stats.scanCount);
        row.add(stats.oldestScan);
        scanTable.addRow(row);
      }
    }
    scanTable.generate(req, sb);
  }

}
