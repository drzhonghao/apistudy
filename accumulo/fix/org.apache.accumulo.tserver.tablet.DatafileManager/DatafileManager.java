

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.metadata.schema.DataFileValue;
import org.apache.accumulo.core.replication.ReplicationConfigurationUtil;
import org.apache.accumulo.core.trace.Span;
import org.apache.accumulo.core.trace.Trace;
import org.apache.accumulo.core.util.MapCounter;
import org.apache.accumulo.core.util.Pair;
import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
import org.apache.accumulo.server.ServerConstants;
import org.apache.accumulo.server.conf.TableConfiguration;
import org.apache.accumulo.server.fs.FileRef;
import org.apache.accumulo.server.fs.VolumeManager;
import org.apache.accumulo.server.master.state.TServerInstance;
import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
import org.apache.accumulo.tserver.TLevel;
import org.apache.accumulo.tserver.tablet.CommitSession;
import org.apache.accumulo.tserver.tablet.Tablet;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;


class DatafileManager {
	private final Logger log = Logger.getLogger(DatafileManager.class);

	private final Map<FileRef, DataFileValue> datafileSizes = Collections.synchronizedMap(new TreeMap<FileRef, DataFileValue>());

	private final Tablet tablet;

	private Long maxMergingMinorCompactionFileSize;

	private final Object bulkFileImportLock = new Object();

	DatafileManager(Tablet tablet, SortedMap<FileRef, DataFileValue> datafileSizes) {
		for (Map.Entry<FileRef, DataFileValue> datafiles : datafileSizes.entrySet()) {
			this.datafileSizes.put(datafiles.getKey(), datafiles.getValue());
		}
		this.tablet = tablet;
	}

	private FileRef mergingMinorCompactionFile = null;

	private final Set<FileRef> filesToDeleteAfterScan = new HashSet<>();

	private final Map<Long, Set<FileRef>> scanFileReservations = new HashMap<>();

	private final MapCounter<FileRef> fileScanReferenceCounts = new MapCounter<>();

	private long nextScanReservationId = 0;

	private boolean reservationsBlocked = false;

	private final Set<FileRef> majorCompactingFiles = new HashSet<>();

	static void rename(VolumeManager fs, Path src, Path dst) throws IOException {
		if (!(fs.rename(src, dst))) {
			throw new IOException((((("Rename " + src) + " to ") + dst) + " returned false "));
		}
	}

	Pair<Long, Map<FileRef, DataFileValue>> reserveFilesForScan() {
		synchronized(tablet) {
			while (reservationsBlocked) {
				try {
					tablet.wait(50);
				} catch (InterruptedException e) {
					log.warn(e, e);
				}
			} 
			Set<FileRef> absFilePaths = new HashSet<>(datafileSizes.keySet());
			long rid = (nextScanReservationId)++;
			scanFileReservations.put(rid, absFilePaths);
			Map<FileRef, DataFileValue> ret = new HashMap<>();
			for (FileRef path : absFilePaths) {
				fileScanReferenceCounts.increment(path, 1);
				ret.put(path, datafileSizes.get(path));
			}
			return new Pair<>(rid, ret);
		}
	}

	void returnFilesForScan(Long reservationId) {
		final Set<FileRef> filesToDelete = new HashSet<>();
		synchronized(tablet) {
			Set<FileRef> absFilePaths = scanFileReservations.remove(reservationId);
			if (absFilePaths == null)
				throw new IllegalArgumentException(("Unknown scan reservation id " + reservationId));

			boolean notify = false;
			for (FileRef path : absFilePaths) {
				long refCount = fileScanReferenceCounts.decrement(path, 1);
				if (refCount == 0) {
					if (filesToDeleteAfterScan.remove(path))
						filesToDelete.add(path);

					notify = true;
				}else
					if (refCount < 0)
						throw new IllegalStateException(((("Scan ref count for " + path) + " is ") + refCount));


			}
			if (notify)
				tablet.notifyAll();

		}
		if ((filesToDelete.size()) > 0) {
			log.debug(((("Removing scan refs from metadata " + (tablet.getExtent())) + " ") + filesToDelete));
		}
	}

