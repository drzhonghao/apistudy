

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.ScannerImpl;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.PartialKey;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.metadata.MetadataTable;
import org.apache.accumulo.core.metadata.schema.DataFileValue;
import org.apache.accumulo.core.metadata.schema.MetadataSchema;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.util.ColumnFQ;
import org.apache.accumulo.fate.util.UtilWaitThread;
import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
import org.apache.accumulo.server.fs.FileRef;
import org.apache.accumulo.server.fs.VolumeManager;
import org.apache.accumulo.server.fs.VolumeManagerImpl;
import org.apache.accumulo.server.master.state.TServerInstance;
import org.apache.accumulo.server.util.MetadataTableUtil;
import org.apache.accumulo.server.zookeeper.ZooLock;
import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
import org.apache.hadoop.io.Text;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.DataFileColumnFamily.NAME;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.ServerColumnFamily.COMPACT_COLUMN;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.ServerColumnFamily.DIRECTORY_COLUMN;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.ServerColumnFamily.FLUSH_COLUMN;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.ServerColumnFamily.TIME_COLUMN;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.TabletColumnFamily.OLD_PREV_ROW_COLUMN;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.TabletColumnFamily.PREV_ROW_COLUMN;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.TabletColumnFamily.SPLIT_RATIO_COLUMN;


public class MasterMetadataUtil {
	private static final Logger log = LoggerFactory.getLogger(MasterMetadataUtil.class);

	public static void addNewTablet(ClientContext context, KeyExtent extent, String path, TServerInstance location, Map<FileRef, DataFileValue> datafileSizes, Map<Long, ? extends Collection<FileRef>> bulkLoadedFiles, String time, long lastFlushID, long lastCompactID, ZooLock zooLock) {
		Mutation m = extent.getPrevRowUpdateMutation();
		DIRECTORY_COLUMN.put(m, new Value(path.getBytes(StandardCharsets.UTF_8)));
		TIME_COLUMN.put(m, new Value(time.getBytes(StandardCharsets.UTF_8)));
		if (lastFlushID > 0)
			FLUSH_COLUMN.put(m, new Value(("" + lastFlushID).getBytes()));

		if (lastCompactID > 0)
			COMPACT_COLUMN.put(m, new Value(("" + lastCompactID).getBytes()));

		if (location != null) {
			location.putLocation(m);
			location.clearFutureLocation(m);
		}
		for (Map.Entry<FileRef, DataFileValue> entry : datafileSizes.entrySet()) {
			m.put(NAME, entry.getKey().meta(), new Value(entry.getValue().encode()));
		}
		for (Map.Entry<Long, ? extends Collection<FileRef>> entry : bulkLoadedFiles.entrySet()) {
			Value tidBytes = new Value(Long.toString(entry.getKey()).getBytes());
			for (FileRef ref : entry.getValue()) {
				m.put(MetadataSchema.TabletsSection.BulkFileColumnFamily.NAME, ref.meta(), new Value(tidBytes));
			}
		}
		MetadataTableUtil.update(context, zooLock, m, extent);
	}

	public static KeyExtent fixSplit(ClientContext context, Text metadataEntry, SortedMap<ColumnFQ, Value> columns, TServerInstance tserver, ZooLock lock) throws IOException, AccumuloException {
		MasterMetadataUtil.log.info((("Incomplete split " + metadataEntry) + " attempting to fix"));
		Value oper = columns.get(OLD_PREV_ROW_COLUMN);
		if ((columns.get(SPLIT_RATIO_COLUMN)) == null) {
			throw new IllegalArgumentException((("Metadata entry does not have split ratio (" + metadataEntry) + ")"));
		}
		double splitRatio = Double.parseDouble(new String(columns.get(SPLIT_RATIO_COLUMN).get(), StandardCharsets.UTF_8));
		Value prevEndRowIBW = columns.get(PREV_ROW_COLUMN);
		if (prevEndRowIBW == null) {
			throw new IllegalArgumentException((("Metadata entry does not have prev row (" + metadataEntry) + ")"));
		}
		Value time = columns.get(TIME_COLUMN);
		if (time == null) {
			throw new IllegalArgumentException((("Metadata entry does not have time (" + metadataEntry) + ")"));
		}
		Value flushID = columns.get(FLUSH_COLUMN);
		long initFlushID = -1;
		if (flushID != null)
			initFlushID = Long.parseLong(flushID.toString());

		Value compactID = columns.get(COMPACT_COLUMN);
		long initCompactID = -1;
		if (compactID != null)
			initCompactID = Long.parseLong(compactID.toString());

		Text metadataPrevEndRow = KeyExtent.decodePrevEndRow(prevEndRowIBW);
		String table = new KeyExtent(metadataEntry, ((Text) (null))).getTableId();
		return MasterMetadataUtil.fixSplit(context, table, metadataEntry, metadataPrevEndRow, oper, splitRatio, tserver, time.toString(), initFlushID, initCompactID, lock);
	}

