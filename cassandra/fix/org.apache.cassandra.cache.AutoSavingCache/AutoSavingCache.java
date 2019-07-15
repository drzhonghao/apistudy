

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.cassandra.cache.CacheKey;
import org.apache.cassandra.cache.ICache;
import org.apache.cassandra.cache.InstrumentingCache;
import org.apache.cassandra.cache.KeyCacheKey;
import org.apache.cassandra.concurrent.DebuggableScheduledThreadPoolExecutor;
import org.apache.cassandra.concurrent.ScheduledExecutors;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.config.SchemaConstants;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.RowIndexEntry;
import org.apache.cassandra.db.compaction.CompactionInfo;
import org.apache.cassandra.db.compaction.OperationType;
import org.apache.cassandra.io.FSWriteError;
import org.apache.cassandra.io.util.ChecksummedRandomAccessReader;
import org.apache.cassandra.io.util.ChecksummedSequentialWriter;
import org.apache.cassandra.io.util.CorruptFileException;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.io.util.LengthAvailableInputStream;
import org.apache.cassandra.io.util.RandomAccessReader;
import org.apache.cassandra.io.util.SequentialWriterOption;
import org.apache.cassandra.io.util.UnbufferedDataOutputStreamPlus;
import org.apache.cassandra.io.util.WrappedDataOutputStreamPlus;
import org.apache.cassandra.service.CacheService;
import org.apache.cassandra.service.CacheService.CacheType;
import org.apache.cassandra.utils.JVMStabilityInspector;
import org.apache.cassandra.utils.Pair;
import org.apache.cassandra.utils.UUIDGen;
import org.cliffc.high_scale_lib.NonBlockingHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.service.CacheService.CacheType.COUNTER_CACHE;
import static org.apache.cassandra.service.CacheService.CacheType.KEY_CACHE;
import static org.apache.cassandra.service.CacheService.CacheType.ROW_CACHE;


public class AutoSavingCache<K extends CacheKey, V> extends InstrumentingCache<K, V> {
	public interface IStreamFactory {
		InputStream getInputStream(File dataPath, File crcPath) throws IOException;

		OutputStream getOutputStream(File dataPath, File crcPath) throws FileNotFoundException;
	}

	private static final Logger logger = LoggerFactory.getLogger(AutoSavingCache.class);

	public static final Set<CacheService.CacheType> flushInProgress = new NonBlockingHashSet<CacheService.CacheType>();

	protected volatile ScheduledFuture<?> saveTask;

	protected final CacheService.CacheType cacheType;

	private final AutoSavingCache.CacheSerializer<K, V> cacheLoader;

	private static final String CURRENT_VERSION = "e";

	private static volatile AutoSavingCache.IStreamFactory streamFactory = new AutoSavingCache.IStreamFactory() {
		private final SequentialWriterOption writerOption = SequentialWriterOption.newBuilder().trickleFsync(DatabaseDescriptor.getTrickleFsync()).trickleFsyncByteInterval(((DatabaseDescriptor.getTrickleFsyncIntervalInKb()) * 1024)).finishOnClose(true).build();

		public InputStream getInputStream(File dataPath, File crcPath) throws IOException {
			return ChecksummedRandomAccessReader.open(dataPath, crcPath);
		}

		public OutputStream getOutputStream(File dataPath, File crcPath) {
			return new ChecksummedSequentialWriter(dataPath, crcPath, null, writerOption);
		}
	};

	public static void setStreamFactory(AutoSavingCache.IStreamFactory streamFactory) {
		AutoSavingCache.streamFactory = streamFactory;
	}

	public AutoSavingCache(ICache<K, V> cache, CacheService.CacheType cacheType, AutoSavingCache.CacheSerializer<K, V> cacheloader) {
		super(cacheType.toString(), cache);
		this.cacheType = cacheType;
		this.cacheLoader = cacheloader;
	}

	public File getCacheDataPath(String version) {
		return DatabaseDescriptor.getSerializedCachePath(cacheType, version, "db");
	}

	public File getCacheCrcPath(String version) {
		return DatabaseDescriptor.getSerializedCachePath(cacheType, version, "crc");
	}

	public AutoSavingCache<K, V>.Writer getWriter(int keysToSave) {
		return new Writer(keysToSave);
	}

	public void scheduleSaving(int savePeriodInSeconds, final int keysToSave) {
		if ((saveTask) != null) {
			saveTask.cancel(false);
			saveTask = null;
		}
		if (savePeriodInSeconds > 0) {
			Runnable runnable = new Runnable() {
				public void run() {
					submitWrite(keysToSave);
				}
			};
			saveTask = ScheduledExecutors.optionalTasks.scheduleWithFixedDelay(runnable, savePeriodInSeconds, savePeriodInSeconds, TimeUnit.SECONDS);
		}
	}

