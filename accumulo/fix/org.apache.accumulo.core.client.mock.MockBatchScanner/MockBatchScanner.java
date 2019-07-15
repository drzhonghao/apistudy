

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.accumulo.core.client.BatchScanner;
import org.apache.accumulo.core.client.mock.MockScannerBase;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.commons.collections.iterators.IteratorChain;


@Deprecated
public class MockBatchScanner extends MockScannerBase implements BatchScanner {
	List<Range> ranges = null;

	@Override
	public void setRanges(Collection<Range> ranges) {
		if ((ranges == null) || ((ranges.size()) == 0)) {
			throw new IllegalArgumentException("ranges must be non null and contain at least 1 range");
		}
		this.ranges = new ArrayList<>(ranges);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<Map.Entry<Key, Value>> iterator() {
		if ((ranges) == null) {
			throw new IllegalStateException("ranges not set");
		}
		IteratorChain chain = new IteratorChain();
		for (Range range : ranges) {
		}
		return chain;
	}

	@Override
	public void close() {
	}
}

