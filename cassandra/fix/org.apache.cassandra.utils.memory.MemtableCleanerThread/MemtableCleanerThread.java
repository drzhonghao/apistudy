

import org.apache.cassandra.utils.concurrent.WaitQueue;
import org.apache.cassandra.utils.memory.MemtablePool;


class MemtableCleanerThread<P extends MemtablePool> extends Thread {
	final P pool;

	final Runnable cleaner;

	final WaitQueue wait = new WaitQueue();

	MemtableCleanerThread(P pool, Runnable cleaner) {
		super(((pool.getClass().getSimpleName()) + "Cleaner"));
		this.pool = pool;
		this.cleaner = cleaner;
		setDaemon(true);
	}

	boolean needsCleaning() {
		return false;
	}

	void trigger() {
		wait.signal();
	}

	@Override
	public void run() {
		while (true) {
			while (!(needsCleaning())) {
				final WaitQueue.Signal signal = wait.register();
				if (!(needsCleaning()))
					signal.awaitUninterruptibly();
				else
					signal.cancel();

			} 
			cleaner.run();
		} 
	}
}

