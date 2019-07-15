

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.monitor.servlets.BasicServlet;
import org.apache.accumulo.monitor.util.Table;
import org.apache.accumulo.monitor.util.celltypes.DurationType;
import org.apache.accumulo.monitor.util.celltypes.NumberType;
import org.apache.accumulo.monitor.util.celltypes.StringType;
import org.apache.accumulo.tracer.TraceFormatter;
import org.apache.accumulo.tracer.thrift.RemoteSpan;
import org.apache.hadoop.io.Text;


public class Summary {
	private static final long serialVersionUID = 1L;

	public static final int DEFAULT_MINUTES = 10;

	int getMinutes(HttpServletRequest req) {
		return 0;
	}

	public String getTitle(HttpServletRequest req) {
		return ("Traces for the last " + (getMinutes(req))) + " minutes";
	}

	private static class Stats {
		int count;

		long min = Long.MAX_VALUE;

		long max = Long.MIN_VALUE;

		long total = 0L;

		long[] histogram = new long[]{ 0, 0, 0, 0, 0, 0 };

		void addSpan(RemoteSpan span) {
			(count)++;
			long ms = (span.stop) - (span.start);
			total += ms;
			min = Math.min(min, ms);
			max = Math.max(max, ms);
			int index = 0;
			while ((ms >= 10) && (index < (histogram.length))) {
				ms /= 10;
				index++;
			} 
			(histogram[index])++;
		}

		long average() {
			return (total) / (count);
		}
	}

	private static class ShowTypeLink extends StringType<String> {
		private static final long serialVersionUID = 1L;

		int minutes;

		public ShowTypeLink(int minutes) {
			this.minutes = minutes;
		}

		@Override
		public String format(Object obj) {
			if (obj == null)
				return "-";

			String type = obj.toString();
			String encodedType = BasicServlet.encode(type);
			return String.format("<a href='/trace/listType?type=%s&minutes=%d'>%s</a>", encodedType, minutes, type);
		}
	}

	private static class HistogramType extends StringType<Summary.Stats> {
		private static final long serialVersionUID = 1L;

		@Override
		public String format(Object obj) {
			Summary.Stats stat = ((Summary.Stats) (obj));
			StringBuilder sb = new StringBuilder();
			sb.append("<table>");
			sb.append("<tr>");
			for (long count : stat.histogram) {
				if (count > 0)
					sb.append(String.format("<td style='width:5em'>%d</td>", count));
				else
					sb.append("<td style='width:5em'>-</td>");

			}
			sb.append("</tr></table>");
			return sb.toString();
		}

		@Override
		public int compare(Summary.Stats o1, Summary.Stats o2) {
			for (int i = 0; i < (o1.histogram.length); i++) {
				long diff = (o1.histogram[i]) - (o2.histogram[i]);
				if (diff < 0)
					return -1;

				if (diff > 0)
					return 1;

			}
			return 0;
		}
	}

	protected Range getRangeForTrace(long minutesSince) {
		long endTime = System.currentTimeMillis();
		long millisSince = (minutesSince * 60) * 1000;
		if (millisSince < minutesSince) {
			millisSince = endTime;
		}
		long startTime = endTime - millisSince;
		String startHexTime = Long.toHexString(startTime);
		String endHexTime = Long.toHexString(endTime);
		while ((startHexTime.length()) < (endHexTime.length())) {
			startHexTime = "0" + startHexTime;
		} 
		return new Range(new Text(("start:" + startHexTime)), new Text(("start:" + endHexTime)));
	}

	private void parseSpans(Scanner scanner, Map<String, Summary.Stats> summary) {
		for (Map.Entry<Key, Value> entry : scanner) {
			RemoteSpan span = TraceFormatter.getRemoteSpan(entry);
			Summary.Stats stats = summary.get(span.description);
			if (stats == null) {
				summary.put(span.description, (stats = new Summary.Stats()));
			}
			stats.addSpan(span);
		}
	}

	public void pageBody(HttpServletRequest req, HttpServletResponse resp, StringBuilder sb) throws Exception {
		int minutes = getMinutes(req);
		Range range = getRangeForTrace(minutes);
		final Map<String, Summary.Stats> summary = new TreeMap<>();
		Table trace = new Table("traceSummary", "All Traces");
		trace.addSortableColumn("Type", new Summary.ShowTypeLink(minutes), "Trace Type");
		trace.addSortableColumn("Total", new NumberType<Integer>(), "Number of spans of this type");
		trace.addSortableColumn("min", new DurationType(), "Shortest span duration");
		trace.addSortableColumn("max", new DurationType(), "Longest span duration");
		trace.addSortableColumn("avg", new DurationType(), "Average span duration");
		trace.addSortableColumn("Histogram", new Summary.HistogramType(), ("Counts of spans of different duration. Columns start at milliseconds," + (" and each column is ten times longer: tens of milliseconds, seconds," + " tens of seconds, etc.")));
		for (Map.Entry<String, Summary.Stats> entry : summary.entrySet()) {
			Summary.Stats stat = entry.getValue();
			trace.addRow(entry.getKey(), stat.count, stat.min, stat.max, stat.average(), stat);
		}
		trace.generate(req, sb);
	}
}

