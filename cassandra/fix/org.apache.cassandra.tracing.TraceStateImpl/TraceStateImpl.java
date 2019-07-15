

import com.google.common.annotations.VisibleForTesting;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.cassandra.concurrent.LocalAwareExecutorService;
import org.apache.cassandra.concurrent.Stage;
import org.apache.cassandra.concurrent.StageManager;
import org.apache.cassandra.db.ConsistencyLevel;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.exceptions.OverloadedException;
import org.apache.cassandra.service.StorageProxy;
import org.apache.cassandra.tracing.TraceState;
import org.apache.cassandra.tracing.Tracing;
import org.apache.cassandra.utils.JVMStabilityInspector;
import org.apache.cassandra.utils.WrappedRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TraceStateImpl extends TraceState {
	private static final Logger logger = LoggerFactory.getLogger(TraceStateImpl.class);

	@VisibleForTesting
	public static int WAIT_FOR_PENDING_EVENTS_TIMEOUT_SECS = Integer.parseInt(System.getProperty("cassandra.wait_for_tracing_events_timeout_secs", "0"));

	private final Set<Future<?>> pendingFutures = ConcurrentHashMap.newKeySet();

	public TraceStateImpl(InetAddress coordinator, UUID sessionId, Tracing.TraceType traceType) {
		super(coordinator, sessionId, traceType);
	}

	protected void traceImpl(String message) {
		final String threadName = Thread.currentThread().getName();
		final int elapsed = elapsed();
		if (TraceStateImpl.logger.isTraceEnabled())
			TraceStateImpl.logger.trace("Adding <{}> to trace events", message);

	}

	protected void waitForPendingEvents() {
		if ((TraceStateImpl.WAIT_FOR_PENDING_EVENTS_TIMEOUT_SECS) <= 0)
			return;

		try {
			if (TraceStateImpl.logger.isTraceEnabled())
				TraceStateImpl.logger.trace("Waiting for up to {} seconds for {} trace events to complete", (+(TraceStateImpl.WAIT_FOR_PENDING_EVENTS_TIMEOUT_SECS)), pendingFutures.size());

			CompletableFuture.allOf(pendingFutures.toArray(new CompletableFuture<?>[pendingFutures.size()])).get(TraceStateImpl.WAIT_FOR_PENDING_EVENTS_TIMEOUT_SECS, TimeUnit.SECONDS);
		} catch (TimeoutException ex) {
			if (TraceStateImpl.logger.isTraceEnabled())
				TraceStateImpl.logger.trace("Failed to wait for tracing events to complete in {} seconds", TraceStateImpl.WAIT_FOR_PENDING_EVENTS_TIMEOUT_SECS);

		} catch (Throwable t) {
			JVMStabilityInspector.inspectThrowable(t);
			TraceStateImpl.logger.error("Got exception whilst waiting for tracing events to complete", t);
		}
	}

	void executeMutation(final Mutation mutation) {
		CompletableFuture<Void> fut = CompletableFuture.runAsync(new WrappedRunnable() {
			protected void runMayThrow() {
				TraceStateImpl.mutateWithCatch(mutation);
			}
		}, StageManager.getStage(Stage.TRACING));
		boolean ret = pendingFutures.add(fut);
		if (!ret)
			TraceStateImpl.logger.warn("Failed to insert pending future, tracing synchronization may not work");

	}

	static void mutateWithCatch(Mutation mutation) {
		try {
			StorageProxy.mutate(Collections.singletonList(mutation), ConsistencyLevel.ANY, System.nanoTime());
		} catch (OverloadedException e) {
		}
	}
}

