import org.apache.accumulo.monitor.servlets.trace.*;


import java.util.Date;

import org.apache.accumulo.monitor.util.celltypes.StringType;
import org.apache.accumulo.tracer.TraceFormatter;
import org.apache.accumulo.tracer.thrift.RemoteSpan;

/**
 *
 */
public class ShowTraceLinkType extends StringType<RemoteSpan> {
  private static final long serialVersionUID = 1L;

  @Override
  public String format(Object obj) {
    if (obj == null)
      return "-";
    RemoteSpan span = (RemoteSpan) obj;
    return String.format("<a href='/trace/show?id=%s'>%s</a>", Long.toHexString(span.traceId),
        TraceFormatter.formatDate(new Date(span.start)));
  }

  @Override
  public int compare(RemoteSpan o1, RemoteSpan o2) {
    if (o1 == null && o2 == null)
      return 0;
    else if (o1 == null)
      return -1;
    else if (o2 == null)
      return 1;
    return o1.start < o2.start ? -1 : (o1.start == o2.start ? 0 : 1);
  }
}