	private static KeyExtent fixSplit(ClientContext context, String table, Text metadataEntry, Text metadataPrevEndRow, Value oper, double splitRatio, TServerInstance tserver, String time, long initFlushID, long initCompactID, ZooLock lock) throws IOException, AccumuloException {
		if (metadataPrevEndRow == null)
			throw new AccumuloException(("Split tablet does not have prev end row, something is amiss, extent = " + metadataEntry));

		Key prevRowKey = new Key(new Text(KeyExtent.getMetadataEntry(table, metadataPrevEndRow)));
		try (ScannerImpl scanner2 = new ScannerImpl(context, MetadataTable.ID, Authorizations.EMPTY)) {
			scanner2.setRange(new Range(prevRowKey, prevRowKey.followingKey(PartialKey.ROW)));
			VolumeManager fs = VolumeManagerImpl.get();
			if (!(scanner2.iterator().hasNext())) {
				MasterMetadataUtil.log.info(((("Rolling back incomplete split " + metadataEntry) + " ") + metadataPrevEndRow));
				MetadataTableUtil.rollBackSplit(metadataEntry, KeyExtent.decodePrevEndRow(oper), context, lock);
				return new KeyExtent(metadataEntry, KeyExtent.decodePrevEndRow(oper));
			}else {
				MasterMetadataUtil.log.info(((("Finishing incomplete split " + metadataEntry) + " ") + metadataPrevEndRow));
				List<FileRef> highDatafilesToRemove = new ArrayList<>();
				SortedMap<FileRef, DataFileValue> origDatafileSizes = new TreeMap<>();
				SortedMap<FileRef, DataFileValue> highDatafileSizes = new TreeMap<>();
				SortedMap<FileRef, DataFileValue> lowDatafileSizes = new TreeMap<>();
				try (Scanner scanner3 = new ScannerImpl(context, MetadataTable.ID, Authorizations.EMPTY)) {
					Key rowKey = new Key(metadataEntry);
					scanner3.fetchColumnFamily(NAME);
					scanner3.setRange(new Range(rowKey, rowKey.followingKey(PartialKey.ROW)));
					for (Map.Entry<Key, Value> entry : scanner3) {
						if ((entry.getKey().compareColumnFamily(NAME)) == 0) {
							origDatafileSizes.put(new FileRef(fs, entry.getKey()), new DataFileValue(entry.getValue().get()));
						}
					}
				}
				MetadataTableUtil.splitDatafiles(table, metadataPrevEndRow, splitRatio, new HashMap<FileRef, org.apache.accumulo.server.util.FileUtil.FileInfo>(), origDatafileSizes, lowDatafileSizes, highDatafileSizes, highDatafilesToRemove);
				MetadataTableUtil.finishSplit(metadataEntry, highDatafileSizes, highDatafilesToRemove, context, lock);
				return new KeyExtent(metadataEntry, KeyExtent.encodePrevEndRow(metadataPrevEndRow));
			}
		}
	}

	private static TServerInstance getTServerInstance(String address, ZooLock zooLock) {
		while (true) {
			try {
				return new TServerInstance(address, zooLock.getSessionId());
			} catch (KeeperException e) {
				MasterMetadataUtil.log.error("{}", e.getMessage(), e);
			} catch (InterruptedException e) {
				MasterMetadataUtil.log.error("{}", e.getMessage(), e);
			}
			UtilWaitThread.sleepUninterruptibly(1, TimeUnit.SECONDS);
		} 
	}

	public static void replaceDatafiles(ClientContext context, KeyExtent extent, Set<FileRef> datafilesToDelete, Set<FileRef> scanFiles, FileRef path, Long compactionId, DataFileValue size, String address, TServerInstance lastLocation, ZooLock zooLock) throws IOException {
		MasterMetadataUtil.replaceDatafiles(context, extent, datafilesToDelete, scanFiles, path, compactionId, size, address, lastLocation, zooLock, true);
	}

