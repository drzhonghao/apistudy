

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.impl.AcceptableThriftTableOperationException;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.client.impl.thrift.TableOperation;
import org.apache.accumulo.core.client.impl.thrift.TableOperationExceptionType;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.file.FileOperations;
import org.apache.accumulo.core.master.state.tables.TableState;
import org.apache.accumulo.core.master.thrift.BulkImportState;
import org.apache.accumulo.core.util.SimpleThreadPool;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.fate.util.UtilWaitThread;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.MasterRepo;
import org.apache.accumulo.master.tableOps.Utils;
import org.apache.accumulo.server.ServerConstants;
import org.apache.accumulo.server.fs.VolumeManager;
import org.apache.accumulo.server.tablets.UniqueNameAllocator;
import org.apache.accumulo.server.util.MetadataTableUtil;
import org.apache.accumulo.server.zookeeper.TransactionWatcher;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.MapFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.accumulo.server.zookeeper.TransactionWatcher.ZooArbitrator.cleanup;
import static org.apache.accumulo.server.zookeeper.TransactionWatcher.ZooArbitrator.start;


public class BulkImport extends MasterRepo {
	public static final String FAILURES_TXT = "failures.txt";

	private static final long serialVersionUID = 1L;

	private static final Logger log = LoggerFactory.getLogger(BulkImport.class);

	private String tableId;

	private String sourceDir;

	private String errorDir;

	private boolean setTime;

	public BulkImport(String tableId, String sourceDir, String errorDir, boolean setTime) {
		this.tableId = tableId;
		this.sourceDir = sourceDir;
		this.errorDir = errorDir;
		this.setTime = setTime;
	}

	@Override
	public long isReady(long tid, Master master) throws Exception {
		if (!(Utils.getReadLock(tableId, tid).tryLock()))
			return 100;

		Tables.clearCache(master.getInstance());
		if ((Tables.getTableState(master.getInstance(), tableId)) == (TableState.ONLINE)) {
			long reserve1;
			long reserve2;
			reserve1 = reserve2 = Utils.reserveHdfsDirectory(sourceDir, tid);
			if (reserve1 == 0)
				reserve2 = Utils.reserveHdfsDirectory(errorDir, tid);

			return reserve2;
		}else {
			throw new AcceptableThriftTableOperationException(tableId, null, TableOperation.BULK_IMPORT, TableOperationExceptionType.OFFLINE, null);
		}
	}

	@Override
	public Repo<Master> call(long tid, Master master) throws Exception {
		BulkImport.log.debug((((" tid " + tid) + " sourceDir ") + (sourceDir)));
		Utils.getReadLock(tableId, tid).lock();
		VolumeManager fs = master.getFileSystem();
		Path errorPath = new Path(errorDir);
		FileStatus errorStatus = null;
		try {
			errorStatus = fs.getFileStatus(errorPath);
		} catch (FileNotFoundException ex) {
		}
		if (errorStatus == null)
			throw new AcceptableThriftTableOperationException(tableId, null, TableOperation.BULK_IMPORT, TableOperationExceptionType.BULK_BAD_ERROR_DIRECTORY, ((errorDir) + " does not exist"));

		if (!(errorStatus.isDirectory()))
			throw new AcceptableThriftTableOperationException(tableId, null, TableOperation.BULK_IMPORT, TableOperationExceptionType.BULK_BAD_ERROR_DIRECTORY, ((errorDir) + " is not a directory"));

		if ((fs.listStatus(errorPath).length) != 0)
			throw new AcceptableThriftTableOperationException(tableId, null, TableOperation.BULK_IMPORT, TableOperationExceptionType.BULK_BAD_ERROR_DIRECTORY, ((errorDir) + " is not empty"));

		start(Constants.BULK_ARBITRATOR_TYPE, tid);
		master.updateBulkImportStatus(sourceDir, BulkImportState.MOVING);
		try {
			String bulkDir = prepareBulkImport(master, fs, sourceDir, tableId);
			BulkImport.log.debug((((" tid " + tid) + " bulkDir ") + bulkDir));
		} catch (IOException ex) {
			BulkImport.log.error("error preparing the bulk import directory", ex);
			throw new AcceptableThriftTableOperationException(tableId, null, TableOperation.BULK_IMPORT, TableOperationExceptionType.BULK_BAD_INPUT_DIRECTORY, (((sourceDir) + ": ") + ex));
		}
		return null;
	}