	public ListenableFuture<Integer> loadSavedAsync() {
		final ListeningExecutorService es = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
		final long start = System.nanoTime();
		ListenableFuture<Integer> cacheLoad = es.submit(new Callable<Integer>() {
			@Override
			public Integer call() {
				return loadSaved();
			}
		});
		cacheLoad.addListener(new Runnable() {
			@Override
			public void run() {
				if ((size()) > 0)
					AutoSavingCache.logger.info("Completed loading ({} ms; {} keys) {} cache", TimeUnit.NANOSECONDS.toMillis(((System.nanoTime()) - start)), CacheService.instance.keyCache.size(), cacheType);

				es.shutdown();
			}
		}, MoreExecutors.directExecutor());
		return cacheLoad;
	}

	public int loadSaved() {
		int count = 0;
		long start = System.nanoTime();
		File dataPath = getCacheDataPath(AutoSavingCache.CURRENT_VERSION);
		File crcPath = getCacheCrcPath(AutoSavingCache.CURRENT_VERSION);
		if ((dataPath.exists()) && (crcPath.exists())) {
			DataInputPlus.DataInputStreamPlus in = null;
			try {
				AutoSavingCache.logger.info("reading saved cache {}", dataPath);
				in = new DataInputPlus.DataInputStreamPlus(new LengthAvailableInputStream(new BufferedInputStream(AutoSavingCache.streamFactory.getInputStream(dataPath, crcPath)), dataPath.length()));
				UUID schemaVersion = new UUID(in.readLong(), in.readLong());
				if (!(schemaVersion.equals(Schema.instance.getVersion())))
					throw new RuntimeException(((("Cache schema version " + (schemaVersion.toString())) + " does not match current schema version ") + (Schema.instance.getVersion())));

				ArrayDeque<Future<Pair<K, V>>> futures = new ArrayDeque<Future<Pair<K, V>>>();
				while ((in.available()) > 0) {
					String ksname = in.readUTF();
					String cfname = in.readUTF();
					ColumnFamilyStore cfs = Schema.instance.getColumnFamilyStoreIncludingIndexes(Pair.create(ksname, cfname));
					Future<Pair<K, V>> entryFuture = cacheLoader.deserialize(in, cfs);
					if (entryFuture == null)
						continue;

					futures.offer(entryFuture);
					count++;
					do {
						while (((futures.peek()) != null) && (futures.peek().isDone())) {
							Future<Pair<K, V>> future = futures.poll();
							Pair<K, V> entry = future.get();
							if ((entry != null) && ((entry.right) != null))
								put(entry.left, entry.right);

						} 
						if ((futures.size()) > 1000)
							Thread.yield();

					} while ((futures.size()) > 1000 );
				} 
				Future<Pair<K, V>> future = null;
				while ((future = futures.poll()) != null) {
					Pair<K, V> entry = future.get();
					if ((entry != null) && ((entry.right) != null))
						put(entry.left, entry.right);

				} 
			} catch (CorruptFileException e) {
				JVMStabilityInspector.inspectThrowable(e);
				AutoSavingCache.logger.warn(String.format("Non-fatal checksum error reading saved cache %s", dataPath.getAbsolutePath()), e);
			} catch (Throwable t) {
				JVMStabilityInspector.inspectThrowable(t);
				AutoSavingCache.logger.info(String.format("Harmless error reading saved cache %s", dataPath.getAbsolutePath()), t);
			} finally {
				FileUtils.closeQuietly(in);
			}
		}
		if (AutoSavingCache.logger.isTraceEnabled())
			AutoSavingCache.logger.trace("completed reading ({} ms; {} keys) saved cache {}", TimeUnit.NANOSECONDS.toMillis(((System.nanoTime()) - start)), count, dataPath);

		return count;
	}

	public Future<?> submitWrite(int keysToSave) {
		return null;
	}

	public class Writer extends CompactionInfo.Holder {
		private final Iterator<K> keyIterator;

		private final CompactionInfo info;

		private long keysWritten;

		private final long keysEstimate;

