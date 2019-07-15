

import java.net.InetAddress;
import java.util.UUID;
import org.apache.cassandra.tracing.TraceState;
import org.apache.cassandra.tracing.Tracing;
import org.apache.cassandra.utils.FBUtilities;


class ExpiredTraceState extends TraceState {
	private final TraceState delegate;

	ExpiredTraceState(TraceState delegate) {
		super(FBUtilities.getBroadcastAddress(), delegate.sessionId, delegate.traceType);
		this.delegate = delegate;
	}

	public int elapsed() {
		return -1;
	}

	protected void traceImpl(String message) {
	}

	protected void waitForPendingEvents() {
	}

	TraceState getDelegate() {
		return delegate;
	}
}

