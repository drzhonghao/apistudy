

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.BlacklistedDirectories;
import org.apache.cassandra.io.FSDiskFullWriteError;
import org.apache.cassandra.io.FSError;
import org.apache.cassandra.io.FSWriteError;
import org.apache.cassandra.io.sstable.Component;
import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.io.sstable.SSTable;
import org.apache.cassandra.io.sstable.SnapshotDeletingTask;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.DirectorySizeCalculator;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.Pair;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.io.sstable.Component.Type.DIGEST;


public class Directories {
	private static final Logger logger = LoggerFactory.getLogger(Directories.class);

	public static final String BACKUPS_SUBDIR = "backups";

	public static final String SNAPSHOT_SUBDIR = "snapshots";

	public static final String TMP_SUBDIR = "tmp";

	public static final String SECONDARY_INDEX_NAME_SEPARATOR = ".";

	public static final Directories.DataDirectory[] dataDirectories;

	static {
		String[] locations = DatabaseDescriptor.getAllDataFileLocations();
		dataDirectories = new Directories.DataDirectory[locations.length];
		for (int i = 0; i < (locations.length); ++i)
			Directories.dataDirectories[i] = new Directories.DataDirectory(new File(locations[i]));

	}

	public static boolean verifyFullPermissions(File dir, String dataDir) {
		if (!(dir.isDirectory())) {
			Directories.logger.error("Not a directory {}", dataDir);
			return false;
		}else
			if (!(Directories.FileAction.hasPrivilege(dir, Directories.FileAction.X))) {
				Directories.logger.error("Doesn't have execute permissions for {} directory", dataDir);
				return false;
			}else
				if (!(Directories.FileAction.hasPrivilege(dir, Directories.FileAction.R))) {
					Directories.logger.error("Doesn't have read permissions for {} directory", dataDir);
					return false;
				}else
					if ((dir.exists()) && (!(Directories.FileAction.hasPrivilege(dir, Directories.FileAction.W)))) {
						Directories.logger.error("Doesn't have write permissions for {} directory", dataDir);
						return false;
					}



		return true;
	}

	public enum FileAction {

		X,
		W,
		XW,
		R,
		XR,
		RW,
		XRW;
		FileAction() {
		}

		public static boolean hasPrivilege(File file, Directories.FileAction action) {
			boolean privilege = false;
			switch (action) {
				case X :
					privilege = file.canExecute();
					break;
				case W :
					privilege = file.canWrite();
					break;
				case XW :
					privilege = (file.canExecute()) && (file.canWrite());
					break;
				case R :
					privilege = file.canRead();
					break;
				case XR :
					privilege = (file.canExecute()) && (file.canRead());
					break;
				case RW :
					privilege = (file.canRead()) && (file.canWrite());
					break;
				case XRW :
					privilege = ((file.canExecute()) && (file.canRead())) && (file.canWrite());
					break;
			}
			return privilege;
		}
	}

	private final CFMetaData metadata;

	private final Directories.DataDirectory[] paths;

	private final File[] dataPaths;

	public Directories(final CFMetaData metadata) {
		this(metadata, Directories.dataDirectories);
	}

	public Directories(final CFMetaData metadata, Collection<Directories.DataDirectory> paths) {
		this(metadata, paths.toArray(new Directories.DataDirectory[paths.size()]));
	}