		protected Writer(int keysToSave) {
			int size = size();
			if ((keysToSave >= size) || (keysToSave == 0)) {
				keyIterator = keyIterator();
				keysEstimate = size;
			}else {
				keyIterator = hotKeyIterator(keysToSave);
				keysEstimate = keysToSave;
			}
			OperationType type;
			if ((cacheType) == (KEY_CACHE))
				type = OperationType.KEY_CACHE_SAVE;
			else
				if ((cacheType) == (ROW_CACHE))
					type = OperationType.ROW_CACHE_SAVE;
				else
					if ((cacheType) == (COUNTER_CACHE))
						type = OperationType.COUNTER_CACHE_SAVE;
					else
						type = OperationType.UNKNOWN;



			info = new CompactionInfo(CFMetaData.createFake(SchemaConstants.SYSTEM_KEYSPACE_NAME, cacheType.toString()), type, 0, keysEstimate, "keys", UUIDGen.getTimeUUID());
		}

		public CacheService.CacheType cacheType() {
			return cacheType;
		}

		public CompactionInfo getCompactionInfo() {
			return info.forProgress(keysWritten, Math.max(keysWritten, keysEstimate));
		}

		public void saveCache() {
			AutoSavingCache.logger.trace("Deleting old {} files.", cacheType);
			deleteOldCacheFiles();
			if (!(keyIterator.hasNext())) {
				AutoSavingCache.logger.trace("Skipping {} save, cache is empty.", cacheType);
				return;
			}
			long start = System.nanoTime();
			Pair<File, File> cacheFilePaths = tempCacheFiles();
			try (WrappedDataOutputStreamPlus writer = new WrappedDataOutputStreamPlus(AutoSavingCache.streamFactory.getOutputStream(cacheFilePaths.left, cacheFilePaths.right))) {
				UUID schemaVersion = Schema.instance.getVersion();
				if (schemaVersion == null) {
					Schema.instance.updateVersion();
					schemaVersion = Schema.instance.getVersion();
				}
				writer.writeLong(schemaVersion.getMostSignificantBits());
				writer.writeLong(schemaVersion.getLeastSignificantBits());
				while (keyIterator.hasNext()) {
					K key = keyIterator.next();
					ColumnFamilyStore cfs = Schema.instance.getColumnFamilyStoreIncludingIndexes(key.ksAndCFName);
					if (cfs == null)
						continue;

					cacheLoader.serialize(key, writer, cfs);
					(keysWritten)++;
					if ((keysWritten) >= (keysEstimate))
						break;

				} 
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new FSWriteError(e, cacheFilePaths.left);
			}
			File cacheFile = getCacheDataPath(AutoSavingCache.CURRENT_VERSION);
			File crcFile = getCacheCrcPath(AutoSavingCache.CURRENT_VERSION);
			cacheFile.delete();
			crcFile.delete();
			if (!(cacheFilePaths.left.renameTo(cacheFile)))
				AutoSavingCache.logger.error("Unable to rename {} to {}", cacheFilePaths.left, cacheFile);

			if (!(cacheFilePaths.right.renameTo(crcFile)))
				AutoSavingCache.logger.error("Unable to rename {} to {}", cacheFilePaths.right, crcFile);

			AutoSavingCache.logger.info("Saved {} ({} items) in {} ms", cacheType, keysWritten, TimeUnit.NANOSECONDS.toMillis(((System.nanoTime()) - start)));
		}

		private Pair<File, File> tempCacheFiles() {
			File dataPath = getCacheDataPath(AutoSavingCache.CURRENT_VERSION);
			File crcPath = getCacheCrcPath(AutoSavingCache.CURRENT_VERSION);
			return Pair.create(FileUtils.createTempFile(dataPath.getName(), null, dataPath.getParentFile()), FileUtils.createTempFile(crcPath.getName(), null, crcPath.getParentFile()));
		}

		private void deleteOldCacheFiles() {
			File savedCachesDir = new File(DatabaseDescriptor.getSavedCachesLocation());
			assert (savedCachesDir.exists()) && (savedCachesDir.isDirectory());
			File[] files = savedCachesDir.listFiles();
			if (files != null) {
				String cacheNameFormat = String.format("%s-%s.db", cacheType.toString(), AutoSavingCache.CURRENT_VERSION);
				for (File file : files) {
					if (!(file.isFile()))
						continue;

					if ((file.getName().endsWith(cacheNameFormat)) || (file.getName().endsWith(cacheType.toString()))) {
						if (!(file.delete()))
							AutoSavingCache.logger.warn("Failed to delete {}", file.getAbsolutePath());

					}
				}
			}else {
				AutoSavingCache.logger.warn("Could not list files in {}", savedCachesDir);
			}
		}
	}

	public interface CacheSerializer<K extends CacheKey, V> {
		void serialize(K key, DataOutputPlus out, ColumnFamilyStore cfs) throws IOException;

		Future<Pair<K, V>> deserialize(DataInputPlus in, ColumnFamilyStore cfs) throws IOException;
	}
}

