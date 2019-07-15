import org.apache.accumulo.tracer.*;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.util.format.DateFormatSupplier;
import org.apache.accumulo.core.util.format.DefaultFormatter;
import org.apache.accumulo.core.util.format.Formatter;
import org.apache.accumulo.core.util.format.FormatterConfig;
import org.apache.accumulo.tracer.thrift.Annotation;
import org.apache.accumulo.tracer.thrift.RemoteSpan;
import org.apache.commons.lang.NotImplementedException;
import org.apache.hadoop.io.Text;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.transport.TMemoryInputTransport;

/**
 * A formatter than can be used in the shell to display trace information.
 *
 */
public class TraceFormatter implements Formatter {
  public static final String DATE_FORMAT = DateFormatSupplier.HUMAN_READABLE_FORMAT;
  // ugh... SimpleDataFormat is not thread safe
  private static final DateFormatSupplier formatter = DateFormatSupplier
      .createSimpleFormatSupplier(DATE_FORMAT);

  public static String formatDate(final Date date) {
    return formatter.get().format(date);
  }

  private final static Text SPAN_CF = new Text("span");

  private Iterator<Entry<Key,Value>> scanner;
  private FormatterConfig config;

  public static RemoteSpan getRemoteSpan(Entry<Key,Value> entry) {
    TMemoryInputTransport transport = new TMemoryInputTransport(entry.getValue().get());
    TCompactProtocol protocol = new TCompactProtocol(transport);
    RemoteSpan span = new RemoteSpan();
    try {
      span.read(protocol);
    } catch (TException ex) {
      throw new RuntimeException(ex);
    }
    return span;
  }

  @Override
  public boolean hasNext() {
    return scanner.hasNext();
  }

  @Override
  public String next() {
    Entry<Key,Value> next = scanner.next();
    if (next.getKey().getColumnFamily().equals(SPAN_CF)) {
      StringBuilder result = new StringBuilder();
      SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT);
      RemoteSpan span = getRemoteSpan(next);
      result.append("----------------------\n");
      result.append(String.format(" %12s:%s%n", "name", span.description));
      result.append(String.format(" %12s:%s%n", "trace", Long.toHexString(span.traceId)));
      result.append(String.format(" %12s:%s%n", "loc", span.svc + "@" + span.sender));
      result.append(String.format(" %12s:%s%n", "span", Long.toHexString(span.spanId)));
      result.append(String.format(" %12s:%s%n", "parent", Long.toHexString(span.parentId)));
      result.append(String.format(" %12s:%s%n", "start", dateFormatter.format(span.start)));
      result.append(String.format(" %12s:%s%n", "ms", span.stop - span.start));
      if (span.data != null) {
        for (Entry<String,String> entry : span.data.entrySet()) {
          result.append(String.format(" %12s:%s%n", entry.getKey(), entry.getValue()));
        }
      }
      if (span.annotations != null) {
        for (Annotation annotation : span.annotations) {
          result.append(String.format(" %12s:%s:%s%n", "annotation", annotation.getMsg(),
              dateFormatter.format(annotation.getTime())));
        }
      }

      if (config.willPrintTimestamps()) {
        result.append(String.format(" %-12s:%d%n", "timestamp", next.getKey().getTimestamp()));
      }
      return result.toString();
    }
    return DefaultFormatter.formatEntry(next, config.willPrintTimestamps());
  }

  @Override
  public void remove() {
    throw new NotImplementedException();
  }

  @Override
  public void initialize(Iterable<Entry<Key,Value>> scanner, FormatterConfig config) {
    this.scanner = scanner.iterator();
    this.config = new FormatterConfig(config);
  }
}
