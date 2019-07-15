import org.apache.accumulo.monitor.util.celltypes.*;


import java.util.Map;

import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.monitor.servlets.BasicServlet;
import org.apache.accumulo.server.client.HdfsZooInstance;

public class TableLinkType extends CellType<String> {

  private static final long serialVersionUID = 1L;
  private Map<String,String> tidToNameMap;

  public TableLinkType() {
    tidToNameMap = Tables.getIdToNameMap(HdfsZooInstance.getInstance());
  }

  @Override
  public String format(Object obj) {
    if (obj == null)
      return "-";
    String tableId = (String) obj;
    // Encode the tableid we use in the link so we construct a correct url
    // e.g. the root table's id of "+r" would not be interpreted properly
    return String.format("<a href='/tables?t=%s'>%s</a>", BasicServlet.encode(tableId),
        displayName(tableId));
  }

  private String displayName(String tableId) {
    if (tableId == null)
      return "-";
    return Tables.getPrintableTableNameFromId(tidToNameMap, tableId);
  }

  @Override
  public int compare(String o1, String o2) {
    return displayName(o1).compareTo(displayName(o2));
  }

  @Override
  public String alignment() {
    return "left";
  }

}
