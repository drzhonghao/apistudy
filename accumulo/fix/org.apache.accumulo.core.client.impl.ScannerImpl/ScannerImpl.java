

import com.google.common.base.Preconditions;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.ScannerOptions;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.Authorizations;


public class ScannerImpl extends ScannerOptions implements Scanner {
	private final ClientContext context;

	private Authorizations authorizations;

	private String tableId;

	private int size;

	private Range range;

	private boolean isolated = false;

	private long readaheadThreshold = Constants.SCANNER_DEFAULT_READAHEAD_THRESHOLD;

	public ScannerImpl(ClientContext context, String tableId, Authorizations authorizations) {
		Preconditions.checkArgument((context != null), "context is null");
		Preconditions.checkArgument((tableId != null), "tableId is null");
		Preconditions.checkArgument((authorizations != null), "authorizations is null");
		this.context = context;
		this.tableId = tableId;
		this.range = new Range(((Key) (null)), ((Key) (null)));
		this.authorizations = authorizations;
		this.size = Constants.SCAN_BATCH_SIZE;
	}

	@Override
	public synchronized void setRange(Range range) {
		Preconditions.checkArgument((range != null), "range is null");
		this.range = range;
	}

	@Override
	public synchronized Range getRange() {
		return range;
	}

	@Override
	public synchronized void setBatchSize(int size) {
		if (size > 0)
			this.size = size;
		else
			throw new IllegalArgumentException("size must be greater than zero");

	}

	@Override
	public synchronized int getBatchSize() {
		return size;
	}

	@Override
	public synchronized Iterator<Map.Entry<Key, Value>> iterator() {
		return null;
	}

	@Override
	public Authorizations getAuthorizations() {
		return authorizations;
	}

	@Override
	public synchronized void enableIsolation() {
		this.isolated = true;
	}

	@Override
	public synchronized void disableIsolation() {
		this.isolated = false;
	}

	@Deprecated
	@Override
	public void setTimeOut(int timeOut) {
		if (timeOut == (Integer.MAX_VALUE))
			setTimeout(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		else
			setTimeout(timeOut, TimeUnit.SECONDS);

	}

	@Deprecated
	@Override
	public int getTimeOut() {
		long timeout = getTimeout(TimeUnit.SECONDS);
		if (timeout >= (Integer.MAX_VALUE))
			return Integer.MAX_VALUE;

		return ((int) (timeout));
	}

	@Override
	public synchronized void setReadaheadThreshold(long batches) {
		if (0 > batches) {
			throw new IllegalArgumentException("Number of batches before read-ahead must be non-negative");
		}
		readaheadThreshold = batches;
	}

	@Override
	public synchronized long getReadaheadThreshold() {
		return readaheadThreshold;
	}
}