	public Directories(final CFMetaData metadata, Directories.DataDirectory[] paths) {
		this.metadata = metadata;
		this.paths = paths;
		String cfId = ByteBufferUtil.bytesToHex(ByteBufferUtil.bytes(metadata.cfId));
		int idx = metadata.cfName.indexOf(Directories.SECONDARY_INDEX_NAME_SEPARATOR);
		String cfName = (idx >= 0) ? metadata.cfName.substring(0, idx) : metadata.cfName;
		String indexNameWithDot = (idx >= 0) ? metadata.cfName.substring(idx) : null;
		this.dataPaths = new File[paths.length];
		String oldSSTableRelativePath = Directories.join(metadata.ksName, cfName);
		for (int i = 0; i < (paths.length); ++i) {
			dataPaths[i] = new File(paths[i].location, oldSSTableRelativePath);
		}
		boolean olderDirectoryExists = Iterables.any(Arrays.asList(dataPaths), new Predicate<File>() {
			public boolean apply(File file) {
				return file.exists();
			}
		});
		if (!olderDirectoryExists) {
			String newSSTableRelativePath = Directories.join(metadata.ksName, ((cfName + '-') + cfId));
			for (int i = 0; i < (paths.length); ++i)
				dataPaths[i] = new File(paths[i].location, newSSTableRelativePath);

		}
		if (indexNameWithDot != null) {
			for (int i = 0; i < (paths.length); ++i)
				dataPaths[i] = new File(dataPaths[i], indexNameWithDot);

		}
		for (File dir : dataPaths) {
			try {
				FileUtils.createDirectory(dir);
			} catch (FSError e) {
				Directories.logger.error("Failed to create {} directory", dir);
				FileUtils.handleFSError(e);
			}
		}
		if (indexNameWithDot != null) {
			for (File dataPath : dataPaths) {
				File[] indexFiles = dataPath.getParentFile().listFiles(new FileFilter() {
					@Override
					public boolean accept(File file) {
						if (file.isDirectory())
							return false;

						Pair<Descriptor, Component> pair = SSTable.tryComponentFromFilename(file.getParentFile(), file.getName());
						return ((pair != null) && (pair.left.ksname.equals(metadata.ksName))) && (pair.left.cfname.equals(metadata.cfName));
					}
				});
				for (File indexFile : indexFiles) {
					File destFile = new File(dataPath, indexFile.getName());
					Directories.logger.trace("Moving index file {} to {}", indexFile, destFile);
					FileUtils.renameWithConfirm(indexFile, destFile);
				}
			}
		}
	}

	public File getLocationForDisk(Directories.DataDirectory dataDirectory) {
		if (dataDirectory != null)
			for (File dir : dataPaths)
				if (dir.getAbsolutePath().startsWith(dataDirectory.location.getAbsolutePath()))
					return dir;



		return null;
	}

	public Directories.DataDirectory getDataDirectoryForFile(File directory) {
		if (directory != null) {
			for (Directories.DataDirectory dataDirectory : paths) {
				if (directory.getAbsolutePath().startsWith(dataDirectory.location.getAbsolutePath()))
					return dataDirectory;

			}
		}
		return null;
	}

	public Descriptor find(String filename) {
		for (File dir : dataPaths) {
			if (new File(dir, filename).exists())
				return Descriptor.fromFilename(dir, filename).left;

		}
		return null;
	}

	public File getDirectoryForNewSSTables() {
		return getWriteableLocationAsFile((-1L));
	}

	public File getWriteableLocationAsFile(long writeSize) {
		File location = getLocationForDisk(getWriteableLocation(writeSize));
		if (location == null)
			throw new FSWriteError(new IOException((("No configured data directory contains enough space to write " + writeSize) + " bytes")), "");

		return location;
	}

	public File getTemporaryWriteableDirectoryAsFile(long writeSize) {
		File location = getLocationForDisk(getWriteableLocation(writeSize));
		if (location == null)
			return null;

		return new File(location, Directories.TMP_SUBDIR);
	}

	public void removeTemporaryDirectories() {
		for (File dataDir : dataPaths) {
			File tmpDir = new File(dataDir, Directories.TMP_SUBDIR);
			if (tmpDir.exists()) {
				Directories.logger.debug("Removing temporary directory {}", tmpDir);
				FileUtils.deleteRecursive(tmpDir);
			}
		}
	}

	public Directories.DataDirectory getWriteableLocation(long writeSize) {
		List<Directories.DataDirectoryCandidate> candidates = new ArrayList<>();
		long totalAvailable = 0L;
		boolean tooBig = false;
		for (Directories.DataDirectory dataDir : paths) {
			if (BlacklistedDirectories.isUnwritable(getLocationForDisk(dataDir))) {
				Directories.logger.trace("removing blacklisted candidate {}", dataDir.location);
				continue;
			}
			Directories.DataDirectoryCandidate candidate = new Directories.DataDirectoryCandidate(dataDir);
			if ((candidate.availableSpace) < writeSize) {
				Directories.logger.trace("removing candidate {}, usable={}, requested={}", candidate.dataDirectory.location, candidate.availableSpace, writeSize);
				tooBig = true;
				continue;
			}
			candidates.add(candidate);
			totalAvailable += candidate.availableSpace;
		}
		if (candidates.isEmpty())
			if (tooBig)
				throw new FSDiskFullWriteError(new IOException((("Insufficient disk space to write " + writeSize) + " bytes")), "");
			else
				throw new FSWriteError(new IOException("All configured data directories have been blacklisted as unwritable for erroring out"), "");


		if ((candidates.size()) == 1)
			return candidates.get(0).dataDirectory;

		Directories.sortWriteableCandidates(candidates, totalAvailable);
		return Directories.pickWriteableDirectory(candidates);
	}

