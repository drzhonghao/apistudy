

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.RowIterator;
import org.apache.accumulo.core.client.SampleNotPresentException;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.InstanceOperations;
import org.apache.accumulo.core.client.impl.Credentials;
import org.apache.accumulo.core.client.impl.ScannerOptions;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.ConfigurationCopy;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.KeyValue;
import org.apache.accumulo.core.data.PartialKey;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.file.FileOperations;
import org.apache.accumulo.core.file.FileSKVIterator;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.IteratorUtil;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.iterators.system.MultiIterator;
import org.apache.accumulo.core.master.state.tables.TableState;
import org.apache.accumulo.core.metadata.MetadataTable;
import org.apache.accumulo.core.metadata.schema.MetadataSchema;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.security.ColumnVisibility;
import org.apache.accumulo.core.util.CachedConfiguration;
import org.apache.accumulo.core.util.ColumnFQ;
import org.apache.accumulo.core.util.Pair;
import org.apache.accumulo.core.volume.Volume;
import org.apache.accumulo.core.volume.VolumeConfiguration;
import org.apache.accumulo.fate.util.UtilWaitThread;
import org.apache.commons.lang.NotImplementedException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.Text;

import static org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope.scan;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.DataFileColumnFamily.NAME;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.TabletColumnFamily.PREV_ROW_COLUMN;


class OfflineIterator implements Iterator<Map.Entry<Key, Value>> {
	static class OfflineIteratorEnvironment implements IteratorEnvironment {
		private final Authorizations authorizations;

		private AccumuloConfiguration conf;

		private boolean useSample;

		private SamplerConfiguration sampleConf;

		public OfflineIteratorEnvironment(Authorizations auths, AccumuloConfiguration acuTableConf, boolean useSample, SamplerConfiguration samplerConf) {
			this.authorizations = auths;
			this.conf = acuTableConf;
			this.useSample = useSample;
			this.sampleConf = samplerConf;
		}

		@Override
		public SortedKeyValueIterator<Key, Value> reserveMapFileReader(String mapFileName) throws IOException {
			throw new NotImplementedException();
		}

		@Override
		public AccumuloConfiguration getConfig() {
			return conf;
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
			return authorizations;
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
			return useSample;
		}

		@Override
		public SamplerConfiguration getSamplerConfiguration() {
			return sampleConf;
		}

		@Override
		public IteratorEnvironment cloneWithSamplingEnabled() {
			if ((sampleConf) == null)
				throw new SampleNotPresentException();

			return new OfflineIterator.OfflineIteratorEnvironment(authorizations, conf, true, sampleConf);
		}
	}

	private SortedKeyValueIterator<Key, Value> iter;

	private Range range;

	private KeyExtent currentExtent;

	private Connector conn;

	private String tableId;

	private Authorizations authorizations;

	private Instance instance;

	private ScannerOptions options;

	private ArrayList<SortedKeyValueIterator<Key, Value>> readers;

	private AccumuloConfiguration config;

