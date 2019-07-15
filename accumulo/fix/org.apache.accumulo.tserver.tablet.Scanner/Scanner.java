

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IterationInterruptedException;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.tserver.tablet.ScanBatch;
import org.apache.accumulo.tserver.tablet.Tablet;
import org.apache.accumulo.tserver.tablet.TabletClosedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Scanner {
	private static final Logger log = LoggerFactory.getLogger(Scanner.class);

	private final Tablet tablet = null;

	private Range range;

	private SortedKeyValueIterator<Key, Value> isolatedIter;

	private boolean sawException = false;

	private boolean scanClosed = false;

	private Semaphore scannerSemaphore;

	public ScanBatch read() throws IOException, TabletClosedException {
		try {
			try {
				scannerSemaphore.acquire();
			} catch (InterruptedException e) {
				sawException = true;
			}
			if (sawException)
				throw new IllegalStateException("Tried to use scanner after exception occurred.");

			if (scanClosed)
				throw new IllegalStateException("Tried to use scanner after it was closed.");

			SortedKeyValueIterator<Key, Value> iter;
		} catch (IterationInterruptedException iie) {
			sawException = true;
			if (tablet.isClosed())
				throw new TabletClosedException(iie);
			else
				throw iie;

		} catch (RuntimeException re) {
			sawException = true;
			throw re;
		} finally {
			scannerSemaphore.release();
		}
		return null;
	}

	public boolean close() {
		boolean obtainedLock = false;
		try {
			obtainedLock = scannerSemaphore.tryAcquire(10, TimeUnit.MILLISECONDS);
			if (!obtainedLock)
				return false;

			scanClosed = true;
		} catch (InterruptedException e) {
			return false;
		} finally {
			if (obtainedLock)
				scannerSemaphore.release();

		}
		return true;
	}
}

