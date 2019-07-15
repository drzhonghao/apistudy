

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import org.apache.cassandra.exceptions.RequestFailureReason;
import org.apache.cassandra.exceptions.WriteFailureException;
import org.apache.cassandra.exceptions.WriteTimeoutException;
import org.apache.cassandra.net.IAsyncCallback;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.service.AbstractWriteResponseHandler;


public class BatchlogResponseHandler<T> extends AbstractWriteResponseHandler<T> {
	AbstractWriteResponseHandler<T> wrapped;

	BatchlogResponseHandler.BatchlogCleanup cleanup;

	protected volatile int requiredBeforeFinish;

	private static final AtomicIntegerFieldUpdater<BatchlogResponseHandler> requiredBeforeFinishUpdater = AtomicIntegerFieldUpdater.newUpdater(BatchlogResponseHandler.class, "requiredBeforeFinish");

	protected int ackCount() {
		return 0;
	}

	public void response(MessageIn<T> msg) {
		wrapped.response(msg);
		if ((BatchlogResponseHandler.requiredBeforeFinishUpdater.decrementAndGet(this)) == 0)
			cleanup.ackMutation();

	}

	public boolean isLatencyForSnitch() {
		return wrapped.isLatencyForSnitch();
	}

	public void onFailure(InetAddress from, RequestFailureReason failureReason) {
		wrapped.onFailure(from, failureReason);
	}

	public void assureSufficientLiveNodes() {
		wrapped.assureSufficientLiveNodes();
	}

	public void get() throws WriteFailureException, WriteTimeoutException {
		wrapped.get();
	}

	protected int totalBlockFor() {
		return 0;
	}

	protected int totalEndpoints() {
		return 0;
	}

	protected boolean waitingFor(InetAddress from) {
		return false;
	}

	protected void signal() {
	}

	public static class BatchlogCleanup {
		private final BatchlogResponseHandler.BatchlogCleanupCallback callback;

		protected volatile int mutationsWaitingFor;

		private static final AtomicIntegerFieldUpdater<BatchlogResponseHandler.BatchlogCleanup> mutationsWaitingForUpdater = AtomicIntegerFieldUpdater.newUpdater(BatchlogResponseHandler.BatchlogCleanup.class, "mutationsWaitingFor");

		public BatchlogCleanup(int mutationsWaitingFor, BatchlogResponseHandler.BatchlogCleanupCallback callback) {
			this.mutationsWaitingFor = mutationsWaitingFor;
			this.callback = callback;
		}

		public void ackMutation() {
			if ((BatchlogResponseHandler.BatchlogCleanup.mutationsWaitingForUpdater.decrementAndGet(this)) == 0)
				callback.invoke();

		}
	}

	public interface BatchlogCleanupCallback {
		public abstract void invoke();
	}
}