	public static void replaceDatafiles(ClientContext context, KeyExtent extent, Set<FileRef> datafilesToDelete, Set<FileRef> scanFiles, FileRef path, Long compactionId, DataFileValue size, String address, TServerInstance lastLocation, ZooLock zooLock, boolean insertDeleteFlags) throws IOException {
		if (insertDeleteFlags) {
			MetadataTableUtil.addDeleteEntries(extent, datafilesToDelete, context);
		}
		Mutation m = new Mutation(extent.getMetadataEntry());
		for (FileRef pathToRemove : datafilesToDelete)
			m.putDelete(NAME, pathToRemove.meta());

		for (FileRef scanFile : scanFiles)
			m.put(MetadataSchema.TabletsSection.ScanFileColumnFamily.NAME, scanFile.meta(), new Value(new byte[0]));

		if ((size.getNumEntries()) > 0)
			m.put(NAME, path.meta(), new Value(size.encode()));

		if (compactionId != null)
			COMPACT_COLUMN.put(m, new Value(("" + compactionId).getBytes()));

		TServerInstance self = MasterMetadataUtil.getTServerInstance(address, zooLock);
		self.putLastLocation(m);
		if ((lastLocation != null) && (!(lastLocation.equals(self))))
			lastLocation.clearLastLocation(m);

		MetadataTableUtil.update(context, zooLock, m, extent);
	}

	public static void updateTabletDataFile(ClientContext context, KeyExtent extent, FileRef path, FileRef mergeFile, DataFileValue dfv, String time, Set<FileRef> filesInUseByScans, String address, ZooLock zooLock, Set<String> unusedWalLogs, TServerInstance lastLocation, long flushId) {
		if (extent.isRootTablet()) {
			if (unusedWalLogs != null) {
				MasterMetadataUtil.updateRootTabletDataFile(extent, path, mergeFile, dfv, time, filesInUseByScans, address, zooLock, unusedWalLogs, lastLocation, flushId);
			}
			return;
		}
		Mutation m = MasterMetadataUtil.getUpdateForTabletDataFile(extent, path, mergeFile, dfv, time, filesInUseByScans, address, zooLock, unusedWalLogs, lastLocation, flushId);
		MetadataTableUtil.update(context, zooLock, m, extent);
	}

	private static void updateRootTabletDataFile(KeyExtent extent, FileRef path, FileRef mergeFile, DataFileValue dfv, String time, Set<FileRef> filesInUseByScans, String address, ZooLock zooLock, Set<String> unusedWalLogs, TServerInstance lastLocation, long flushId) {
		IZooReaderWriter zk = ZooReaderWriter.getInstance();
		for (String entry : unusedWalLogs) {
			String[] parts = entry.split("/");
			while (true) {
				break;
			} 
		}
	}

	private static Mutation getUpdateForTabletDataFile(KeyExtent extent, FileRef path, FileRef mergeFile, DataFileValue dfv, String time, Set<FileRef> filesInUseByScans, String address, ZooLock zooLock, Set<String> unusedWalLogs, TServerInstance lastLocation, long flushId) {
		Mutation m = new Mutation(extent.getMetadataEntry());
		if ((dfv.getNumEntries()) > 0) {
			m.put(NAME, path.meta(), new Value(dfv.encode()));
			TIME_COLUMN.put(m, new Value(time.getBytes(StandardCharsets.UTF_8)));
			TServerInstance self = MasterMetadataUtil.getTServerInstance(address, zooLock);
			self.putLastLocation(m);
			if ((lastLocation != null) && (!(lastLocation.equals(self))))
				lastLocation.clearLastLocation(m);

		}
		if (unusedWalLogs != null) {
			for (String entry : unusedWalLogs) {
				m.putDelete(MetadataSchema.TabletsSection.LogColumnFamily.NAME, new Text(entry));
			}
		}
		for (FileRef scanFile : filesInUseByScans)
			m.put(MetadataSchema.TabletsSection.ScanFileColumnFamily.NAME, scanFile.meta(), new Value(new byte[0]));

		if (mergeFile != null)
			m.putDelete(NAME, mergeFile.meta());

		FLUSH_COLUMN.put(m, new Value(Long.toString(flushId).getBytes(StandardCharsets.UTF_8)));
		return m;
	}
}

