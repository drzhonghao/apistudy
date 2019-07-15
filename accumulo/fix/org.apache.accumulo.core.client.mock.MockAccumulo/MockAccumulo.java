

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.accumulo.core.client.BatchScanner;
import org.apache.accumulo.core.client.admin.TimeType;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.client.mock.MockBatchScanner;
import org.apache.accumulo.core.client.mock.MockNamespace;
import org.apache.accumulo.core.client.mock.MockTable;
import org.apache.accumulo.core.client.mock.MockUser;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.util.Pair;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.Text;


@Deprecated
public class MockAccumulo {
	final Map<String, MockTable> tables = new HashMap<>();

	final Map<String, MockNamespace> namespaces = new HashMap<>();

	final Map<String, String> systemProperties = new HashMap<>();

	Map<String, MockUser> users = new HashMap<>();

	final FileSystem fs = null;

	final AtomicInteger tableIdCounter = new AtomicInteger(0);

	public FileSystem getFileSystem() {
		return fs;
	}

	void setProperty(String key, String value) {
		systemProperties.put(key, value);
	}

	String removeProperty(String key) {
		return systemProperties.remove(key);
	}

	public void addMutation(String table, Mutation m) {
		MockTable t = tables.get(table);
	}

	public BatchScanner createBatchScanner(String tableName, Authorizations authorizations) {
		return new MockBatchScanner(tables.get(tableName), authorizations);
	}

	public void createTable(String username, String tableName, boolean useVersions, TimeType timeType) {
		Map<String, String> opts = Collections.emptyMap();
		createTable(username, tableName, useVersions, timeType, opts);
	}

	public void createTable(String username, String tableName, boolean useVersions, TimeType timeType, Map<String, String> properties) {
		String namespace = Tables.qualify(tableName).getFirst();
		if (!(namespaceExists(namespace))) {
			return;
		}
		MockNamespace n = namespaces.get(namespace);
	}

	public void createTable(String username, String tableName, TimeType timeType, Map<String, String> properties) {
		String namespace = Tables.qualify(tableName).getFirst();
		HashMap<String, String> props = new HashMap<>(properties);
		if (!(namespaceExists(namespace))) {
			return;
		}
		MockNamespace n = namespaces.get(namespace);
		MockTable t = new MockTable(n, timeType, Integer.toString(tableIdCounter.incrementAndGet()), props);
		t.setNamespaceName(namespace);
		t.setNamespace(n);
		tables.put(tableName, t);
	}

	public void createNamespace(String username, String namespace) {
		if (!(namespaceExists(namespace))) {
			MockNamespace n = new MockNamespace();
			namespaces.put(namespace, n);
		}
	}

	public void addSplits(String tableName, SortedSet<Text> partitionKeys) {
		tables.get(tableName).addSplits(partitionKeys);
	}

	public Collection<Text> getSplits(String tableName) {
		return tables.get(tableName).getSplits();
	}

	public void merge(String tableName, Text start, Text end) {
		tables.get(tableName).merge(start, end);
	}

	private boolean namespaceExists(String namespace) {
		return namespaces.containsKey(namespace);
	}
}

