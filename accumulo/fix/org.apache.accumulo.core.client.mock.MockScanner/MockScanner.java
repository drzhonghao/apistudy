

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.impl.ScannerOptions;
import org.apache.accumulo.core.client.mock.MockScannerBase;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.Filter;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.iterators.WrappingIterator;


@Deprecated
public class MockScanner extends MockScannerBase implements Scanner {
	int batchSize = 0;

	Range range = new Range();

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
	public void setRange(Range range) {
		this.range = range;
	}

	@Override
	public Range getRange() {
		return this.range;
	}

	@Override
	public void setBatchSize(int size) {
		this.batchSize = size;
	}

	@Override
	public int getBatchSize() {
		return this.batchSize;
	}

	@Override
	public void enableIsolation() {
	}

	@Override
	public void disableIsolation() {
	}

	static class RangeFilter extends Filter {
		Range range;

		RangeFilter(SortedKeyValueIterator<Key, Value> i, Range range) {
			setSource(i);
			this.range = range;
		}

		@Override
		public boolean accept(Key k, Value v) {
			return range.contains(k);
		}
	}

	@Override
	public Iterator<Map.Entry<Key, Value>> iterator() {
		return null;
	}

	@Override
	public long getReadaheadThreshold() {
		return 0;
	}

	@Override
	public void setReadaheadThreshold(long batches) {
	}
}

