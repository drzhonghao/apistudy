

import org.apache.cassandra.db.monitoring.ApproximateTime;
import org.apache.cassandra.db.monitoring.Monitorable;
import org.apache.cassandra.db.monitoring.MonitoringState;


public abstract class MonitorableImpl implements Monitorable {
	private MonitoringState state;

	private boolean isSlow;

	private long constructionTime = -1;

	private long timeout;

	private long slowTimeout;

	private boolean isCrossNode;

	protected MonitorableImpl() {
		this.state = MonitoringState.IN_PROGRESS;
		this.isSlow = false;
	}

	public void setMonitoringTime(long constructionTime, boolean isCrossNode, long timeout, long slowTimeout) {
		assert constructionTime >= 0;
		this.constructionTime = constructionTime;
		this.isCrossNode = isCrossNode;
		this.timeout = timeout;
		this.slowTimeout = slowTimeout;
	}

	public long constructionTime() {
		return constructionTime;
	}

	public long timeout() {
		return timeout;
	}

	public boolean isCrossNode() {
		return isCrossNode;
	}

	public long slowTimeout() {
		return slowTimeout;
	}

	public boolean isInProgress() {
		check();
		return (state) == (MonitoringState.IN_PROGRESS);
	}

	public boolean isAborted() {
		check();
		return (state) == (MonitoringState.ABORTED);
	}

	public boolean isCompleted() {
		check();
		return (state) == (MonitoringState.COMPLETED);
	}

	public boolean isSlow() {
		check();
		return isSlow;
	}

	public boolean abort() {
		if ((state) == (MonitoringState.IN_PROGRESS)) {
			if ((constructionTime) >= 0) {
			}
			state = MonitoringState.ABORTED;
			return true;
		}
		return (state) == (MonitoringState.ABORTED);
	}

	public boolean complete() {
		if ((state) == (MonitoringState.IN_PROGRESS)) {
			if (((isSlow) && ((slowTimeout) > 0)) && ((constructionTime) >= 0)) {
			}
			state = MonitoringState.COMPLETED;
			return true;
		}
		return (state) == (MonitoringState.COMPLETED);
	}

	private void check() {
		if (((constructionTime) < 0) || ((state) != (MonitoringState.IN_PROGRESS)))
			return;

		long elapsed = (ApproximateTime.currentTimeMillis()) - (constructionTime);
		if ((elapsed >= (slowTimeout)) && (!(isSlow)))
			isSlow = true;

		if (elapsed >= (timeout))
			abort();

	}
}