	void removeFilesAfterScan(Set<FileRef> scanFiles) {
		if ((scanFiles.size()) == 0)
			return;

		Set<FileRef> filesToDelete = new HashSet<>();
		synchronized(tablet) {
			for (FileRef path : scanFiles) {
				if ((fileScanReferenceCounts.get(path)) == 0)
					filesToDelete.add(path);
				else
					filesToDeleteAfterScan.add(path);

			}
		}
		if ((filesToDelete.size()) > 0) {
			log.debug(((("Removing scan refs from metadata " + (tablet.getExtent())) + " ") + filesToDelete));
		}
	}

	private TreeSet<FileRef> waitForScansToFinish(Set<FileRef> pathsToWaitFor, boolean blockNewScans, long maxWaitTime) {
		long startTime = System.currentTimeMillis();
		TreeSet<FileRef> inUse = new TreeSet<>();
		Span waitForScans = Trace.start("waitForScans");
		try {
			synchronized(tablet) {
				if (blockNewScans) {
					if (reservationsBlocked)
						throw new IllegalStateException();

					reservationsBlocked = true;
				}
				for (FileRef path : pathsToWaitFor) {
					while (((fileScanReferenceCounts.get(path)) > 0) && (((System.currentTimeMillis()) - startTime) < maxWaitTime)) {
						try {
							tablet.wait(100);
						} catch (InterruptedException e) {
							log.warn(e, e);
						}
					} 
				}
				for (FileRef path : pathsToWaitFor) {
					if ((fileScanReferenceCounts.get(path)) > 0)
						inUse.add(path);

				}
				if (blockNewScans) {
					reservationsBlocked = false;
					tablet.notifyAll();
				}
			}
		} finally {
			waitForScans.stop();
		}
		return inUse;
	}

	public void importMapFiles(long tid, Map<FileRef, DataFileValue> pathsString, boolean setTime) throws IOException {
		String bulkDir = null;
		Map<FileRef, DataFileValue> paths = new HashMap<>();
		for (Map.Entry<FileRef, DataFileValue> entry : pathsString.entrySet())
			paths.put(entry.getKey(), entry.getValue());

		for (FileRef tpath : paths.keySet()) {
			boolean inTheRightDirectory = false;
			Path parent = tpath.path().getParent().getParent();
			for (String tablesDir : ServerConstants.getTablesDirs()) {
				if (parent.equals(new Path(tablesDir, tablet.getExtent().getTableId()))) {
					inTheRightDirectory = true;
					break;
				}
			}
			if (!inTheRightDirectory) {
				throw new IOException((("Data file " + tpath) + " not in table dirs"));
			}
			if (bulkDir == null)
				bulkDir = tpath.path().getParent().toString();
			else
				if (!(bulkDir.equals(tpath.path().getParent().toString())))
					throw new IllegalArgumentException(((("bulk files in different dirs " + bulkDir) + " ") + tpath));


		}
		if (tablet.getExtent().isMeta()) {
			throw new IllegalArgumentException("Can not import files to a metadata tablet");
		}
		synchronized(bulkFileImportLock) {
			if ((paths.size()) > 0) {
				long bulkTime = Long.MIN_VALUE;
				if (setTime) {
					for (DataFileValue dfv : paths.values()) {
						long nextTime = tablet.getAndUpdateTime();
						if (nextTime < bulkTime)
							throw new IllegalStateException(((("Time went backwards unexpectedly " + nextTime) + " ") + bulkTime));

						bulkTime = nextTime;
						dfv.setTime(bulkTime);
					}
				}
				tablet.updatePersistedTime(bulkTime, paths, tid);
			}
		}
		synchronized(tablet) {
			for (Map.Entry<FileRef, DataFileValue> tpath : paths.entrySet()) {
				if (datafileSizes.containsKey(tpath.getKey())) {
					log.error(("Adding file that is already in set " + (tpath.getKey())));
				}
				datafileSizes.put(tpath.getKey(), tpath.getValue());
			}
		}
		for (Map.Entry<FileRef, DataFileValue> entry : paths.entrySet()) {
			log.log(TLevel.TABLET_HIST, (((((tablet.getExtent()) + " import ") + (entry.getKey())) + " ") + (entry.getValue())));
		}
	}

