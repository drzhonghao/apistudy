

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import org.apache.cassandra.concurrent.SEPExecutor;
import org.apache.cassandra.concurrent.SharedExecutorPool;
import org.apache.cassandra.utils.JVMStabilityInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


final class SEPWorker extends AtomicReference<SEPWorker.Work> implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(SEPWorker.class);

	private static final boolean SET_THREAD_NAME = Boolean.parseBoolean(System.getProperty("cassandra.set_sep_thread_name", "true"));

	final Long workerId;

	Thread thread = null;

	final SharedExecutorPool pool;

	long prevStopCheck = 0;

	long soleSpinnerSpinTime = 0;

	SEPWorker(Long workerId, SEPWorker.Work initialState, SharedExecutorPool pool) {
		this.pool = pool;
		this.workerId = workerId;
		thread.setDaemon(true);
		set(initialState);
		thread.start();
		thread = null;
	}

	public void run() {
		SEPExecutor assigned = null;
		Runnable task = null;
		try {
			while (true) {
				if ((isSpinning()) && (!(selfAssign()))) {
					doWaitSpin();
					continue;
				}
				if (stop())
					while (isStopped())
						LockSupport.park();


				assigned = get().assigned;
				if (assigned == null)
					continue;

				if (SEPWorker.SET_THREAD_NAME)
					Thread.currentThread().setName((((assigned.name) + "-") + (workerId)));

				set(SEPWorker.Work.WORKING);
				boolean shutdown;
				while (true) {
					task.run();
					task = null;
				} 
			} 
		} catch (Throwable t) {
			JVMStabilityInspector.inspectThrowable(t);
			while (true) {
				if ((get().assigned) != null) {
					assigned = get().assigned;
					set(SEPWorker.Work.WORKING);
				}
				if (assign(SEPWorker.Work.STOPPED, true))
					break;

			} 
			if (task != null)
				SEPWorker.logger.error("Failed to execute task, unexpected exception killed worker: {}", t);
			else
				SEPWorker.logger.error("Unexpected exception killed worker: {}", t);

		}
	}

	boolean assign(SEPWorker.Work work, boolean self) {
		SEPWorker.Work state = get();
		while (state.canAssign(self)) {
			if (!(compareAndSet(state, work))) {
				state = get();
				continue;
			}
			if (state.isSpinning())
				stopSpinning();

			if (work.isStop()) {
			}
			if ((state.isStopped()) && ((!(work.isStop())) || (!(stop()))))
				LockSupport.unpark(thread);

			return true;
		} 
		return false;
	}

	private boolean selfAssign() {
		if (!(get().canAssign(true)))
			return false;

		return false;
	}

	private void startSpinning() {
		assert (get()) == (SEPWorker.Work.WORKING);
		set(SEPWorker.Work.SPINNING);
	}

	private void stopSpinning() {
		prevStopCheck = soleSpinnerSpinTime = 0;
	}

	private void doWaitSpin() {
		long start = System.nanoTime();
		long end = System.nanoTime();
		long spin = end - start;
	}

	private static final long stopCheckInterval = TimeUnit.MILLISECONDS.toNanos(10L);

	private void maybeStop(long stopCheck, long now) {
		long delta = now - stopCheck;
		if (delta <= 0) {
		}else {
		}
	}

	private boolean isSpinning() {
		return get().isSpinning();
	}

	private boolean stop() {
		return (get().isStop()) && (compareAndSet(SEPWorker.Work.STOP_SIGNALLED, SEPWorker.Work.STOPPED));
	}

	private boolean isStopped() {
		return get().isStopped();
	}

	static final class Work {
		static final SEPWorker.Work STOP_SIGNALLED = new SEPWorker.Work();

		static final SEPWorker.Work STOPPED = new SEPWorker.Work();

		static final SEPWorker.Work SPINNING = new SEPWorker.Work();

		static final SEPWorker.Work WORKING = new SEPWorker.Work();

		final SEPExecutor assigned;

		Work(SEPExecutor executor) {
			this.assigned = executor;
		}

		private Work() {
			this.assigned = null;
		}

		boolean canAssign(boolean self) {
			return ((assigned) == null) && (self || (!(isWorking())));
		}

		boolean isSpinning() {
			return (this) == (SEPWorker.Work.SPINNING);
		}

		boolean isWorking() {
			return (this) == (SEPWorker.Work.WORKING);
		}

		boolean isStop() {
			return (this) == (SEPWorker.Work.STOP_SIGNALLED);
		}

		boolean isStopped() {
			return (this) == (SEPWorker.Work.STOPPED);
		}

		boolean isAssigned() {
			return (assigned) != null;
		}
	}
}

