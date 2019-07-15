

import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import org.apache.cassandra.concurrent.NamedThreadFactory;
import org.apache.cassandra.config.RequestSchedulerOptions;
import org.apache.cassandra.scheduler.IRequestScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RoundRobinScheduler implements IRequestScheduler {
	private static final Logger logger = LoggerFactory.getLogger(RoundRobinScheduler.class);

	private final Semaphore taskCount;

	private final Semaphore queueSize = new Semaphore(0, false);

	private final int defaultWeight;

	private final Map<String, Integer> weights;

	public RoundRobinScheduler(RequestSchedulerOptions options) {
		defaultWeight = options.default_weight;
		weights = options.weights;
		taskCount = new Semaphore(((options.throttle_limit) - 1));
		Runnable runnable = () -> {
			while (true) {
				schedule();
			} 
		};
		Thread scheduler = NamedThreadFactory.createThread(runnable, "REQUEST-SCHEDULER");
		scheduler.start();
		RoundRobinScheduler.logger.info("Started the RoundRobin Request Scheduler");
	}

	public void queue(Thread t, String id, long timeoutMS) throws TimeoutException {
		queueSize.release();
	}

	public void release() {
		taskCount.release();
	}

	private void schedule() {
		queueSize.acquireUninterruptibly();
		queueSize.release();
	}

	Semaphore getTaskCount() {
		return taskCount;
	}

	private int getWeight(String weightingVar) {
		return ((weights) != null) && (weights.containsKey(weightingVar)) ? weights.get(weightingVar) : defaultWeight;
	}
}

