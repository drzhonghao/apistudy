

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.IsolatedScanner;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.master.thrift.BulkImportState;
import org.apache.accumulo.core.metadata.MetadataTable;
import org.apache.accumulo.core.metadata.schema.MetadataSchema;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.BulkImport;
import org.apache.accumulo.master.tableOps.MasterRepo;
import org.apache.accumulo.server.AccumuloServerContext;
import org.apache.accumulo.server.fs.FileRef;
import org.apache.accumulo.server.fs.VolumeManager;
import org.apache.accumulo.server.master.LiveTServerSet;
import org.apache.accumulo.server.master.state.TServerInstance;
import org.apache.accumulo.server.zookeeper.DistributedWorkQueue;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.BulkFileColumnFamily.NAME;


class CopyFailed extends MasterRepo {
	private static final Logger log = LoggerFactory.getLogger(CopyFailed.class);

	private static final long serialVersionUID = 1L;

	private String tableId;

	private String source;

	private String bulk;

	private String error;

	public CopyFailed(String tableId, String source, String bulk, String error) {
		this.tableId = tableId;
		this.source = source;
		this.bulk = bulk;
		this.error = error;
	}

	@Override
	public long isReady(long tid, Master master) throws Exception {
		Set<TServerInstance> finished = new HashSet<>();
		Set<TServerInstance> running = master.onlineTabletServers();
		for (TServerInstance server : running) {
			try {
				LiveTServerSet.TServerConnection client = master.getConnection(server);
				if ((client != null) && (!(client.isActive(tid))))
					finished.add(server);

			} catch (TException ex) {
				CopyFailed.log.info(((((("Ignoring error trying to check on tid " + tid) + " from server ") + server) + ": ") + ex));
			}
		}
		if (finished.containsAll(running))
			return 0;

		return 500;
	}

	@Override
	public Repo<Master> call(long tid, Master master) throws Exception {
		master.updateBulkImportStatus(source, BulkImportState.COPY_FILES);
		VolumeManager fs = master.getFileSystem();
		if (!(fs.exists(new Path(error, BulkImport.FAILURES_TXT)))) {
		}
		HashMap<FileRef, String> failures = new HashMap<>();
		HashMap<FileRef, String> loadedFailures = new HashMap<>();
		try (final BufferedReader in = new BufferedReader(new InputStreamReader(fs.open(new Path(error, BulkImport.FAILURES_TXT)), StandardCharsets.UTF_8))) {
			String line = null;
			while ((line = in.readLine()) != null) {
				Path path = new Path(line);
				if (!(fs.exists(new Path(error, path.getName()))))
					failures.put(new FileRef(line, path), line);

			} 
		}
		Connector conn = master.getConnector();
		try (final Scanner mscanner = new IsolatedScanner(conn.createScanner(MetadataTable.NAME, Authorizations.EMPTY))) {
			mscanner.setRange(new KeyExtent(tableId, null, null).toMetadataRange());
			mscanner.fetchColumnFamily(NAME);
			for (Map.Entry<Key, Value> entry : mscanner) {
				if ((Long.parseLong(entry.getValue().toString())) == tid) {
					FileRef loadedFile = new FileRef(fs, entry.getKey());
					String absPath = failures.remove(loadedFile);
					if (absPath != null) {
						loadedFailures.put(loadedFile, absPath);
					}
				}
			}
		}
		for (String failure : failures.values()) {
			Path orig = new Path(failure);
			Path dest = new Path(error, orig.getName());
			fs.rename(orig, dest);
			CopyFailed.log.debug((((((("tid " + tid) + " renamed ") + orig) + " to ") + dest) + ": import failed"));
		}
		if ((loadedFailures.size()) > 0) {
			DistributedWorkQueue bifCopyQueue = new DistributedWorkQueue(((((Constants.ZROOT) + "/") + (master.getInstance().getInstanceID())) + (Constants.ZBULK_FAILED_COPYQ)), master.getConfiguration());
			HashSet<String> workIds = new HashSet<>();
			for (String failure : loadedFailures.values()) {
				Path orig = new Path(failure);
				Path dest = new Path(error, orig.getName());
				if (fs.exists(dest))
					continue;

				bifCopyQueue.addWork(orig.getName(), ((failure + ",") + dest).getBytes(StandardCharsets.UTF_8));
				workIds.add(orig.getName());
				CopyFailed.log.debug((((((("tid " + tid) + " added to copyq: ") + orig) + " to ") + dest) + ": failed"));
			}
			bifCopyQueue.waitUntilDone(workIds);
		}
		fs.deleteRecursively(new Path(error, BulkImport.FAILURES_TXT));
		return null;
	}
}