	static Directories.DataDirectory pickWriteableDirectory(List<Directories.DataDirectoryCandidate> candidates) {
		double rnd = ThreadLocalRandom.current().nextDouble();
		for (Directories.DataDirectoryCandidate candidate : candidates) {
			rnd -= candidate.perc;
			if (rnd <= 0)
				return candidate.dataDirectory;

		}
		return candidates.get(0).dataDirectory;
	}

	static void sortWriteableCandidates(List<Directories.DataDirectoryCandidate> candidates, long totalAvailable) {
		for (Directories.DataDirectoryCandidate candidate : candidates)
			candidate.calcFreePerc(totalAvailable);

		Collections.sort(candidates);
	}

	public boolean hasAvailableDiskSpace(long estimatedSSTables, long expectedTotalWriteSize) {
		long writeSize = expectedTotalWriteSize / estimatedSSTables;
		long totalAvailable = 0L;
		for (Directories.DataDirectory dataDir : paths) {
			if (BlacklistedDirectories.isUnwritable(getLocationForDisk(dataDir)))
				continue;

			Directories.DataDirectoryCandidate candidate = new Directories.DataDirectoryCandidate(dataDir);
			if ((candidate.availableSpace) < writeSize)
				continue;

			totalAvailable += candidate.availableSpace;
		}
		return totalAvailable > expectedTotalWriteSize;
	}

	public Directories.DataDirectory[] getWriteableLocations() {
		List<Directories.DataDirectory> nonBlacklistedDirs = new ArrayList<>();
		for (Directories.DataDirectory dir : paths) {
			if (!(BlacklistedDirectories.isUnwritable(dir.location)))
				nonBlacklistedDirs.add(dir);

		}
		Collections.sort(nonBlacklistedDirs, new Comparator<Directories.DataDirectory>() {
			@Override
			public int compare(Directories.DataDirectory o1, Directories.DataDirectory o2) {
				return o1.location.compareTo(o2.location);
			}
		});
		return nonBlacklistedDirs.toArray(new Directories.DataDirectory[nonBlacklistedDirs.size()]);
	}

	public static File getSnapshotDirectory(Descriptor desc, String snapshotName) {
		return Directories.getSnapshotDirectory(desc.directory, snapshotName);
	}

	public static File getSnapshotDirectory(File location, String snapshotName) {
		if (location.getName().startsWith(Directories.SECONDARY_INDEX_NAME_SEPARATOR)) {
			return Directories.getOrCreate(location.getParentFile(), Directories.SNAPSHOT_SUBDIR, snapshotName, location.getName());
		}else {
			return Directories.getOrCreate(location, Directories.SNAPSHOT_SUBDIR, snapshotName);
		}
	}

	public File getSnapshotManifestFile(String snapshotName) {
		File snapshotDir = Directories.getSnapshotDirectory(getDirectoryForNewSSTables(), snapshotName);
		return new File(snapshotDir, "manifest.json");
	}

	public File getSnapshotSchemaFile(String snapshotName) {
		File snapshotDir = Directories.getSnapshotDirectory(getDirectoryForNewSSTables(), snapshotName);
		return new File(snapshotDir, "schema.cql");
	}

	public File getNewEphemeralSnapshotMarkerFile(String snapshotName) {
		File snapshotDir = new File(getWriteableLocationAsFile(1L), Directories.join(Directories.SNAPSHOT_SUBDIR, snapshotName));
		return Directories.getEphemeralSnapshotMarkerFile(snapshotDir);
	}

	private static File getEphemeralSnapshotMarkerFile(File snapshotDirectory) {
		return new File(snapshotDirectory, "ephemeral.snapshot");
	}

	public static File getBackupsDirectory(Descriptor desc) {
		return Directories.getBackupsDirectory(desc.directory);
	}

