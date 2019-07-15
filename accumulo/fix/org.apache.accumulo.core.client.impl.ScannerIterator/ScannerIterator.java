

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.ScannerOptions;
import org.apache.accumulo.core.client.impl.ThriftScanner;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.KeyValue;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.util.NamingThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ScannerIterator implements Iterator<Map.Entry<Key, Value>> {
	private static final Logger log = LoggerFactory.getLogger(ScannerIterator.class);

	private int timeOut;

	private Iterator<KeyValue> iter;

	private ThriftScanner.ScanState scanState;

	private ScannerOptions options;

	private ArrayBlockingQueue<Object> synchQ;

	private boolean finished = false;

	private boolean readaheadInProgress = false;

	private long batchCount = 0;

	private long readaheadThreshold;

	private static final List<KeyValue> EMPTY_LIST = Collections.emptyList();

	private static ThreadPoolExecutor readaheadPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 3L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new NamingThreadFactory("Accumulo scanner read ahead thread"));

	private class Reader implements Runnable {
		@Override
		public void run() {
			try {
				while (true) {
					return;
				} 
			} catch (Exception e) {
				ScannerIterator.log.error("{}", e.getMessage(), e);
				synchQ.add(e);
			}
		}
	}

	ScannerIterator(ClientContext context, String tableId, Authorizations authorizations, Range range, int size, int timeOut, ScannerOptions options, boolean isolated, long readaheadThreshold) {
		this.timeOut = timeOut;
		this.readaheadThreshold = readaheadThreshold;
		this.options = new ScannerOptions(options);
		synchQ = new ArrayBlockingQueue<>(1);
		if (0L == readaheadThreshold) {
			initiateReadAhead();
		}
		iter = null;
	}

	private void initiateReadAhead() {
		readaheadInProgress = true;
		ScannerIterator.readaheadPool.execute(new ScannerIterator.Reader());
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean hasNext() {
		if (finished)
			return false;

		if (((iter) != null) && (iter.hasNext())) {
			return true;
		}
		try {
			if (!(readaheadInProgress)) {
				new ScannerIterator.Reader().run();
			}
			Object obj = synchQ.take();
			if (obj instanceof Exception) {
				finished = true;
				if (obj instanceof RuntimeException)
					throw ((RuntimeException) (obj));
				else
					throw new RuntimeException(((Exception) (obj)));

			}
			List<KeyValue> currentBatch = ((List<KeyValue>) (obj));
			if ((currentBatch.size()) == 0) {
				currentBatch = null;
				finished = true;
				return false;
			}
			iter = currentBatch.iterator();
			(batchCount)++;
			if ((batchCount) > (readaheadThreshold)) {
				initiateReadAhead();
			}
		} catch (InterruptedException e1) {
			throw new RuntimeException(e1);
		}
		return true;
	}

	@Override
	public Map.Entry<Key, Value> next() {
		if (hasNext())
			return iter.next();

		throw new NoSuchElementException();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("remove is not supported in Scanner");
	}
}

