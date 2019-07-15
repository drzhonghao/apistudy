

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.monitor.servlets.trace.ShowTraceLinkType;
import org.apache.accumulo.monitor.util.Table;
import org.apache.accumulo.monitor.util.celltypes.DurationType;
import org.apache.accumulo.monitor.util.celltypes.StringType;
import org.apache.accumulo.tracer.TraceFormatter;
import org.apache.accumulo.tracer.thrift.RemoteSpan;
import org.apache.hadoop.io.Text;


public class ListType {
	private static final long serialVersionUID = 1L;

	String getType(HttpServletRequest req) {
		return null;
	}

	int getMinutes(HttpServletRequest req) {
		return 0;
	}

	public void pageBody(HttpServletRequest req, HttpServletResponse resp, StringBuilder sb) throws Exception {
		final String type = getType(req);
		int minutes = getMinutes(req);
		long endTime = System.currentTimeMillis();
		long startTime = endTime - ((minutes * 60) * 1000);
		Range range = new Range(new Text(("start:" + (Long.toHexString(startTime)))), new Text(("start:" + (Long.toHexString(endTime)))));
		final Table trace = new Table("trace", ("Traces for " + (getType(req))));
		trace.addSortableColumn("Start", new ShowTraceLinkType(), "Start Time");
		trace.addSortableColumn("ms", new DurationType(), "Span time");
		trace.addUnsortableColumn("Source", new StringType<String>(), "Service and location");
		trace.generate(req, sb);
	}

	private void addRows(Scanner scanner, String type, Table trace) {
		for (Map.Entry<Key, Value> entry : scanner) {
			RemoteSpan span = TraceFormatter.getRemoteSpan(entry);
			if (span.description.equals(type)) {
				trace.addRow(span, Long.valueOf(((span.stop) - (span.start))), (((span.svc) + ":") + (span.sender)));
			}
		}
	}

	public String getTitle(HttpServletRequest req) {
		return ((("Traces for " + (getType(req))) + " for the last ") + (getMinutes(req))) + " minutes";
	}
}

