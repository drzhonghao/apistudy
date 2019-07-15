

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.apache.cassandra.concurrent.LocalAwareExecutorService;
import org.apache.cassandra.concurrent.Stage;
import org.apache.cassandra.concurrent.StageManager;
import org.apache.cassandra.tracing.TraceState;
import org.apache.cassandra.tracing.TraceStateImpl;
import org.apache.cassandra.tracing.Tracing;
import org.apache.cassandra.utils.WrappedRunnable;


class TracingImpl extends Tracing {
	public void stopSessionImpl() {
		final TraceStateImpl state = getStateImpl();
		if (state == null)
			return;

		int elapsed = state.elapsed();
		ByteBuffer sessionId = state.sessionIdBytes;
		int ttl = state.ttl;
	}

	public TraceState begin(final String request, final InetAddress client, final Map<String, String> parameters) {
		assert Tracing.isTracing();
		final TraceStateImpl state = getStateImpl();
		assert state != null;
		final long startedAt = System.currentTimeMillis();
		final ByteBuffer sessionId = state.sessionIdBytes;
		final String command = state.traceType.toString();
		final int ttl = state.ttl;
		return state;
	}

	private TraceStateImpl getStateImpl() {
		TraceState state = get();
		if (state == null)
			return null;

		if (state instanceof TraceStateImpl) {
			return ((TraceStateImpl) (state));
		}
		assert false : "TracingImpl states should be of type TraceStateImpl";
		return null;
	}

	@Override
	protected TraceState newTraceState(InetAddress coordinator, UUID sessionId, Tracing.TraceType traceType) {
		return new TraceStateImpl(coordinator, sessionId, traceType);
	}

	public void trace(final ByteBuffer sessionId, final String message, final int ttl) {
		final String threadName = Thread.currentThread().getName();
		StageManager.getStage(Stage.TRACING).execute(new WrappedRunnable() {
			public void runMayThrow() {
			}
		});
	}
}

