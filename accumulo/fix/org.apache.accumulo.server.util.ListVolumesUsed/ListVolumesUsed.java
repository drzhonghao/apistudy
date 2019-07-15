

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.metadata.MetadataTable;
import org.apache.accumulo.core.metadata.RootTable;
import org.apache.accumulo.core.metadata.schema.MetadataSchema;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.tabletserver.log.LogEntry;
import org.apache.accumulo.core.util.ColumnFQ;
import org.apache.accumulo.server.AccumuloServerContext;
import org.apache.accumulo.server.client.HdfsZooInstance;
import org.apache.accumulo.server.conf.ServerConfigurationFactory;
import org.apache.accumulo.server.fs.VolumeManager;
import org.apache.accumulo.server.log.WalStateManager;
import org.apache.accumulo.server.util.MetadataTableUtil;
import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;

import static org.apache.accumulo.core.metadata.schema.MetadataSchema.DeletesSection.getRange;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.DeletesSection.getRowPrefix;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.DataFileColumnFamily.NAME;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.ServerColumnFamily.DIRECTORY_COLUMN;
import static org.apache.accumulo.server.fs.VolumeManager.FileType.TABLE;
import static org.apache.accumulo.server.fs.VolumeManager.FileType.WAL;


public class ListVolumesUsed {
	public static void main(String[] args) throws Exception {
		ListVolumesUsed.listVolumes(new AccumuloServerContext(new ServerConfigurationFactory(HdfsZooInstance.getInstance())));
	}

	private static String getTableURI(String rootTabletDir) {
		Path ret = TABLE.getVolume(new Path(rootTabletDir));
		if (ret == null)
			return "RELATIVE";

		return ret.toString();
	}

	private static String getLogURI(String logEntry) {
		Path ret = WAL.getVolume(new Path(logEntry));
		if (ret == null)
			return "RELATIVE";

		return ret.toString();
	}

	private static void getLogURIs(TreeSet<String> volumes, LogEntry logEntry) {
		volumes.add(ListVolumesUsed.getLogURI(logEntry.filename));
	}

	private static void listZookeeper() throws Exception {
		System.out.println("Listing volumes referenced in zookeeper");
		TreeSet<String> volumes = new TreeSet<>();
		volumes.add(ListVolumesUsed.getTableURI(MetadataTableUtil.getRootTabletDir()));
		ArrayList<LogEntry> result = new ArrayList<>();
		for (LogEntry logEntry : result) {
			ListVolumesUsed.getLogURIs(volumes, logEntry);
		}
		for (String volume : volumes)
			System.out.println(("\tVolume : " + volume));

	}

	private static void listTable(String name, Connector conn) throws Exception {
		System.out.println((("Listing volumes referenced in " + name) + " tablets section"));
		Scanner scanner = conn.createScanner(name, Authorizations.EMPTY);
		scanner.setRange(MetadataSchema.TabletsSection.getRange());
		scanner.fetchColumnFamily(NAME);
		scanner.fetchColumnFamily(MetadataSchema.TabletsSection.LogColumnFamily.NAME);
		DIRECTORY_COLUMN.fetch(scanner);
		TreeSet<String> volumes = new TreeSet<>();
		for (Map.Entry<Key, Value> entry : scanner) {
			if (entry.getKey().getColumnFamily().equals(NAME)) {
				volumes.add(ListVolumesUsed.getTableURI(entry.getKey().getColumnQualifier().toString()));
			}else
				if (entry.getKey().getColumnFamily().equals(MetadataSchema.TabletsSection.LogColumnFamily.NAME)) {
					LogEntry le = LogEntry.fromKeyValue(entry.getKey(), entry.getValue());
					ListVolumesUsed.getLogURIs(volumes, le);
				}else
					if (DIRECTORY_COLUMN.hasColumns(entry.getKey())) {
						volumes.add(ListVolumesUsed.getTableURI(entry.getValue().toString()));
					}


		}
		for (String volume : volumes)
			System.out.println(("\tVolume : " + volume));

		volumes.clear();
		scanner.clearColumns();
		scanner.setRange(getRange());
		for (Map.Entry<Key, Value> entry : scanner) {
			String delPath = entry.getKey().getRow().toString().substring(getRowPrefix().length());
			volumes.add(ListVolumesUsed.getTableURI(delPath));
		}
		System.out.println((("Listing volumes referenced in " + name) + " deletes section (volume replacement occurrs at deletion time)"));
		for (String volume : volumes)
			System.out.println(("\tVolume : " + volume));

		volumes.clear();
		WalStateManager wals = new WalStateManager(conn.getInstance(), ZooReaderWriter.getInstance());
		for (Path path : wals.getAllState().keySet()) {
			volumes.add(ListVolumesUsed.getLogURI(path.toString()));
		}
		System.out.println((("Listing volumes referenced in " + name) + " current logs"));
		for (String volume : volumes)
			System.out.println(("\tVolume : " + volume));

	}

	public static void listVolumes(ClientContext context) throws Exception {
		Connector conn = context.getConnector();
		ListVolumesUsed.listZookeeper();
		System.out.println();
		ListVolumesUsed.listTable(RootTable.NAME, conn);
		System.out.println();
		ListVolumesUsed.listTable(MetadataTable.NAME, conn);
	}
}