	private Path createNewBulkDir(VolumeManager fs, String tableId) throws IOException {
		Path tempPath = fs.matchingFileSystem(new Path(sourceDir), ServerConstants.getTablesDirs());
		if (tempPath == null)
			throw new IOException(((sourceDir) + " is not in a volume configured for Accumulo"));

		String tableDir = tempPath.toString();
		if (tableDir == null)
			throw new IOException(((sourceDir) + " is not in a volume configured for Accumulo"));

		Path directory = new Path(((tableDir + "/") + tableId));
		fs.mkdirs(directory);
		UniqueNameAllocator namer = UniqueNameAllocator.getInstance();
		while (true) {
			Path newBulkDir = new Path(directory, ((Constants.BULK_PREFIX) + (namer.getNextName())));
			if (fs.exists(newBulkDir))
				throw new IOException(("Dir exist when it should not " + newBulkDir));

			if (fs.mkdirs(newBulkDir))
				return newBulkDir;

			BulkImport.log.warn((("Failed to create " + newBulkDir) + " for unknown reason"));
			UtilWaitThread.sleepUninterruptibly(3, TimeUnit.SECONDS);
		} 
	}

	private String prepareBulkImport(Master master, final VolumeManager fs, String dir, String tableId) throws Exception {
		final Path bulkDir = createNewBulkDir(fs, tableId);
		MetadataTableUtil.addBulkLoadInProgressFlag(master, ((("/" + (bulkDir.getParent().getName())) + "/") + (bulkDir.getName())));
		Path dirPath = new Path(dir);
		FileStatus[] mapFiles = fs.listStatus(dirPath);
		final UniqueNameAllocator namer = UniqueNameAllocator.getInstance();
		int workerCount = master.getConfiguration().getCount(Property.MASTER_BULK_RENAME_THREADS);
		SimpleThreadPool workers = new SimpleThreadPool(workerCount, "bulk move");
		List<Future<Exception>> results = new ArrayList<>();
		for (FileStatus file : mapFiles) {
			final FileStatus fileStatus = file;
			results.add(workers.submit(new Callable<Exception>() {
				@Override
				public Exception call() throws Exception {
					try {
						String[] sa = fileStatus.getPath().getName().split("\\.");
						String extension = "";
						if ((sa.length) > 1) {
							extension = sa[((sa.length) - 1)];
							if (!(FileOperations.getValidExtensions().contains(extension))) {
								BulkImport.log.warn(((fileStatus.getPath()) + " does not have a valid extension, ignoring"));
								return null;
							}
						}else {
							extension = Constants.MAPFILE_EXTENSION;
						}
						if (extension.equals(Constants.MAPFILE_EXTENSION)) {
							if (!(fileStatus.isDirectory())) {
								BulkImport.log.warn(((fileStatus.getPath()) + " is not a map file, ignoring"));
								return null;
							}
							if (fileStatus.getPath().getName().equals("_logs")) {
								BulkImport.log.info(((fileStatus.getPath()) + " is probably a log directory from a map/reduce task, skipping"));
								return null;
							}
							try {
								FileStatus dataStatus = fs.getFileStatus(new Path(fileStatus.getPath(), MapFile.DATA_FILE_NAME));
								if (dataStatus.isDirectory()) {
									BulkImport.log.warn(((fileStatus.getPath()) + " is not a map file, ignoring"));
									return null;
								}
							} catch (FileNotFoundException fnfe) {
								BulkImport.log.warn(((fileStatus.getPath()) + " is not a map file, ignoring"));
								return null;
							}
						}
						String newName = (("I" + (namer.getNextName())) + ".") + extension;
						Path newPath = new Path(bulkDir, newName);
						try {
							fs.rename(fileStatus.getPath(), newPath);
							BulkImport.log.debug(((("Moved " + (fileStatus.getPath())) + " to ") + newPath));
						} catch (IOException E1) {
							BulkImport.log.error("Could not move: {} {}", fileStatus.getPath().toString(), E1.getMessage());
						}
					} catch (Exception ex) {
						return ex;
					}
					return null;
				}
			}));
		}
		workers.shutdown();
		while (!(workers.awaitTermination(1000L, TimeUnit.MILLISECONDS))) {
		} 
		for (Future<Exception> ex : results) {
			if ((ex.get()) != null) {
				throw ex.get();
			}
		}
		return bulkDir.toString();
	}

	@Override
	public void undo(long tid, Master environment) throws Exception {
		Utils.unreserveHdfsDirectory(sourceDir, tid);
		Utils.unreserveHdfsDirectory(errorDir, tid);
		Utils.getReadLock(tableId, tid).unlock();
		cleanup(Constants.BULK_ARBITRATOR_TYPE, tid);
	}
}

