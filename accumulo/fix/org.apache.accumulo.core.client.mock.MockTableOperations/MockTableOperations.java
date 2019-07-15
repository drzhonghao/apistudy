

import com.google.common.base.Preconditions;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.NamespaceNotFoundException;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.CompactionConfig;
import org.apache.accumulo.core.client.admin.CompactionStrategyConfig;
import org.apache.accumulo.core.client.admin.DiskUsage;
import org.apache.accumulo.core.client.admin.Locations;
import org.apache.accumulo.core.client.admin.NewTableConfiguration;
import org.apache.accumulo.core.client.admin.TimeType;
import org.apache.accumulo.core.client.impl.TableOperationsHelper;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.client.mock.MockAccumulo;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.DefaultConfiguration;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.file.FileOperations;
import org.apache.accumulo.core.file.FileSKVIterator;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.security.ColumnVisibility;
import org.apache.accumulo.core.util.Pair;
import org.apache.accumulo.start.classloader.vfs.AccumuloVFSClassLoader;
import org.apache.commons.lang.NotImplementedException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Deprecated
class MockTableOperations extends TableOperationsHelper {
	private static final Logger log = LoggerFactory.getLogger(MockTableOperations.class);

	private static final byte[] ZERO = new byte[]{ 0 };

	private final MockAccumulo acu;

	private final String username;

	MockTableOperations(MockAccumulo acu, String username) {
		this.acu = acu;
		this.username = username;
	}

	@Override
	public SortedSet<String> list() {
		return null;
	}

	@Override
	public boolean exists(String tableName) {
		return false;
	}

	private boolean namespaceExists(String namespace) {
		return false;
	}

	@Override
	public void create(String tableName) throws AccumuloException, AccumuloSecurityException, TableExistsException {
		create(tableName, new NewTableConfiguration());
	}

	@Override
	@Deprecated
	public void create(String tableName, boolean versioningIter) throws AccumuloException, AccumuloSecurityException, TableExistsException {
		create(tableName, versioningIter, TimeType.MILLIS);
	}

	@Override
	@Deprecated
	public void create(String tableName, boolean versioningIter, TimeType timeType) throws AccumuloException, AccumuloSecurityException, TableExistsException {
		NewTableConfiguration ntc = new NewTableConfiguration().setTimeType(timeType);
		if (versioningIter)
			create(tableName, ntc);
		else
			create(tableName, ntc.withoutDefaultIterators());

	}

	@Override
	public void create(String tableName, NewTableConfiguration ntc) throws AccumuloException, AccumuloSecurityException, TableExistsException {
		String namespace = Tables.qualify(tableName).getFirst();
		Preconditions.checkArgument(tableName.matches(Tables.VALID_NAME_REGEX));
		if (exists(tableName))
			throw new TableExistsException(tableName, tableName, "");

		Preconditions.checkArgument(namespaceExists(namespace), (("Namespace (" + namespace) + ") does not exist, create it first"));
		acu.createTable(username, tableName, ntc.getTimeType(), ntc.getProperties());
	}

	@Override
	public void addSplits(String tableName, SortedSet<Text> partitionKeys) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		if (!(exists(tableName)))
			throw new TableNotFoundException(tableName, tableName, "");

