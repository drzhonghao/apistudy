

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import org.apache.accumulo.core.client.SampleNotPresentException;
import org.apache.accumulo.core.client.impl.ScannerOptions;
import org.apache.accumulo.core.client.mock.MockTable;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.DefaultConfiguration;
import org.apache.accumulo.core.data.ArrayByteSequence;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Column;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.IteratorUtil;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.iterators.system.ColumnFamilySkippingIterator;
import org.apache.accumulo.core.iterators.system.ColumnQualifierFilter;
import org.apache.accumulo.core.iterators.system.DeletingIterator;
import org.apache.accumulo.core.iterators.system.MultiIterator;
import org.apache.accumulo.core.iterators.system.VisibilityFilter;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.commons.lang.NotImplementedException;

import static org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope.scan;


@Deprecated
public class MockScannerBase extends ScannerOptions {
	protected final MockTable table;

	protected final Authorizations auths;

	MockScannerBase(MockTable mockTable, Authorizations authorizations) {
		this.table = mockTable;
		this.auths = authorizations;
	}

	static HashSet<ByteSequence> createColumnBSS(Collection<Column> columns) {
		HashSet<ByteSequence> columnSet = new HashSet<>();
		for (Column c : columns) {
			columnSet.add(new ArrayByteSequence(c.getColumnFamily()));
		}
		return columnSet;
	}

	static class MockIteratorEnvironment implements IteratorEnvironment {
		private final Authorizations auths;

		MockIteratorEnvironment(Authorizations auths) {
			this.auths = auths;
		}

		@Override
		public SortedKeyValueIterator<Key, Value> reserveMapFileReader(String mapFileName) throws IOException {
			throw new NotImplementedException();
		}

		@Override
		public AccumuloConfiguration getConfig() {
			return AccumuloConfiguration.getDefaultConfiguration();
		}

		@Override
		public IteratorUtil.IteratorScope getIteratorScope() {
			return scan;
		}

		@Override
		public boolean isFullMajorCompaction() {
			return false;
		}

		private ArrayList<SortedKeyValueIterator<Key, Value>> topLevelIterators = new ArrayList<>();

		@Override
		public void registerSideChannel(SortedKeyValueIterator<Key, Value> iter) {
			topLevelIterators.add(iter);
		}

		@Override
		public Authorizations getAuthorizations() {
			return auths;
		}

		SortedKeyValueIterator<Key, Value> getTopLevelIterator(SortedKeyValueIterator<Key, Value> iter) {
			if (topLevelIterators.isEmpty())
				return iter;

			ArrayList<SortedKeyValueIterator<Key, Value>> allIters = new ArrayList<>(topLevelIterators);
			allIters.add(iter);
			return new MultiIterator(allIters, false);
		}

		@Override
		public boolean isSamplingEnabled() {
			return false;
		}

		@Override
		public SamplerConfiguration getSamplerConfiguration() {
			return null;
		}

		@Override
		public IteratorEnvironment cloneWithSamplingEnabled() {
			throw new SampleNotPresentException();
		}
	}

	public SortedKeyValueIterator<Key, Value> createFilter(SortedKeyValueIterator<Key, Value> inner) throws IOException {
		byte[] defaultLabels = new byte[]{  };
		inner = new ColumnFamilySkippingIterator(new DeletingIterator(inner, false));
		SortedKeyValueIterator<Key, Value> cqf = ColumnQualifierFilter.wrap(inner, new HashSet<>(fetchedColumns));
		SortedKeyValueIterator<Key, Value> vf = VisibilityFilter.wrap(cqf, auths, defaultLabels);
		MockScannerBase.MockIteratorEnvironment iterEnv = new MockScannerBase.MockIteratorEnvironment(auths);
		return null;
	}

	@Override
	public Iterator<Map.Entry<Key, Value>> iterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Authorizations getAuthorizations() {
		return auths;
	}

	@Override
	public void setClassLoaderContext(String context) {
		throw new UnsupportedOperationException();
	}
}

