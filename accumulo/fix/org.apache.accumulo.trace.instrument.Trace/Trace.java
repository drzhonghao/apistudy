

import org.apache.accumulo.core.trace.Span;
import org.apache.accumulo.trace.instrument.Sampler;
import org.apache.accumulo.trace.thrift.TInfo;
import org.apache.htrace.TraceScope;
import org.apache.htrace.Tracer;

import static org.apache.htrace.Trace.currentSpan;
import static org.apache.htrace.Trace.startSpan;


@Deprecated
public class Trace extends org.apache.accumulo.core.trace.Trace {
	public static Span on(String description) {
		return null;
	}

	public static void off() {
		org.apache.accumulo.core.trace.Trace.off();
	}

	public static void offNoFlush() {
		org.apache.accumulo.core.trace.Trace.offNoFlush();
	}

	public static boolean isTracing() {
		return org.apache.accumulo.core.trace.Trace.isTracing();
	}

	public static Span currentTrace() {
		return new Span(currentSpan());
	}

	public static Span start(String description) {
		return null;
	}

	public static Span trace(TInfo info, String description) {
		return null;
	}

	public static Span startThread(Span parent, String description) {
		return new Span(startSpan(description, parent.getSpan()));
	}

	public static void endThread(Span span) {
		if (span != null) {
			span.stop();
			Tracer.getInstance().continueSpan(null).close();
		}
	}

	public static Runnable wrap(Runnable runnable) {
		return org.apache.accumulo.core.trace.Trace.wrap(runnable);
	}

	public static <T> T wrapAll(T instance) {
		return org.apache.accumulo.core.trace.Trace.wrapAll(instance);
	}

	public static <T> T wrapAll(T instance, Sampler dist) {
		return org.apache.accumulo.core.trace.Trace.wrapAll(instance, dist);
	}
}

