import org.apache.accumulo.core.trace.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.htrace.NullScope;
import org.apache.htrace.TraceScope;

/**
 * This is a wrapper for a TraceScope object, which is a wrapper for a Span and its parent. Not
 * recommended for client use.
 */
public class Span {
  public static final Span NULL_SPAN = new Span(NullScope.INSTANCE);
  private TraceScope scope = null;
  protected org.apache.htrace.Span span = null;

  public Span(TraceScope scope) {
    this.scope = scope;
    this.span = scope.getSpan();
  }

  public Span(org.apache.htrace.Span span) {
    this.span = span;
  }

  public TraceScope getScope() {
    return scope;
  }

  public org.apache.htrace.Span getSpan() {
    return span;
  }

  public long traceId() {
    return span.getTraceId();
  }

  public void data(String k, String v) {
    if (span != null)
      span.addKVAnnotation(k.getBytes(UTF_8), v.getBytes(UTF_8));
  }

  public void stop() {
    if (scope == null) {
      if (span != null) {
        span.stop();
      }
    } else {
      scope.close();
    }
  }
}
