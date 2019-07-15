

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.InstanceOperations;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.DefaultConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.metadata.MetadataTable;
import org.apache.accumulo.core.metadata.schema.MetadataSchema;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.util.ColumnFQ;
import org.apache.accumulo.core.volume.Volume;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.ExportTable;
import org.apache.accumulo.master.tableOps.MasterRepo;
import org.apache.accumulo.server.AccumuloServerContext;
import org.apache.accumulo.server.ServerConstants;
import org.apache.accumulo.server.conf.ServerConfigurationFactory;
import org.apache.accumulo.server.conf.TableConfiguration;
import org.apache.accumulo.server.fs.VolumeManager;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;

import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.CurrentLocationColumnFamily.NAME;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.ServerColumnFamily.TIME_COLUMN;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.TabletColumnFamily.PREV_ROW_COLUMN;


class WriteExportFiles extends MasterRepo {
	private static final long serialVersionUID = 1L;

	private void checkOffline(Connector conn) throws Exception {
	}

	@Override
	public long isReady(long tid, Master master) throws Exception {
		Connector conn = master.getConnector();
		checkOffline(conn);
		Scanner metaScanner = conn.createScanner(MetadataTable.NAME, Authorizations.EMPTY);
		metaScanner.fetchColumnFamily(NAME);
		metaScanner.fetchColumnFamily(MetadataSchema.TabletsSection.FutureLocationColumnFamily.NAME);
		if (metaScanner.iterator().hasNext()) {
			return 500;
		}
		metaScanner.clearColumns();
		metaScanner.fetchColumnFamily(MetadataSchema.TabletsSection.LogColumnFamily.NAME);
		if (metaScanner.iterator().hasNext()) {
		}
		return 0;
	}

	@Override
	public Repo<Master> call(long tid, Master master) throws Exception {
		return null;
	}

	@Override
	public void undo(long tid, Master env) throws Exception {
	}

	public static void exportTable(VolumeManager fs, AccumuloServerContext context, String tableName, String tableID, String exportDir) throws Exception {
		fs.mkdirs(new Path(exportDir));
		Path exportMetaFilePath = fs.getVolumeByPath(new Path(exportDir)).getFileSystem().makeQualified(new Path(exportDir, Constants.EXPORT_FILE));
		FSDataOutputStream fileOut = fs.create(exportMetaFilePath, false);
		ZipOutputStream zipOut = new ZipOutputStream(fileOut);
		BufferedOutputStream bufOut = new BufferedOutputStream(zipOut);
		DataOutputStream dataOut = new DataOutputStream(bufOut);
		try {
			zipOut.putNextEntry(new ZipEntry(Constants.EXPORT_INFO_FILE));
			OutputStreamWriter osw = new OutputStreamWriter(dataOut, StandardCharsets.UTF_8);
			osw.append(((((ExportTable.EXPORT_VERSION_PROP) + ":") + (ExportTable.VERSION)) + "\n"));
			osw.append((("srcInstanceName:" + (context.getInstance().getInstanceName())) + "\n"));
			osw.append((("srcInstanceID:" + (context.getInstance().getInstanceID())) + "\n"));
			osw.append((("srcZookeepers:" + (context.getInstance().getZooKeepers())) + "\n"));
			osw.append((("srcTableName:" + tableName) + "\n"));
			osw.append((("srcTableID:" + tableID) + "\n"));
			osw.append(((((ExportTable.DATA_VERSION_PROP) + ":") + (ServerConstants.DATA_VERSION)) + "\n"));
			osw.append((("srcCodeVersion:" + (Constants.VERSION)) + "\n"));
			osw.flush();
			dataOut.flush();
			WriteExportFiles.exportConfig(context, tableID, zipOut, dataOut);
			dataOut.flush();
			Map<String, String> uniqueFiles = WriteExportFiles.exportMetadata(fs, context, tableID, zipOut, dataOut);
			dataOut.close();
			dataOut = null;
			WriteExportFiles.createDistcpFile(fs, exportDir, exportMetaFilePath, uniqueFiles);
		} finally {
			if (dataOut != null)
				dataOut.close();

		}
	}