	public static File getBackupsDirectory(File location) {
		if (location.getName().startsWith(Directories.SECONDARY_INDEX_NAME_SEPARATOR)) {
			return Directories.getOrCreate(location.getParentFile(), Directories.BACKUPS_SUBDIR, location.getName());
		}else {
			return Directories.getOrCreate(location, Directories.BACKUPS_SUBDIR);
		}
	}

	public static class DataDirectory {
		public final File location;

		public DataDirectory(File location) {
			this.location = location;
		}

		public long getAvailableSpace() {
			long availableSpace = (FileUtils.getUsableSpace(location)) - (DatabaseDescriptor.getMinFreeSpacePerDriveInBytes());
			return availableSpace > 0 ? availableSpace : 0;
		}

		@Override
		public boolean equals(Object o) {
			if ((this) == o)
				return true;

			if ((o == null) || ((getClass()) != (o.getClass())))
				return false;

			Directories.DataDirectory that = ((Directories.DataDirectory) (o));
			return location.equals(that.location);
		}

		@Override
		public int hashCode() {
			return location.hashCode();
		}

		public String toString() {
			return (("DataDirectory{" + "location=") + (location)) + '}';
		}
	}

	static final class DataDirectoryCandidate implements Comparable<Directories.DataDirectoryCandidate> {
		final Directories.DataDirectory dataDirectory;

		final long availableSpace;

		double perc;

		public DataDirectoryCandidate(Directories.DataDirectory dataDirectory) {
			this.dataDirectory = dataDirectory;
			this.availableSpace = dataDirectory.getAvailableSpace();
		}

		void calcFreePerc(long totalAvailableSpace) {
			double w = availableSpace;
			w /= totalAvailableSpace;
			perc = w;
		}

		public int compareTo(Directories.DataDirectoryCandidate o) {
			if ((this) == o)
				return 0;

			int r = Double.compare(perc, o.perc);
			if (r != 0)
				return -r;

			return (System.identityHashCode(this)) - (System.identityHashCode(o));
		}
	}

	public enum FileType {

		FINAL,
		TEMPORARY,
		TXN_LOG;}

	public enum OnTxnErr {

		THROW,
		IGNORE;}

	public Directories.SSTableLister sstableLister(Directories.OnTxnErr onTxnErr) {
		return new Directories.SSTableLister(onTxnErr);
	}

	public class SSTableLister {
		private final Directories.OnTxnErr onTxnErr;

		private boolean skipTemporary;

		private boolean includeBackups;

		private boolean onlyBackups;

		private int nbFiles;

		private final Map<Descriptor, Set<Component>> components = new HashMap<>();

		private boolean filtered;

		private String snapshotName;

		private SSTableLister(Directories.OnTxnErr onTxnErr) {
			this.onTxnErr = onTxnErr;
		}

		public Directories.SSTableLister skipTemporary(boolean b) {
			if (filtered)
				throw new IllegalStateException("list() has already been called");

			skipTemporary = b;
			return this;
		}

		public Directories.SSTableLister includeBackups(boolean b) {
			if (filtered)
				throw new IllegalStateException("list() has already been called");

			includeBackups = b;
			return this;
		}

		public Directories.SSTableLister onlyBackups(boolean b) {
			if (filtered)
				throw new IllegalStateException("list() has already been called");

			onlyBackups = b;
			includeBackups = b;
			return this;
		}

		public Directories.SSTableLister snapshots(String sn) {
			if (filtered)
				throw new IllegalStateException("list() has already been called");

			snapshotName = sn;
			return this;
		}

		public Map<Descriptor, Set<Component>> list() {
			filter();
			return ImmutableMap.copyOf(components);
		}

		public List<File> listFiles() {
			filter();
			List<File> l = new ArrayList<>(nbFiles);
			for (Map.Entry<Descriptor, Set<Component>> entry : components.entrySet()) {
				for (Component c : entry.getValue()) {
					l.add(new File(entry.getKey().filenameFor(c)));
				}
			}
			return l;
		}

		private void filter() {
			if (filtered)
				return;

			for (File location : dataPaths) {
				if (BlacklistedDirectories.isUnreadable(location))
					continue;

				if ((snapshotName) != null) {
					continue;
				}
			}
			filtered = true;
		}

