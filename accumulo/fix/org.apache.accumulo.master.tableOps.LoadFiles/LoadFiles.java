

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.accumulo.core.client.impl.AcceptableThriftTableOperationException;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.thrift.ClientService;
import org.apache.accumulo.core.client.impl.thrift.TableOperation;
import org.apache.accumulo.core.client.impl.thrift.TableOperationExceptionType;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.master.thrift.BulkImportState;
import org.apache.accumulo.core.rpc.ThriftUtil;
import org.apache.accumulo.core.security.thrift.TCredentials;
import org.apache.accumulo.core.tabletserver.thrift.TabletClientService;
import org.apache.accumulo.core.trace.Tracer;
import org.apache.accumulo.core.trace.thrift.TInfo;
import org.apache.accumulo.core.util.HostAndPort;
import org.apache.accumulo.core.util.SimpleThreadPool;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.fate.util.UtilWaitThread;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.BulkImport;
import org.apache.accumulo.master.tableOps.MasterRepo;
import org.apache.accumulo.server.fs.VolumeManager;
import org.apache.accumulo.server.master.state.TServerInstance;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.htrace.wrappers.TraceExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class LoadFiles extends MasterRepo {
	private static final long serialVersionUID = 1L;

	private static ExecutorService threadPool = null;

	private static final Logger log = LoggerFactory.getLogger(LoadFiles.class);

	private String tableId;

	private String source;

	private String bulk;

	private String errorDir;

	private boolean setTime;

	public LoadFiles(String tableId, String source, String bulk, String errorDir, boolean setTime) {
		this.tableId = tableId;
		this.source = source;
		this.bulk = bulk;
		this.errorDir = errorDir;
		this.setTime = setTime;
	}

	@Override
	public long isReady(long tid, Master master) throws Exception {
		if ((master.onlineTabletServers().size()) == 0)
			return 500;

		return 0;
	}

	private static synchronized ExecutorService getThreadPool(Master master) {
		if ((LoadFiles.threadPool) == null) {
			int threadPoolSize = master.getConfiguration().getCount(Property.MASTER_BULK_THREADPOOL_SIZE);
			ThreadPoolExecutor pool = new SimpleThreadPool(threadPoolSize, "bulk import");
			pool.allowCoreThreadTimeOut(true);
			LoadFiles.threadPool = new TraceExecutorService(pool);
		}
		return LoadFiles.threadPool;
	}

	@Override
	public Repo<Master> call(final long tid, final Master master) throws Exception {
		master.updateBulkImportStatus(source, BulkImportState.LOADING);
		ExecutorService executor = LoadFiles.getThreadPool(master);
		final AccumuloConfiguration conf = master.getConfiguration();
		VolumeManager fs = master.getFileSystem();
		List<FileStatus> files = new ArrayList<>();
		for (FileStatus entry : fs.listStatus(new Path(bulk))) {
			files.add(entry);
		}
		LoadFiles.log.debug((((("tid " + tid) + " importing ") + (files.size())) + " files"));
		Path writable = new Path(this.errorDir, ".iswritable");
		if (!(fs.createNewFile(writable))) {
			fs.delete(writable);
			if (!(fs.createNewFile(writable)))
				throw new AcceptableThriftTableOperationException(tableId, null, TableOperation.BULK_IMPORT, TableOperationExceptionType.BULK_BAD_ERROR_DIRECTORY, ("Unable to write to " + (this.errorDir)));

		}
		fs.delete(writable);
		final Set<String> filesToLoad = Collections.synchronizedSet(new HashSet<String>());
		for (FileStatus f : files)
			filesToLoad.add(f.getPath().toString());

		final int RETRIES = Math.max(1, conf.getCount(Property.MASTER_BULK_RETRIES));
		for (int attempt = 0; (attempt < RETRIES) && ((filesToLoad.size()) > 0); attempt++) {
			List<Future<List<String>>> results = new ArrayList<>();
			if ((master.onlineTabletServers().size()) == 0)
				LoadFiles.log.warn((("There are no tablet server to process bulk import, waiting (tid = " + tid) + ")"));

			while ((master.onlineTabletServers().size()) == 0) {
				UtilWaitThread.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
			} 
			final List<String> loaded = Collections.synchronizedList(new ArrayList<String>());
			final Random random = new Random();
			final TServerInstance[] servers = master.onlineTabletServers().toArray(new TServerInstance[0]);
			for (final String file : filesToLoad) {
				results.add(executor.submit(new Callable<List<String>>() {
					@Override
					public List<String> call() {
						List<String> failures = new ArrayList<>();
						ClientService.Client client = null;
						HostAndPort server = null;
						try {
							long timeInMillis = master.getConfiguration().getTimeInMillis(Property.MASTER_BULK_TIMEOUT);
							server = servers[random.nextInt(servers.length)].getLocation();
							client = ThriftUtil.getTServerClient(server, master, timeInMillis);
							List<String> attempt = Collections.singletonList(file);
							LoadFiles.log.debug(((("Asking " + server) + " to bulk import ") + file));
							List<String> fail = client.bulkImportFiles(Tracer.traceInfo(), master.rpcCreds(), tid, tableId, attempt, errorDir, setTime);
							if (fail.isEmpty()) {
								loaded.add(file);
							}else {
								failures.addAll(fail);
							}
						} catch (Exception ex) {
							LoadFiles.log.error(((((("rpc failed server:" + server) + ", tid:") + tid) + " ") + ex));
						} finally {
							ThriftUtil.returnClient(client);
						}
						return failures;
					}
				}));
			}
			Set<String> failures = new HashSet<>();
			for (Future<List<String>> f : results)
				failures.addAll(f.get());

			filesToLoad.removeAll(loaded);
			if ((filesToLoad.size()) > 0) {
				LoadFiles.log.debug((((((("tid " + tid) + " attempt ") + (attempt + 1)) + " ") + (LoadFiles.sampleList(filesToLoad, 10))) + " failed"));
				UtilWaitThread.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
			}
		}
		FSDataOutputStream failFile = fs.create(new Path(errorDir, BulkImport.FAILURES_TXT), true);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(failFile, StandardCharsets.UTF_8));
		try {
			for (String f : filesToLoad) {
				out.write(f);
				out.write("\n");
			}
		} finally {
			out.close();
		}
		return null;
	}

	static String sampleList(Collection<?> potentiallyLongList, int max) {
		StringBuilder result = new StringBuilder();
		result.append("[");
		int i = 0;
		for (Object obj : potentiallyLongList) {
			result.append(obj);
			if (i >= max) {
				result.append("...");
				break;
			}else {
				result.append(", ");
			}
			i++;
		}
		if (i < max)
			result.delete(((result.length()) - 2), result.length());

		result.append("]");
		return result.toString();
	}
}

