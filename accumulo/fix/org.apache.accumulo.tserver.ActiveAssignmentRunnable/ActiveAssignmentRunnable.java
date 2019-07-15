

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.tserver.RunnableStartedAt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ActiveAssignmentRunnable implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(ActiveAssignmentRunnable.class);

	private final ConcurrentHashMap<KeyExtent, RunnableStartedAt> activeAssignments;

	private final KeyExtent extent;

	private final Runnable delegate;

	private volatile Thread executingThread;

	public ActiveAssignmentRunnable(ConcurrentHashMap<KeyExtent, RunnableStartedAt> activeAssignments, KeyExtent extent, Runnable delegate) {
		Objects.requireNonNull(activeAssignments);
		Objects.requireNonNull(extent);
		Objects.requireNonNull(delegate);
		this.activeAssignments = activeAssignments;
		this.extent = extent;
		this.delegate = delegate;
	}

	@Override
	public void run() {
		if (activeAssignments.containsKey(extent)) {
			throw new IllegalStateException(("Active assignment already exists for " + (extent)));
		}
		executingThread = Thread.currentThread();
		try {
			delegate.run();
		} finally {
			if (ActiveAssignmentRunnable.log.isTraceEnabled()) {
				ActiveAssignmentRunnable.log.trace("Finished assignment for {} at {}", extent, System.currentTimeMillis());
			}
			activeAssignments.remove(extent);
		}
	}

	public Exception getException() {
		final Exception e = new Exception(("Assignment of " + (extent)));
		if (null != (executingThread)) {
			e.setStackTrace(executingThread.getStackTrace());
		}
		return e;
	}
}

