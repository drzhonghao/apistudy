

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.accumulo.core.file.blockfile.cache.BlockCache;
import org.apache.accumulo.core.file.blockfile.cache.CacheEntry;
import org.apache.accumulo.core.file.blockfile.cache.CachedBlock;
import org.apache.accumulo.core.file.blockfile.cache.CachedBlockQueue;
import org.apache.accumulo.core.file.blockfile.cache.ClassSize;
import org.apache.accumulo.core.file.blockfile.cache.HeapSize;
import org.apache.accumulo.core.file.blockfile.cache.SizeConstants;
import org.apache.accumulo.core.util.NamingThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LruBlockCache implements BlockCache , HeapSize {
	private static final Logger log = LoggerFactory.getLogger(LruBlockCache.class);

	static final float DEFAULT_LOAD_FACTOR = 0.75F;

	static final int DEFAULT_CONCURRENCY_LEVEL = 16;

	static final float DEFAULT_MIN_FACTOR = 0.75F;

	static final float DEFAULT_ACCEPTABLE_FACTOR = 0.85F;

	static final float DEFAULT_SINGLE_FACTOR = 0.25F;

	static final float DEFAULT_MULTI_FACTOR = 0.5F;

	static final float DEFAULT_MEMORY_FACTOR = 0.25F;

	static final int statThreadPeriod = 60;

	private final ConcurrentHashMap<String, CachedBlock> map;

	private final ReentrantLock evictionLock = new ReentrantLock(true);

	private volatile boolean evictionInProgress = false;

	private final LruBlockCache.EvictionThread evictionThread;

	private final ScheduledExecutorService scheduleThreadPool = Executors.newScheduledThreadPool(1, new NamingThreadFactory("LRUBlockCacheStats"));

	private final AtomicLong size;

	private final AtomicLong elements;

	private final AtomicLong count;

	private final LruBlockCache.CacheStats stats;

	private long maxSize;

	private long blockSize;

	private float acceptableFactor;

	private float minFactor;

	private float singleFactor;

	private float multiFactor;

	private float memoryFactor;

	private long overhead;

	public LruBlockCache(long maxSize, long blockSize) {
		this(maxSize, blockSize, true);
	}

	public LruBlockCache(long maxSize, long blockSize, boolean evictionThread) {
		this(maxSize, blockSize, evictionThread, ((int) (Math.ceil(((1.2 * maxSize) / blockSize)))), LruBlockCache.DEFAULT_LOAD_FACTOR, LruBlockCache.DEFAULT_CONCURRENCY_LEVEL, LruBlockCache.DEFAULT_MIN_FACTOR, LruBlockCache.DEFAULT_ACCEPTABLE_FACTOR, LruBlockCache.DEFAULT_SINGLE_FACTOR, LruBlockCache.DEFAULT_MULTI_FACTOR, LruBlockCache.DEFAULT_MEMORY_FACTOR);
	}

	public LruBlockCache(long maxSize, long blockSize, boolean evictionThread, int mapInitialSize, float mapLoadFactor, int mapConcurrencyLevel, float minFactor, float acceptableFactor, float singleFactor, float multiFactor, float memoryFactor) {
		if (((singleFactor + multiFactor) + memoryFactor) != 1) {
			throw new IllegalArgumentException(("Single, multi, and memory factors " + " should total 1.0"));
		}
		if (minFactor >= acceptableFactor) {
			throw new IllegalArgumentException("minFactor must be smaller than acceptableFactor");
		}
		if ((minFactor >= 1.0F) || (acceptableFactor >= 1.0F)) {
			throw new IllegalArgumentException("all factors must be < 1");
		}
		this.maxSize = maxSize;
		this.blockSize = blockSize;
		map = new ConcurrentHashMap<>(mapInitialSize, mapLoadFactor, mapConcurrencyLevel);
		this.minFactor = minFactor;
		this.acceptableFactor = acceptableFactor;
		this.singleFactor = singleFactor;
		this.multiFactor = multiFactor;
		this.memoryFactor = memoryFactor;
		this.stats = new LruBlockCache.CacheStats();
		this.count = new AtomicLong(0);
		this.elements = new AtomicLong(0);
		this.overhead = LruBlockCache.calculateOverhead(maxSize, blockSize, mapConcurrencyLevel);
		this.size = new AtomicLong(this.overhead);
		if (evictionThread) {
			this.evictionThread = new LruBlockCache.EvictionThread(this);
			this.evictionThread.start();
			while (!(this.evictionThread.running())) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException ex) {
					throw new RuntimeException(ex);
				}
			} 
		}else {
			this.evictionThread = null;
		}
		this.scheduleThreadPool.scheduleAtFixedRate(new LruBlockCache.StatisticsThread(this), LruBlockCache.statThreadPeriod, LruBlockCache.statThreadPeriod, TimeUnit.SECONDS);
	}

	public void setMaxSize(long maxSize) {
		this.maxSize = maxSize;
		if (((this.size.get()) > (acceptableSize())) && (!(evictionInProgress))) {
			runEviction();
		}
	}

	@Override
	public CacheEntry cacheBlock(String blockName, byte[] buf, boolean inMemory) {
		CachedBlock cb = map.get(blockName);
		if (cb != null) {
			stats.duplicateReads();
			cb.access(count.incrementAndGet());
		}else {
			cb = new CachedBlock(blockName, buf, count.incrementAndGet(), inMemory);
			CachedBlock currCb = map.putIfAbsent(blockName, cb);
			if (currCb != null) {
				stats.duplicateReads();
				cb = currCb;
				cb.access(count.incrementAndGet());
			}else {
				long newSize = size.addAndGet(cb.heapSize());
				elements.incrementAndGet();
				if ((newSize > (acceptableSize())) && (!(evictionInProgress))) {
					runEviction();
				}
			}
		}
		return cb;
	}

	@Override
	public CacheEntry cacheBlock(String blockName, byte[] buf) {
		return cacheBlock(blockName, buf, false);
	}

	@Override
	public CachedBlock getBlock(String blockName) {
		CachedBlock cb = map.get(blockName);
		if (cb == null) {
			stats.miss();
			return null;
		}
		stats.hit();
		cb.access(count.incrementAndGet());
		return cb;
	}

	protected long evictBlock(CachedBlock block) {
		map.remove(block.getName());
		size.addAndGet(((-1) * (block.heapSize())));
		elements.decrementAndGet();
		stats.evicted();
		return block.heapSize();
	}

	private void runEviction() {
		if ((evictionThread) == null) {
			evict();
		}else {
			evictionThread.evict();
		}
	}

	void evict() {
		if (!(evictionLock.tryLock()))
			return;

		try {
			evictionInProgress = true;
			long bytesToFree = (size.get()) - (minSize());
			LruBlockCache.log.trace("Block cache LRU eviction started.  Attempting to free {} bytes", bytesToFree);
			if (bytesToFree <= 0)
				return;

			LruBlockCache.BlockBucket bucketSingle = new LruBlockCache.BlockBucket(bytesToFree, blockSize, singleSize());
			LruBlockCache.BlockBucket bucketMulti = new LruBlockCache.BlockBucket(bytesToFree, blockSize, multiSize());
			LruBlockCache.BlockBucket bucketMemory = new LruBlockCache.BlockBucket(bytesToFree, blockSize, memorySize());
			for (CachedBlock cachedBlock : map.values()) {
			}
			PriorityQueue<LruBlockCache.BlockBucket> bucketQueue = new PriorityQueue<>(3);
			bucketQueue.add(bucketSingle);
			bucketQueue.add(bucketMulti);
			bucketQueue.add(bucketMemory);
			int remainingBuckets = 3;
			long bytesFreed = 0;
			LruBlockCache.BlockBucket bucket;
			while ((bucket = bucketQueue.poll()) != null) {
				long overflow = bucket.overflow();
				if (overflow > 0) {
					long bucketBytesToFree = Math.min(overflow, ((long) (Math.ceil(((bytesToFree - bytesFreed) / ((double) (remainingBuckets)))))));
					bytesFreed += bucket.free(bucketBytesToFree);
				}
				remainingBuckets--;
			} 
			float singleMB = ((float) (bucketSingle.totalSize())) / ((float) (1024 * 1024));
			float multiMB = ((float) (bucketMulti.totalSize())) / ((float) (1024 * 1024));
			float memoryMB = ((float) (bucketMemory.totalSize())) / ((float) (1024 * 1024));
			LruBlockCache.log.trace(("Block cache LRU eviction completed. Freed {} bytes. Priority Sizes:" + " Single={}MB ({}), Multi={}MB ({}), Memory={}MB ({})"));
		} finally {
			stats.evict();
			evictionInProgress = false;
			evictionLock.unlock();
		}
	}

	private class BlockBucket implements Comparable<LruBlockCache.BlockBucket> {
		private CachedBlockQueue queue;

		private long totalSize = 0;

		private long bucketSize;

		public BlockBucket(long bytesToFree, long blockSize, long bucketSize) {
			this.bucketSize = bucketSize;
			queue = new CachedBlockQueue(bytesToFree, blockSize);
			totalSize = 0;
		}

		public void add(CachedBlock block) {
			totalSize += block.heapSize();
			queue.add(block);
		}

		public long free(long toFree) {
			CachedBlock[] blocks = queue.get();
			long freedBytes = 0;
			for (int i = 0; i < (blocks.length); i++) {
				freedBytes += evictBlock(blocks[i]);
				if (freedBytes >= toFree) {
					return freedBytes;
				}
			}
			return freedBytes;
		}

		public long overflow() {
			return (totalSize) - (bucketSize);
		}

		public long totalSize() {
			return totalSize;
		}

		@Override
		public int compareTo(LruBlockCache.BlockBucket that) {
			if ((this.overflow()) == (that.overflow()))
				return 0;

			return (this.overflow()) > (that.overflow()) ? 1 : -1;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(overflow());
		}

		@Override
		public boolean equals(Object that) {
			if (that instanceof LruBlockCache.BlockBucket)
				return (compareTo(((LruBlockCache.BlockBucket) (that)))) == 0;

			return false;
		}
	}

	@Override
	public long getMaxSize() {
		return this.maxSize;
	}

	public long getCurrentSize() {
		return this.size.get();
	}

	public long getFreeSize() {
		return (getMaxSize()) - (getCurrentSize());
	}

	public long size() {
		return this.elements.get();
	}

	public long getEvictionCount() {
		return this.stats.getEvictionCount();
	}

	public long getEvictedCount() {
		return this.stats.getEvictedCount();
	}

	private static class EvictionThread extends Thread {
		private WeakReference<LruBlockCache> cache;

		private boolean running = false;

		public EvictionThread(LruBlockCache cache) {
			super("LruBlockCache.EvictionThread");
			setDaemon(true);
			this.cache = new WeakReference<>(cache);
		}

		public synchronized boolean running() {
			return running;
		}

		@Override
		public void run() {
			while (true) {
				synchronized(this) {
					running = true;
					try {
						this.wait();
					} catch (InterruptedException e) {
					}
				}
				LruBlockCache cache = this.cache.get();
				if (cache == null)
					break;

				cache.evict();
			} 
		}

		public void evict() {
			synchronized(this) {
				this.notify();
			}
		}
	}

	private static class StatisticsThread extends Thread {
		LruBlockCache lru;

		public StatisticsThread(LruBlockCache lru) {
			super("LruBlockCache.StatisticsThread");
			setDaemon(true);
			this.lru = lru;
		}

		@Override
		public void run() {
			lru.logStats();
		}
	}

	public void logStats() {
		long totalSize = heapSize();
		long freeSize = (maxSize) - totalSize;
		float sizeMB = ((float) (totalSize)) / ((float) (1024 * 1024));
		float freeMB = ((float) (freeSize)) / ((float) (1024 * 1024));
		float maxMB = ((float) (maxSize)) / ((float) (1024 * 1024));
		LruBlockCache.log.debug(("Cache Stats: Sizes: Total={}MB ({}), Free={}MB ({}), Max={}MB" + ((" ({}), Counts: Blocks={}, Access={}, Hit={}, Miss={}, Evictions={}," + " Evicted={}, Ratios: Hit Ratio={}%, Miss Ratio={}%, Evicted/Run={},") + " Duplicate Reads={}")), sizeMB, totalSize);
	}

	public LruBlockCache.CacheStats getStats() {
		return this.stats;
	}

	public static class CacheStats {
		private final AtomicLong accessCount = new AtomicLong(0);

		private final AtomicLong hitCount = new AtomicLong(0);

		private final AtomicLong missCount = new AtomicLong(0);

		private final AtomicLong evictionCount = new AtomicLong(0);

		private final AtomicLong evictedCount = new AtomicLong(0);

		private final AtomicLong duplicateReads = new AtomicLong(0);

		public void miss() {
			missCount.incrementAndGet();
			accessCount.incrementAndGet();
		}

		public void hit() {
			hitCount.incrementAndGet();
			accessCount.incrementAndGet();
		}

		public void evict() {
			evictionCount.incrementAndGet();
		}

		public void duplicateReads() {
			duplicateReads.incrementAndGet();
		}

		public void evicted() {
			evictedCount.incrementAndGet();
		}

		public long getRequestCount() {
			return accessCount.get();
		}

		public long getMissCount() {
			return missCount.get();
		}

		public long getHitCount() {
			return hitCount.get();
		}

		public long getEvictionCount() {
			return evictionCount.get();
		}

		public long getDuplicateReads() {
			return duplicateReads.get();
		}

		public long getEvictedCount() {
			return evictedCount.get();
		}

		public double getHitRatio() {
			return ((float) (getHitCount())) / ((float) (getRequestCount()));
		}

		public double getMissRatio() {
			return ((float) (getMissCount())) / ((float) (getRequestCount()));
		}

		public double evictedPerEviction() {
			return ((float) (getEvictedCount())) / ((float) (getEvictionCount()));
		}
	}

	public static final long CACHE_FIXED_OVERHEAD = ClassSize.align((((((3 * (SizeConstants.SIZEOF_LONG)) + (8 * (ClassSize.REFERENCE))) + (5 * (SizeConstants.SIZEOF_FLOAT))) + (SizeConstants.SIZEOF_BOOLEAN)) + (ClassSize.OBJECT)));

	@Override
	public long heapSize() {
		return getCurrentSize();
	}

	public static long calculateOverhead(long maxSize, long blockSize, int concurrency) {
		return (((LruBlockCache.CACHE_FIXED_OVERHEAD) + (ClassSize.CONCURRENT_HASHMAP)) + (((int) (Math.ceil(((maxSize * 1.2) / blockSize)))) * (ClassSize.CONCURRENT_HASHMAP_ENTRY))) + (concurrency * (ClassSize.CONCURRENT_HASHMAP_SEGMENT));
	}

	private long acceptableSize() {
		return ((long) (Math.floor(((this.maxSize) * (this.acceptableFactor)))));
	}

	private long minSize() {
		return ((long) (Math.floor(((this.maxSize) * (this.minFactor)))));
	}

	private long singleSize() {
		return ((long) (Math.floor((((this.maxSize) * (this.singleFactor)) * (this.minFactor)))));
	}

	private long multiSize() {
		return ((long) (Math.floor((((this.maxSize) * (this.multiFactor)) * (this.minFactor)))));
	}

	private long memorySize() {
		return ((long) (Math.floor((((this.maxSize) * (this.memoryFactor)) * (this.minFactor)))));
	}

	public void shutdown() {
		this.scheduleThreadPool.shutdown();
	}
}