		acu.addSplits(tableName, partitionKeys);
	}

	@Deprecated
	@Override
	public Collection<Text> getSplits(String tableName) throws TableNotFoundException {
		return listSplits(tableName);
	}

	@Deprecated
	@Override
	public Collection<Text> getSplits(String tableName, int maxSplits) throws TableNotFoundException {
		return listSplits(tableName);
	}

	@Override
	public Collection<Text> listSplits(String tableName) throws TableNotFoundException {
		if (!(exists(tableName)))
			throw new TableNotFoundException(tableName, tableName, "");

		return acu.getSplits(tableName);
	}

	@Override
	public Collection<Text> listSplits(String tableName, int maxSplits) throws TableNotFoundException {
		return listSplits(tableName);
	}

	@Override
	public void delete(String tableName) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		if (!(exists(tableName)))
			throw new TableNotFoundException(tableName, tableName, "");

	}

	@Override
	public void rename(String oldTableName, String newTableName) throws AccumuloException, AccumuloSecurityException, TableExistsException, TableNotFoundException {
		if (!(exists(oldTableName)))
			throw new TableNotFoundException(oldTableName, oldTableName, "");

		if (exists(newTableName))
			throw new TableExistsException(newTableName, newTableName, "");

		String namespace = Tables.qualify(newTableName).getFirst();
	}

	@Deprecated
	@Override
	public void flush(String tableName) throws AccumuloException, AccumuloSecurityException {
	}

	@Override
	public void setProperty(String tableName, String property, String value) throws AccumuloException, AccumuloSecurityException {
	}

	@Override
	public void removeProperty(String tableName, String property) throws AccumuloException, AccumuloSecurityException {
	}

	@Override
	public Iterable<Map.Entry<String, String>> getProperties(String tableName) throws TableNotFoundException {
		String namespace = Tables.qualify(tableName).getFirst();
		if (!(exists(tableName))) {
			if (!(namespaceExists(namespace)))
				throw new TableNotFoundException(tableName, new NamespaceNotFoundException(null, namespace, null));

			throw new TableNotFoundException(null, tableName, null);
		}
		return null;
	}

	@Override
	public void setLocalityGroups(String tableName, Map<String, Set<Text>> groups) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		if (!(exists(tableName)))
			throw new TableNotFoundException(tableName, tableName, "");

	}

	@Override
	public Map<String, Set<Text>> getLocalityGroups(String tableName) throws AccumuloException, TableNotFoundException {
		if (!(exists(tableName)))
			throw new TableNotFoundException(tableName, tableName, "");

		return null;
	}

	@Override
	public Set<Range> splitRangeByTablets(String tableName, Range range, int maxSplits) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		if (!(exists(tableName)))
			throw new TableNotFoundException(tableName, tableName, "");

		return Collections.singleton(range);
	}

	@Override
	public void importDirectory(String tableName, String dir, String failureDir, boolean setTime) throws IOException, AccumuloException, AccumuloSecurityException, TableNotFoundException {
		long time = System.currentTimeMillis();
		Path importPath = new Path(dir);
		Path failurePath = new Path(failureDir);
		FileSystem fs = acu.getFileSystem();
		if (fs.isFile(importPath)) {
			throw new IOException("Import path must be a directory.");
		}
		if (fs.isFile(failurePath)) {
			throw new IOException("Failure path must be a directory.");
		}
		Path createPath = failurePath.suffix("/.createFile");
		FSDataOutputStream createStream = null;
		try {
			createStream = fs.create(createPath);
		} catch (IOException e) {
			throw new IOException("Error path is not writable.");
		} finally {
			if (createStream != null) {
				createStream.close();
			}
		}
		fs.delete(createPath, false);
		FileStatus[] failureChildStats = fs.listStatus(failurePath);
		if ((failureChildStats.length) > 0) {
			throw new IOException("Error path must be empty.");
		}
		for (FileStatus importStatus : fs.listStatus(importPath)) {
			try {
				FileSKVIterator importIterator = FileOperations.getInstance().newReaderBuilder().forFile(importStatus.getPath().toString(), fs, fs.getConf()).withTableConfiguration(AccumuloConfiguration.getDefaultConfiguration()).seekToBeginning().build();
				while (importIterator.hasTop()) {
					Key key = importIterator.getTopKey();
					Value value = importIterator.getTopValue();
					if (setTime) {
						key.setTimestamp(time);
					}
					Mutation mutation = new Mutation(key.getRow());
					if (!(key.isDeleted())) {
						mutation.put(key.getColumnFamily(), key.getColumnQualifier(), new ColumnVisibility(key.getColumnVisibilityData().toArray()), key.getTimestamp(), value);
					}else {
						mutation.putDelete(key.getColumnFamily(), key.getColumnQualifier(), new ColumnVisibility(key.getColumnVisibilityData().toArray()), key.getTimestamp());
					}
					importIterator.next();
				} 
			} catch (Exception e) {
				FSDataOutputStream failureWriter = null;
				DataInputStream failureReader = null;
				try {
					failureWriter = fs.create(failurePath.suffix(("/" + (importStatus.getPath().getName()))));
					failureReader = fs.open(importStatus.getPath());
					int read = 0;
					byte[] buffer = new byte[1024];
					while ((-1) != (read = failureReader.read(buffer))) {
						failureWriter.write(buffer, 0, read);
					} 
				} finally {
					if (failureReader != null)
						failureReader.close();

					if (failureWriter != null)
						failureWriter.close();

				}
			}
			fs.delete(importStatus.getPath(), true);
		}
	}

	@Override
	public void offline(String tableName) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		offline(tableName, false);
	}

	@Override
	public void offline(String tableName, boolean wait) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		if (!(exists(tableName)))
			throw new TableNotFoundException(tableName, tableName, "");

	}

	@Override
	public void online(String tableName) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		online(tableName, false);
	}

	@Override
	public void online(String tableName, boolean wait) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		if (!(exists(tableName)))
			throw new TableNotFoundException(tableName, tableName, "");

	}

	@Override
	public void clearLocatorCache(String tableName) throws TableNotFoundException {
		if (!(exists(tableName)))
			throw new TableNotFoundException(tableName, tableName, "");

	}

	@Override
	public Map<String, String> tableIdMap() {
		Map<String, String> result = new HashMap<>();
		return result;
	}

	@Override
	public List<DiskUsage> getDiskUsage(Set<String> tables) throws AccumuloException, AccumuloSecurityException {
		List<DiskUsage> diskUsages = new ArrayList<>();
		diskUsages.add(new DiskUsage(new TreeSet<>(tables), 0L));
		return diskUsages;
	}

	@Override
	public void merge(String tableName, Text start, Text end) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		if (!(exists(tableName)))
			throw new TableNotFoundException(tableName, tableName, "");

		acu.merge(tableName, start, end);
	}

	@Override
	public void deleteRows(String tableName, Text start, Text end) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		if (!(exists(tableName)))
			throw new TableNotFoundException(tableName, tableName, "");

		Text startText = (start != null) ? new Text(start) : new Text();
		if (((startText.getLength()) == 0) && (end == null)) {
			return;
		}
		startText.append(MockTableOperations.ZERO, 0, 1);
	}

	@Override
	public void compact(String tableName, Text start, Text end, boolean flush, boolean wait) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		if (!(exists(tableName)))
			throw new TableNotFoundException(tableName, tableName, "");

	}

	@Override
	public void compact(String tableName, Text start, Text end, List<IteratorSetting> iterators, boolean flush, boolean wait) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		if (!(exists(tableName)))
			throw new TableNotFoundException(tableName, tableName, "");

		if ((iterators != null) && ((iterators.size()) > 0))
			throw new UnsupportedOperationException();

	}

	@Override
	public void compact(String tableName, CompactionConfig config) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		if (!(exists(tableName)))
			throw new TableNotFoundException(tableName, tableName, "");

		if (((config.getIterators().size()) > 0) || ((config.getCompactionStrategy()) != null))
			throw new UnsupportedOperationException("Mock does not support iterators or compaction strategies for compactions");

	}

	@Override
	public void cancelCompaction(String tableName) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		if (!(exists(tableName)))
			throw new TableNotFoundException(tableName, tableName, "");

	}

	@Override
	public void clone(String srcTableName, String newTableName, boolean flush, Map<String, String> propertiesToSet, Set<String> propertiesToExclude) throws AccumuloException, AccumuloSecurityException, TableExistsException, TableNotFoundException {
		throw new NotImplementedException();
	}

	@Override
	public void flush(String tableName, Text start, Text end, boolean wait) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		if (!(exists(tableName)))
			throw new TableNotFoundException(tableName, tableName, "");

	}

	@Override
	public Text getMaxRow(String tableName, Authorizations auths, Text startRow, boolean startInclusive, Text endRow, boolean endInclusive) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		return null;
	}

	@Override
	public void importTable(String tableName, String exportDir) throws AccumuloException, AccumuloSecurityException, TableExistsException {
		throw new NotImplementedException();
	}

	@Override
	public void exportTable(String tableName, String exportDir) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		throw new NotImplementedException();
	}

	@Override
	public boolean testClassLoad(String tableName, String className, String asTypeName) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		try {
			AccumuloVFSClassLoader.loadClass(className, Class.forName(asTypeName));
		} catch (ClassNotFoundException e) {
			MockTableOperations.log.warn((((("Could not load class '" + className) + "' with type name '") + asTypeName) + "' in testClassLoad()."), e);
			return false;
		}
		return true;
	}

	@Override
	public void setSamplerConfiguration(String tableName, SamplerConfiguration samplerConfiguration) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearSamplerConfiguration(String tableName) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		throw new UnsupportedOperationException();
	}

	@Override
	public SamplerConfiguration getSamplerConfiguration(String tableName) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Locations locate(String tableName, Collection<Range> ranges) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		throw new UnsupportedOperationException();
	}
}