	private static void createDistcpFile(VolumeManager fs, String exportDir, Path exportMetaFilePath, Map<String, String> uniqueFiles) throws IOException {
		BufferedWriter distcpOut = new BufferedWriter(new OutputStreamWriter(fs.create(new Path(exportDir, "distcp.txt"), false), StandardCharsets.UTF_8));
		try {
			for (String file : uniqueFiles.values()) {
				distcpOut.append(file);
				distcpOut.newLine();
			}
			distcpOut.append(exportMetaFilePath.toString());
			distcpOut.newLine();
			distcpOut.close();
			distcpOut = null;
		} finally {
			if (distcpOut != null)
				distcpOut.close();

		}
	}

	private static Map<String, String> exportMetadata(VolumeManager fs, AccumuloServerContext context, String tableID, ZipOutputStream zipOut, DataOutputStream dataOut) throws IOException, AccumuloException, AccumuloSecurityException, TableNotFoundException {
		zipOut.putNextEntry(new ZipEntry(Constants.EXPORT_METADATA_FILE));
		Map<String, String> uniqueFiles = new HashMap<>();
		Scanner metaScanner = context.getConnector().createScanner(MetadataTable.NAME, Authorizations.EMPTY);
		metaScanner.fetchColumnFamily(MetadataSchema.TabletsSection.DataFileColumnFamily.NAME);
		PREV_ROW_COLUMN.fetch(metaScanner);
		TIME_COLUMN.fetch(metaScanner);
		metaScanner.setRange(new KeyExtent(tableID, null, null).toMetadataRange());
		for (Map.Entry<Key, Value> entry : metaScanner) {
			entry.getKey().write(dataOut);
			entry.getValue().write(dataOut);
			if (entry.getKey().getColumnFamily().equals(MetadataSchema.TabletsSection.DataFileColumnFamily.NAME)) {
				String path = fs.getFullPath(entry.getKey()).toString();
				String[] tokens = path.split("/");
				if ((tokens.length) < 1) {
					throw new RuntimeException(("Illegal path " + path));
				}
				String filename = tokens[((tokens.length) - 1)];
				String existingPath = uniqueFiles.get(filename);
				if (existingPath == null) {
					uniqueFiles.put(filename, path);
				}else
					if (!(existingPath.equals(path))) {
						throw new IOException((("Cannot export table with nonunique file names " + filename) + ". Major compact table."));
					}

			}
		}
		return uniqueFiles;
	}

	private static void exportConfig(AccumuloServerContext context, String tableID, ZipOutputStream zipOut, DataOutputStream dataOut) throws IOException, AccumuloException, AccumuloSecurityException, TableNotFoundException {
		Connector conn = context.getConnector();
		DefaultConfiguration defaultConfig = AccumuloConfiguration.getDefaultConfiguration();
		Map<String, String> siteConfig = conn.instanceOperations().getSiteConfiguration();
		Map<String, String> systemConfig = conn.instanceOperations().getSystemConfiguration();
		TableConfiguration tableConfig = context.getServerConfigurationFactory().getTableConfiguration(tableID);
		OutputStreamWriter osw = new OutputStreamWriter(dataOut, StandardCharsets.UTF_8);
		zipOut.putNextEntry(new ZipEntry(Constants.EXPORT_TABLE_CONFIG_FILE));
		for (Map.Entry<String, String> prop : tableConfig) {
			if (prop.getKey().startsWith(Property.TABLE_PREFIX.getKey())) {
				Property key = Property.getPropertyByKey(prop.getKey());
				if ((key == null) || (!(defaultConfig.get(key).equals(prop.getValue())))) {
					if ((!(prop.getValue().equals(siteConfig.get(prop.getKey())))) && (!(prop.getValue().equals(systemConfig.get(prop.getKey()))))) {
						osw.append(((((prop.getKey()) + "=") + (prop.getValue())) + "\n"));
					}
				}
			}
		}
		osw.flush();
	}
}