		private BiFunction<File, Directories.FileType, Boolean> getFilter() {
			return ( file, type) -> {
				switch (type) {
					case TXN_LOG :
						return false;
					case TEMPORARY :
						if (skipTemporary)
							return false;

					case FINAL :
						Pair<Descriptor, Component> pair = SSTable.tryComponentFromFilename(file.getParentFile(), file.getName());
						if (pair == null)
							return false;

						if ((!(pair.left.ksname.equals(metadata.ksName))) || (!(pair.left.cfname.equals(metadata.cfName))))
							return false;

						Set<Component> previous = components.get(pair.left);
						if (previous == null) {
							previous = new HashSet<>();
							components.put(pair.left, previous);
						}else
							if ((pair.right.type) == (DIGEST)) {
								if ((pair.right) != (pair.left.digestComponent)) {
									components.remove(pair.left);
									Descriptor updated = pair.left.withDigestComponent(pair.right);
									components.put(updated, previous);
								}
							}

						previous.add(pair.right);
						(nbFiles)++;
						return false;
					default :
						throw new AssertionError();
				}
			};
		}
	}

	public Map<String, Pair<Long, Long>> getSnapshotDetails() {
		final Map<String, Pair<Long, Long>> snapshotSpaceMap = new HashMap<>();
		for (File snapshot : listSnapshots()) {
			final long sizeOnDisk = FileUtils.folderSize(snapshot);
			final long trueSize = getTrueAllocatedSizeIn(snapshot);
			Pair<Long, Long> spaceUsed = snapshotSpaceMap.get(snapshot.getName());
			if (spaceUsed == null)
				spaceUsed = Pair.create(sizeOnDisk, trueSize);
			else
				spaceUsed = Pair.create(((spaceUsed.left) + sizeOnDisk), ((spaceUsed.right) + trueSize));

			snapshotSpaceMap.put(snapshot.getName(), spaceUsed);
		}
		return snapshotSpaceMap;
	}

	public List<String> listEphemeralSnapshots() {
		final List<String> ephemeralSnapshots = new LinkedList<>();
		for (File snapshot : listSnapshots()) {
			if (Directories.getEphemeralSnapshotMarkerFile(snapshot).exists())
				ephemeralSnapshots.add(snapshot.getName());

		}
		return ephemeralSnapshots;
	}

	private List<File> listSnapshots() {
		final List<File> snapshots = new LinkedList<>();
		for (final File dir : dataPaths) {
			File snapshotDir = (dir.getName().startsWith(Directories.SECONDARY_INDEX_NAME_SEPARATOR)) ? new File(dir.getParent(), Directories.SNAPSHOT_SUBDIR) : new File(dir, Directories.SNAPSHOT_SUBDIR);
			if ((snapshotDir.exists()) && (snapshotDir.isDirectory())) {
				final File[] snapshotDirs = snapshotDir.listFiles();
				if (snapshotDirs != null) {
					for (final File snapshot : snapshotDirs) {
						if (snapshot.isDirectory())
							snapshots.add(snapshot);

					}
				}
			}
		}
		return snapshots;
	}

	public boolean snapshotExists(String snapshotName) {
		for (File dir : dataPaths) {
			File snapshotDir;
			if (dir.getName().startsWith(Directories.SECONDARY_INDEX_NAME_SEPARATOR)) {
				snapshotDir = new File(dir.getParentFile(), Directories.join(Directories.SNAPSHOT_SUBDIR, snapshotName, dir.getName()));
			}else {
				snapshotDir = new File(dir, Directories.join(Directories.SNAPSHOT_SUBDIR, snapshotName));
			}
			if (snapshotDir.exists())
				return true;

		}
		return false;
	}

	public static void clearSnapshot(String snapshotName, List<File> snapshotDirectories) {
		String tag = (snapshotName == null) ? "" : snapshotName;
		for (File dir : snapshotDirectories) {
			File snapshotDir = new File(dir, Directories.join(Directories.SNAPSHOT_SUBDIR, tag));
			if (snapshotDir.exists()) {
				Directories.logger.trace("Removing snapshot directory {}", snapshotDir);
				try {
					FileUtils.deleteRecursive(snapshotDir);
				} catch (FSWriteError e) {
					if (FBUtilities.isWindows)
						SnapshotDeletingTask.addFailedSnapshot(snapshotDir);
					else
						throw e;

				}
			}
		}
	}