	FileRef reserveMergingMinorCompactionFile() {
		if ((mergingMinorCompactionFile) != null)
			throw new IllegalStateException(("Tried to reserve merging minor compaction file when already reserved  : " + (mergingMinorCompactionFile)));

		if (tablet.getExtent().isRootTablet())
			return null;

		int maxFiles = tablet.getTableConfiguration().getMaxFilesPerTablet();
		if (((majorCompactingFiles.size()) > 0) && ((datafileSizes.size()) == maxFiles))
			return null;

		if ((datafileSizes.size()) >= maxFiles) {
			long maxFileSize = Long.MAX_VALUE;
			maxMergingMinorCompactionFileSize = AccumuloConfiguration.getMemoryInBytes(tablet.getTableConfiguration().get(Property.TABLE_MINC_MAX_MERGE_FILE_SIZE));
			if ((maxMergingMinorCompactionFileSize) > 0) {
				maxFileSize = maxMergingMinorCompactionFileSize;
			}
			long min = maxFileSize;
			FileRef minName = null;
			for (Map.Entry<FileRef, DataFileValue> entry : datafileSizes.entrySet()) {
				if (((entry.getValue().getSize()) <= min) && (!(majorCompactingFiles.contains(entry.getKey())))) {
					min = entry.getValue().getSize();
					minName = entry.getKey();
				}
			}
			if (minName == null)
				return null;

			mergingMinorCompactionFile = minName;
			return minName;
		}
		return null;
	}

	void unreserveMergingMinorCompactionFile(FileRef file) {
		if ((((file == null) && ((mergingMinorCompactionFile) != null)) || ((file != null) && ((mergingMinorCompactionFile) == null))) || (((file != null) && ((mergingMinorCompactionFile) != null)) && (!(file.equals(mergingMinorCompactionFile)))))
			throw new IllegalStateException(((("Disagreement " + file) + " ") + (mergingMinorCompactionFile)));

		mergingMinorCompactionFile = null;
	}

	void bringMinorCompactionOnline(FileRef tmpDatafile, FileRef newDatafile, FileRef absMergeFile, DataFileValue dfv, CommitSession commitSession, long flushId) throws IOException {
		IZooReaderWriter zoo = ZooReaderWriter.getInstance();
		if (tablet.getExtent().isRootTablet()) {
			try {
			} catch (Exception e) {
				throw new IllegalStateException("Can not bring major compaction online, lock not held", e);
			}
		}
		do {
			if ((dfv.getNumEntries()) == 0) {
			}else {
			}
			break;
		} while (true );
		long t1;
		long t2;
		Set<FileRef> filesInUseByScans = Collections.emptySet();
		if (absMergeFile != null)
			filesInUseByScans = Collections.singleton(absMergeFile);

		if (absMergeFile != null) {
		}
		boolean replicate = ReplicationConfigurationUtil.isEnabled(tablet.getExtent(), tablet.getTableConfiguration());
		Set<String> logFileOnly = null;
		if (replicate) {
			logFileOnly = new HashSet<>();
		}
		try {
			if (replicate) {
				if (log.isDebugEnabled()) {
					log.debug(((("Recording that data has been ingested into " + (tablet.getExtent())) + " using ") + logFileOnly));
				}
				for (String logFile : logFileOnly) {
				}
			}
		} finally {
		}
		do {
			break;
		} while (true );
		synchronized(tablet) {
			t1 = System.currentTimeMillis();
			if (datafileSizes.containsKey(newDatafile)) {
				log.error(("Adding file that is already in set " + newDatafile));
			}
			if ((dfv.getNumEntries()) > 0) {
				datafileSizes.put(newDatafile, dfv);
			}
			if (absMergeFile != null) {
				datafileSizes.remove(absMergeFile);
			}
			unreserveMergingMinorCompactionFile(absMergeFile);
			tablet.flushComplete(flushId);
			t2 = System.currentTimeMillis();
		}
		removeFilesAfterScan(filesInUseByScans);
		if (absMergeFile != null)
			log.log(TLevel.TABLET_HIST, (((((tablet.getExtent()) + " MinC [") + absMergeFile) + ",memory] -> ") + newDatafile));
		else
			log.log(TLevel.TABLET_HIST, (((tablet.getExtent()) + " MinC [memory] -> ") + newDatafile));

		log.debug(String.format("MinC finish lock %.2f secs %s", ((t2 - t1) / 1000.0), tablet.getExtent().toString()));
		long splitSize = tablet.getTableConfiguration().getMemoryInBytes(Property.TABLE_SPLIT_THRESHOLD);
		if ((dfv.getSize()) > splitSize) {
			log.debug(String.format(("Minor Compaction wrote out file larger than split threshold." + " split threshold = %,d  file size = %,d"), splitSize, dfv.getSize()));
		}
	}