	public OfflineIterator(ScannerOptions options, Instance instance, Credentials credentials, Authorizations authorizations, Text table, Range range) {
		this.options = new ScannerOptions(options);
		this.instance = instance;
		this.range = range;
		this.tableId = table.toString();
		this.authorizations = authorizations;
		this.readers = new ArrayList<>();
		try {
			conn = instance.getConnector(credentials.getPrincipal(), credentials.getToken());
			config = new ConfigurationCopy(conn.instanceOperations().getSiteConfiguration());
			nextTablet();
			while (((iter) != null) && (!(iter.hasTop())))
				nextTablet();

		} catch (Exception e) {
			if (e instanceof RuntimeException)
				throw ((RuntimeException) (e));

			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean hasNext() {
		return ((iter) != null) && (iter.hasTop());
	}

	@Override
	public Map.Entry<Key, Value> next() {
		try {
			byte[] v = iter.getTopValue().get();
			KeyValue ret = new KeyValue(new Key(iter.getTopKey()), Arrays.copyOf(v, v.length));
			iter.next();
			while (((iter) != null) && (!(iter.hasTop())))
				nextTablet();

			return ret;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void nextTablet() throws IOException, AccumuloException, TableNotFoundException {
		Range nextRange = null;
		if ((currentExtent) == null) {
			Text startRow;
			if ((range.getStartKey()) != null)
				startRow = range.getStartKey().getRow();
			else
				startRow = new Text();

			nextRange = new Range(new KeyExtent(tableId, startRow, null).getMetadataEntry(), true, null, false);
		}else {
			if ((currentExtent.getEndRow()) == null) {
				iter = null;
				return;
			}
			if (range.afterEndKey(new Key(currentExtent.getEndRow()).followingKey(PartialKey.ROW))) {
				iter = null;
				return;
			}
			nextRange = new Range(currentExtent.getMetadataEntry(), false, null, false);
		}
		List<String> relFiles = new ArrayList<>();
		Pair<KeyExtent, String> eloc = getTabletFiles(nextRange, relFiles);
		while ((eloc.getSecond()) != null) {
			if ((Tables.getTableState(instance, tableId)) != (TableState.OFFLINE)) {
				Tables.clearCache(instance);
				if ((Tables.getTableState(instance, tableId)) != (TableState.OFFLINE)) {
					throw new AccumuloException(((("Table is online " + (tableId)) + " cannot scan tablet in offline mode ") + (eloc.getFirst())));
				}
			}
			UtilWaitThread.sleepUninterruptibly(250, TimeUnit.MILLISECONDS);
			eloc = getTabletFiles(nextRange, relFiles);
		} 
		KeyExtent extent = eloc.getFirst();
		if (!(extent.getTableId().equals(tableId))) {
			throw new AccumuloException((((" did not find tablets for table " + (tableId)) + " ") + extent));
		}
		if (((currentExtent) != null) && (!(extent.isPreviousExtent(currentExtent))))
			throw new AccumuloException((((" " + (currentExtent)) + " is not previous extent ") + extent));

		@SuppressWarnings("deprecation")
		String tablesDir = (config.get(Property.INSTANCE_DFS_DIR)) + (Constants.HDFS_TABLES_DIR);
		List<String> absFiles = new ArrayList<>();
		for (String relPath : relFiles) {
			if (relPath.contains(":")) {
				absFiles.add(relPath);
			}else {
				if (relPath.startsWith("..")) {
					absFiles.add((tablesDir + (relPath.substring(2))));
				}else {
					absFiles.add((((tablesDir + "/") + (tableId)) + relPath));
				}
			}
		}
		iter = createIterator(extent, absFiles);
		currentExtent = extent;
	}

	private Pair<KeyExtent, String> getTabletFiles(Range nextRange, List<String> relFiles) throws TableNotFoundException {
		Scanner scanner = conn.createScanner(MetadataTable.NAME, Authorizations.EMPTY);
		scanner.setBatchSize(100);
		scanner.setRange(nextRange);
		RowIterator rowIter = new RowIterator(scanner);
		Iterator<Map.Entry<Key, Value>> row = rowIter.next();
		KeyExtent extent = null;
		String location = null;
		while (row.hasNext()) {
			Map.Entry<Key, Value> entry = row.next();
			Key key = entry.getKey();
			if (key.getColumnFamily().equals(NAME)) {
				relFiles.add(key.getColumnQualifier().toString());
			}
			if ((key.getColumnFamily().equals(MetadataSchema.TabletsSection.CurrentLocationColumnFamily.NAME)) || (key.getColumnFamily().equals(MetadataSchema.TabletsSection.FutureLocationColumnFamily.NAME))) {
				location = entry.getValue().toString();
			}
			if (PREV_ROW_COLUMN.hasColumns(key)) {
				extent = new KeyExtent(key.getRow(), entry.getValue());
			}
		} 
		return new Pair<>(extent, location);
	}

	private SortedKeyValueIterator<Key, Value> createIterator(KeyExtent extent, List<String> absFiles) throws IOException, AccumuloException, TableNotFoundException {
		AccumuloConfiguration acuTableConf = AccumuloConfiguration.getTableConfiguration(conn, tableId);
		Configuration conf = CachedConfiguration.getInstance();
		for (SortedKeyValueIterator<Key, Value> reader : readers) {
			((FileSKVIterator) (reader)).close();
		}
		readers.clear();
		SamplerConfiguration scannerSamplerConfig = options.getSamplerConfiguration();
		SamplerConfigurationImpl scannerSamplerConfigImpl = (scannerSamplerConfig == null) ? null : new SamplerConfigurationImpl(scannerSamplerConfig);
		SamplerConfigurationImpl samplerConfImpl = SamplerConfigurationImpl.newSamplerConfig(acuTableConf);
		if ((scannerSamplerConfigImpl != null) && (((samplerConfImpl != null) && (!(scannerSamplerConfigImpl.equals(samplerConfImpl)))) || (samplerConfImpl == null))) {
			throw new SampleNotPresentException();
		}
		for (String file : absFiles) {
			FileSystem fs = VolumeConfiguration.getVolume(file, conf, config).getFileSystem();
			FileSKVIterator reader = FileOperations.getInstance().newReaderBuilder().forFile(file, fs, conf).withTableConfiguration(acuTableConf).build();
			if (scannerSamplerConfigImpl != null) {
				reader = reader.getSample(scannerSamplerConfigImpl);
				if (reader == null)
					throw new SampleNotPresentException();

			}
			readers.add(reader);
		}
		MultiIterator multiIter = new MultiIterator(readers, extent);
		OfflineIterator.OfflineIteratorEnvironment iterEnv = new OfflineIterator.OfflineIteratorEnvironment(authorizations, acuTableConf, false, (samplerConfImpl == null ? null : samplerConfImpl.toSamplerConfiguration()));
		byte[] defaultSecurityLabel;
		ColumnVisibility cv = new ColumnVisibility(acuTableConf.get(Property.TABLE_DEFAULT_SCANTIME_VISIBILITY));
		defaultSecurityLabel = cv.getExpression();
		return null;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}