	public long snapshotCreationTime(String snapshotName) {
		for (File dir : dataPaths) {
			File snapshotDir = Directories.getSnapshotDirectory(dir, snapshotName);
			if (snapshotDir.exists())
				return snapshotDir.lastModified();

		}
		throw new RuntimeException((("Snapshot " + snapshotName) + " doesn't exist"));
	}

	public long trueSnapshotsSize() {
		long result = 0L;
		for (File dir : dataPaths) {
			File snapshotDir = (dir.getName().startsWith(Directories.SECONDARY_INDEX_NAME_SEPARATOR)) ? new File(dir.getParent(), Directories.SNAPSHOT_SUBDIR) : new File(dir, Directories.SNAPSHOT_SUBDIR);
			result += getTrueAllocatedSizeIn(snapshotDir);
		}
		return result;
	}

	public long getRawDiretoriesSize() {
		long totalAllocatedSize = 0L;
		for (File path : dataPaths)
			totalAllocatedSize += FileUtils.folderSize(path);

		return totalAllocatedSize;
	}

	public long getTrueAllocatedSizeIn(File input) {
		if (!(input.isDirectory()))
			return 0;

		Directories.SSTableSizeSummer visitor = new Directories.SSTableSizeSummer(input, sstableLister(Directories.OnTxnErr.THROW).listFiles());
		try {
			Files.walkFileTree(input.toPath(), visitor);
		} catch (IOException e) {
			Directories.logger.error("Could not calculate the size of {}. {}", input, e);
		}
		return visitor.getAllocatedSize();
	}

	public static List<File> getKSChildDirectories(String ksName) {
		return Directories.getKSChildDirectories(ksName, Directories.dataDirectories);
	}

	public static List<File> getKSChildDirectories(String ksName, Directories.DataDirectory[] directories) {
		List<File> result = new ArrayList<>();
		for (Directories.DataDirectory dataDirectory : directories) {
			File ksDir = new File(dataDirectory.location, ksName);
			File[] cfDirs = ksDir.listFiles();
			if (cfDirs == null)
				continue;

			for (File cfDir : cfDirs) {
				if (cfDir.isDirectory())
					result.add(cfDir);

			}
		}
		return result;
	}

	public List<File> getCFDirectories() {
		List<File> result = new ArrayList<>();
		for (File dataDirectory : dataPaths) {
			if (dataDirectory.isDirectory())
				result.add(dataDirectory);

		}
		return result;
	}

	private static File getOrCreate(File base, String... subdirs) {
		File dir = ((subdirs == null) || ((subdirs.length) == 0)) ? base : new File(base, Directories.join(subdirs));
		if (dir.exists()) {
			if (!(dir.isDirectory()))
				throw new AssertionError(String.format("Invalid directory path %s: path exists but is not a directory", dir));

		}else
			if ((!(dir.mkdirs())) && (!((dir.exists()) && (dir.isDirectory())))) {
				throw new FSWriteError(new IOException(("Unable to create directory " + dir)), dir);
			}

		return dir;
	}

	private static String join(String... s) {
		return StringUtils.join(s, File.separator);
	}

	@com.google.common.annotations.VisibleForTesting
	static void overrideDataDirectoriesForTest(String loc) {
		for (int i = 0; i < (Directories.dataDirectories.length); ++i)
			Directories.dataDirectories[i] = new Directories.DataDirectory(new File(loc));

	}

	@com.google.common.annotations.VisibleForTesting
	static void resetDataDirectoriesAfterTest() {
		String[] locations = DatabaseDescriptor.getAllDataFileLocations();
		for (int i = 0; i < (locations.length); ++i)
			Directories.dataDirectories[i] = new Directories.DataDirectory(new File(locations[i]));

	}

	private class SSTableSizeSummer extends DirectorySizeCalculator {
		private final HashSet<File> toSkip;

		SSTableSizeSummer(File path, List<File> files) {
			super(path);
			toSkip = new HashSet<>(files);
		}

		@Override
		public boolean isAcceptable(Path path) {
			File file = path.toFile();
			Pair<Descriptor, Component> pair = SSTable.tryComponentFromFilename(path.getParent().toFile(), file.getName());
			return (((pair != null) && (pair.left.ksname.equals(metadata.ksName))) && (pair.left.cfname.equals(metadata.cfName))) && (!(toSkip.contains(file)));
		}
	}
}