	public void reserveMajorCompactingFiles(Collection<FileRef> files) {
		if ((majorCompactingFiles.size()) != 0)
			throw new IllegalStateException(("Major compacting files not empty " + (majorCompactingFiles)));

		if (((mergingMinorCompactionFile) != null) && (files.contains(mergingMinorCompactionFile)))
			throw new IllegalStateException(("Major compaction tried to resrve file in use by minor compaction " + (mergingMinorCompactionFile)));

		majorCompactingFiles.addAll(files);
	}

	public void clearMajorCompactingFile() {
		majorCompactingFiles.clear();
	}

	void bringMajorCompactionOnline(Set<FileRef> oldDatafiles, FileRef tmpDatafile, FileRef newDatafile, Long compactionId, DataFileValue dfv) throws IOException {
		final KeyExtent extent = tablet.getExtent();
		long t1;
		long t2;
		if (!(extent.isRootTablet())) {
			if ((dfv.getNumEntries()) == 0) {
			}
		}
		TServerInstance lastLocation = null;
		synchronized(tablet) {
			t1 = System.currentTimeMillis();
			IZooReaderWriter zoo = ZooReaderWriter.getInstance();
			tablet.incrementDataSourceDeletions();
			if (extent.isRootTablet()) {
				waitForScansToFinish(oldDatafiles, true, Long.MAX_VALUE);
				try {
				} catch (Exception e) {
					throw new IllegalStateException("Can not bring major compaction online, lock not held", e);
				}
			}
			for (FileRef oldDatafile : oldDatafiles) {
				if (!(datafileSizes.containsKey(oldDatafile))) {
					log.error(("file does not exist in set " + oldDatafile));
				}
				datafileSizes.remove(oldDatafile);
				majorCompactingFiles.remove(oldDatafile);
			}
			if (datafileSizes.containsKey(newDatafile)) {
				log.error(("Adding file that is already in set " + newDatafile));
			}
			if ((dfv.getNumEntries()) > 0) {
				datafileSizes.put(newDatafile, dfv);
			}
			majorCompactingFiles.add(newDatafile);
			lastLocation = tablet.resetLastLocation();
			tablet.setLastCompactionID(compactionId);
			t2 = System.currentTimeMillis();
		}
		if (!(extent.isRootTablet())) {
			Set<FileRef> filesInUseByScans = waitForScansToFinish(oldDatafiles, false, 10000);
			if ((filesInUseByScans.size()) > 0)
				log.debug(((("Adding scan refs to metadata " + extent) + " ") + filesInUseByScans));

			removeFilesAfterScan(filesInUseByScans);
		}
		log.debug(String.format("MajC finish lock %.2f secs", ((t2 - t1) / 1000.0)));
		log.log(TLevel.TABLET_HIST, ((((extent + " MajC ") + oldDatafiles) + " --> ") + newDatafile));
	}

	public SortedMap<FileRef, DataFileValue> getDatafileSizes() {
		synchronized(tablet) {
			TreeMap<FileRef, DataFileValue> copy = new TreeMap<>(datafileSizes);
			return Collections.unmodifiableSortedMap(copy);
		}
	}

	public Set<FileRef> getFiles() {
		synchronized(tablet) {
			HashSet<FileRef> files = new HashSet<>(datafileSizes.keySet());
			return Collections.unmodifiableSet(files);
		}
	}

	public int getNumFiles() {
		return datafileSizes.size();
	}
}

