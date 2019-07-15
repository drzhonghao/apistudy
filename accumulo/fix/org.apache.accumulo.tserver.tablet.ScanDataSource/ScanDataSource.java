

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
import org.apache.accumulo.core.data.Column;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.data.thrift.IterInfo;
import org.apache.accumulo.core.iterators.IterationInterruptedException;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.IteratorUtil;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.iterators.system.SourceSwitchingIterator;
import org.apache.accumulo.core.iterators.system.StatsIterator;
import org.apache.accumulo.core.metadata.schema.DataFileValue;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.server.conf.TableConfiguration;
import org.apache.accumulo.server.fs.FileRef;
import org.apache.accumulo.tserver.FileManager;
import org.apache.accumulo.tserver.InMemoryMap;
import org.apache.accumulo.tserver.InMemoryMap.MemoryIterator;
import org.apache.accumulo.tserver.tablet.Tablet;
import org.apache.accumulo.tserver.tablet.TabletClosedException;

import static org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope.scan;


class ScanDataSource implements SourceSwitchingIterator.DataSource {
	private final Tablet tablet;

	private FileManager.ScanFileManager fileManager;

	private SortedKeyValueIterator<Key, Value> iter;

	private long expectedDeletionCount;

	private List<InMemoryMap.MemoryIterator> memIters = null;

	private long fileReservationId;

	private AtomicBoolean interruptFlag;

	private StatsIterator statsIterator;

	private final boolean loadIters;

	private static final Set<Column> EMPTY_COLS = Collections.emptySet();

	ScanDataSource(Tablet tablet, Authorizations authorizations, byte[] defaultLabels, HashSet<Column> columnSet, List<IterInfo> ssiList, Map<String, Map<String, String>> ssio, AtomicBoolean interruptFlag, SamplerConfiguration samplerConfig, long batchTimeOut, String context) {
		this.tablet = tablet;
		expectedDeletionCount = tablet.getDataSourceDeletions();
		this.interruptFlag = interruptFlag;
		this.loadIters = true;
	}

	ScanDataSource(Tablet tablet, Authorizations authorizations, byte[] defaultLabels, AtomicBoolean iFlag) {
		this.tablet = tablet;
		expectedDeletionCount = tablet.getDataSourceDeletions();
		this.interruptFlag = iFlag;
		this.loadIters = false;
	}

	@Override
	public SourceSwitchingIterator.DataSource getNewDataSource() {
		if (!(isCurrent())) {
			if ((memIters) != null) {
				memIters = null;
				fileReservationId = -1;
			}
			if ((fileManager) != null)
				fileManager.releaseOpenFiles(false);

			expectedDeletionCount = tablet.getDataSourceDeletions();
			iter = null;
			return this;
		}else
			return this;

	}

	@Override
	public boolean isCurrent() {
		return (expectedDeletionCount) == (tablet.getDataSourceDeletions());
	}

	@Override
	public SortedKeyValueIterator<Key, Value> iterator() throws IOException {
		if ((iter) == null)
			iter = createIterator();

		return iter;
	}

	private SortedKeyValueIterator<Key, Value> createIterator() throws IOException {
		Map<FileRef, DataFileValue> files;
		synchronized(tablet) {
			if ((memIters) != null)
				throw new IllegalStateException("Tried to create new scan iterator w/o releasing memory");

			if (tablet.isClosed())
				throw new TabletClosedException();

			if (interruptFlag.get())
				throw new IterationInterruptedException((((tablet.getExtent().toString()) + " ") + (interruptFlag.hashCode())));

			if ((fileManager) == null) {
			}
			if ((fileManager.getNumOpenFiles()) != 0)
				throw new IllegalStateException("Tried to create new scan iterator w/o releasing files");

			expectedDeletionCount = tablet.getDataSourceDeletions();
		}
		if (!(loadIters)) {
		}else {
			List<IterInfo> iterInfos;
			Map<String, Map<String, String>> iterOpts;
			TableConfiguration.ParsedIteratorConfig pic = tablet.getTableConfiguration().getParsedIteratorConfig(scan);
			String context;
		}
		return null;
	}

	void close(boolean sawErrors) {
		if ((memIters) != null) {
			memIters = null;
			fileReservationId = -1;
		}
		synchronized(tablet) {
		}
		if ((fileManager) != null) {
			fileManager.releaseOpenFiles(sawErrors);
			fileManager = null;
		}
		if ((statsIterator) != null) {
			statsIterator.report();
		}
	}

	public void interrupt() {
		interruptFlag.set(true);
	}

	@Override
	public SourceSwitchingIterator.DataSource getDeepCopyDataSource(IteratorEnvironment env) {
		throw new UnsupportedOperationException();
	}

	public void reattachFileManager() throws IOException {
		if ((fileManager) != null) {
		}
	}

	public void detachFileManager() {
		if ((fileManager) != null)
			fileManager.detach();

	}

	@Override
	public void setInterruptFlag(AtomicBoolean flag) {
		throw new UnsupportedOperationException();
	}
}

