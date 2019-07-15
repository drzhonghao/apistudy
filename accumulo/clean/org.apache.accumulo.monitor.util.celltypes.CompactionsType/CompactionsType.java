import org.apache.accumulo.monitor.util.celltypes.CellType;
import org.apache.accumulo.monitor.util.celltypes.NumberType;
import org.apache.accumulo.monitor.util.celltypes.*;


import org.apache.accumulo.core.master.thrift.Compacting;
import org.apache.accumulo.core.master.thrift.TableInfo;

public class CompactionsType extends CellType<TableInfo> {

  private static final long serialVersionUID = 1L;
  private String fieldName;

  public CompactionsType(String which) {
    this.fieldName = which;
  }

  @Override
  public String format(Object obj) {
    if (obj == null)
      return "-";
    TableInfo summary = (TableInfo) obj;
    Compacting c = summary.majors;
    if (fieldName.equals("minor"))
      c = summary.minors;
    else if (fieldName.equals("scans"))
      c = summary.scans;
    if (c == null)
      c = new Compacting();
    return String.format("%s&nbsp;(%,d)",
        NumberType.commas(c.running, c.queued == 0 ? 0 : 1, summary.onlineTablets), c.queued);
  }

  @Override
  public int compare(TableInfo o1, TableInfo o2) {
    if (o1 == null)
      return -1;
    if (o2 == null)
      return 1;
    Compacting c1 = o1.majors;
    Compacting c2 = o2.majors;
    if (fieldName.equals("minor")) {
      c1 = o1.minors;
      c2 = o2.minors;
    } else if (fieldName.equals("scans")) {
      c1 = o1.scans;
      c2 = o2.scans;
    }
    if (c1 == null)
      return -1;
    if (c2 == null)
      return 1;
    return c1.running + c1.queued - c2.running - c2.queued;
  }

  @Override
  public String alignment() {
    return "right";
  }

}
