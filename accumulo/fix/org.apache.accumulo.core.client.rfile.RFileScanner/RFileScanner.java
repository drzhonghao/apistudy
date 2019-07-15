

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.impl.BaseIteratorEnvironment;
import org.apache.accumulo.core.client.impl.ScannerOptions;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.DefaultConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.file.blockfile.cache.BlockCache;
import org.apache.accumulo.core.file.blockfile.cache.CacheEntry;
import org.apache.accumulo.core.file.blockfile.cache.LruBlockCache;
import org.apache.accumulo.core.iterators.IteratorUtil;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.hadoop.io.Text;

import static org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope.scan;


class RFileScanner extends ScannerOptions implements Scanner {
	private static final byte[] EMPTY_BYTES = new byte[0];

	private static final Range EMPTY_RANGE = new Range();

	private Range range;

	private BlockCache dataCache = null;

	private BlockCache indexCache = null;

	private RFileScanner.Opts opts;

	private int batchSize = 1000;

	private long readaheadThreshold = 3;

	private static final long CACHE_BLOCK_SIZE = AccumuloConfiguration.getDefaultConfiguration().getMemoryInBytes(Property.TSERV_DEFAULT_BLOCKSIZE);

	static class Opts {
		Authorizations auths = Authorizations.EMPTY;

		long dataCacheSize;

		long indexCacheSize;

		boolean useSystemIterators = true;

		public HashMap<String, String> tableConfig;

		Range bounds;
	}

	private static class NoopCache implements BlockCache {
		@Override
		public CacheEntry cacheBlock(String blockName, byte[] buf, boolean inMemory) {
			return null;
		}

		@Override
		public CacheEntry cacheBlock(String blockName, byte[] buf) {
			return null;
		}

		@Override
		public CacheEntry getBlock(String blockName) {
			return null;
		}

		@Override
		public long getMaxSize() {
			return Integer.MAX_VALUE;
		}
	}

	RFileScanner(RFileScanner.Opts opts) {
		if ((!(opts.auths.equals(Authorizations.EMPTY))) && (!(opts.useSystemIterators))) {
			throw new IllegalArgumentException("Set authorizations and specified not to use system iterators");
		}
		this.opts = opts;
		if ((opts.indexCacheSize) > 0) {
			this.indexCache = new LruBlockCache(opts.indexCacheSize, RFileScanner.CACHE_BLOCK_SIZE);
		}else {
			this.indexCache = new RFileScanner.NoopCache();
		}
		if ((opts.dataCacheSize) > 0) {
			this.dataCache = new LruBlockCache(opts.dataCacheSize, RFileScanner.CACHE_BLOCK_SIZE);
		}else {
			this.dataCache = new RFileScanner.NoopCache();
		}
	}

	@Override
	public synchronized void fetchColumnFamily(Text col) {
		Preconditions.checkArgument(opts.useSystemIterators, "Can only fetch columns when using system iterators");
		super.fetchColumnFamily(col);
	}

	@Override
	public synchronized void fetchColumn(Text colFam, Text colQual) {
		Preconditions.checkArgument(opts.useSystemIterators, "Can only fetch columns when using system iterators");
		super.fetchColumn(colFam, colQual);
	}

	@Override
	public void fetchColumn(IteratorSetting.Column column) {
		Preconditions.checkArgument(opts.useSystemIterators, "Can only fetch columns when using system iterators");
		super.fetchColumn(column);
	}

	@Override
	public void setClassLoaderContext(String classLoaderContext) {
		throw new UnsupportedOperationException();
	}

	@Deprecated
	@Override
	public void setTimeOut(int timeOut) {
		if (timeOut == (Integer.MAX_VALUE))
			setTimeout(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		else
			setTimeout(timeOut, TimeUnit.SECONDS);

	}

	@Deprecated
	@Override
	public int getTimeOut() {
		long timeout = getTimeout(TimeUnit.SECONDS);
		if (timeout >= (Integer.MAX_VALUE))
			return Integer.MAX_VALUE;

		return ((int) (timeout));
	}

	@Override
	public void setRange(Range range) {
		this.range = range;
	}

	@Override
	public Range getRange() {
		return range;
	}

	@Override
	public void setBatchSize(int size) {
		this.batchSize = size;
	}

	@Override
	public int getBatchSize() {
		return batchSize;
	}

	@Override
	public void enableIsolation() {
	}

	@Override
	public void disableIsolation() {
	}

	@Override
	public synchronized void setReadaheadThreshold(long batches) {
		Preconditions.checkArgument((batches > 0));
		readaheadThreshold = batches;
	}

	@Override
	public synchronized long getReadaheadThreshold() {
		return readaheadThreshold;
	}

	@Override
	public Authorizations getAuthorizations() {
		return opts.auths;
	}

	@Override
	public void addScanIterator(IteratorSetting cfg) {
		super.addScanIterator(cfg);
	}

	@Override
	public void removeScanIterator(String iteratorName) {
		super.removeScanIterator(iteratorName);
	}

	@Override
	public void updateScanIteratorOption(String iteratorName, String key, String value) {
		super.updateScanIteratorOption(iteratorName, key, value);
	}

	private class IterEnv extends BaseIteratorEnvironment {
		@Override
		public IteratorUtil.IteratorScope getIteratorScope() {
			return scan;
		}

		@Override
		public boolean isFullMajorCompaction() {
			return false;
		}

		@Override
		public Authorizations getAuthorizations() {
			return opts.auths;
		}

		@Override
		public boolean isSamplingEnabled() {
			return (RFileScanner.this.getSamplerConfiguration()) != null;
		}

		@Override
		public SamplerConfiguration getSamplerConfiguration() {
			return RFileScanner.this.getSamplerConfiguration();
		}
	}
}

