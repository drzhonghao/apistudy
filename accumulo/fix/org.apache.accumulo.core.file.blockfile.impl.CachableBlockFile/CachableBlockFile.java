

import com.google.common.cache.Cache;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.file.blockfile.ABlockReader;
import org.apache.accumulo.core.file.blockfile.ABlockWriter;
import org.apache.accumulo.core.file.blockfile.BlockFileReader;
import org.apache.accumulo.core.file.blockfile.BlockFileWriter;
import org.apache.accumulo.core.file.blockfile.cache.BlockCache;
import org.apache.accumulo.core.file.blockfile.cache.CacheEntry;
import org.apache.accumulo.core.file.blockfile.impl.SeekableByteArrayInputStream;
import org.apache.accumulo.core.file.rfile.bcfile.BCFile;
import org.apache.accumulo.core.file.streams.PositionedOutput;
import org.apache.accumulo.core.file.streams.RateLimitedInputStream;
import org.apache.accumulo.core.file.streams.RateLimitedOutputStream;
import org.apache.accumulo.core.util.ratelimit.RateLimiter;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.Seekable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CachableBlockFile {
	private CachableBlockFile() {
	}

	private static final Logger log = LoggerFactory.getLogger(CachableBlockFile.class);

	public static class Writer implements BlockFileWriter {
		private BCFile.Writer _bc;

		private CachableBlockFile.BlockWrite _bw;

		private final PositionedOutput fsout;

		private long length = 0;

		public Writer(FileSystem fs, Path fName, String compressAlgor, RateLimiter writeLimiter, Configuration conf, AccumuloConfiguration accumuloConfiguration) throws IOException {
			this(new RateLimitedOutputStream(fs.create(fName), writeLimiter), compressAlgor, conf, accumuloConfiguration);
		}

		public <OutputStreamType extends OutputStream & PositionedOutput> Writer(OutputStreamType fsout, String compressAlgor, Configuration conf, AccumuloConfiguration accumuloConfiguration) throws IOException {
			this.fsout = fsout;
			init(fsout, compressAlgor, conf, accumuloConfiguration);
		}

		private <OutputStreamT extends OutputStream & PositionedOutput> void init(OutputStreamT fsout, String compressAlgor, Configuration conf, AccumuloConfiguration accumuloConfiguration) throws IOException {
			_bc = new BCFile.Writer(fsout, compressAlgor, conf, false, accumuloConfiguration);
		}

		@Override
		public ABlockWriter prepareMetaBlock(String name) throws IOException {
			_bw = new CachableBlockFile.BlockWrite(_bc.prepareMetaBlock(name));
			return _bw;
		}

		@Override
		public ABlockWriter prepareDataBlock() throws IOException {
			_bw = new CachableBlockFile.BlockWrite(_bc.prepareDataBlock());
			return _bw;
		}

		@Override
		public void close() throws IOException {
			_bw.close();
			_bc.close();
			length = this.fsout.position();
			((OutputStream) (this.fsout)).close();
		}

		@Override
		public long getLength() throws IOException {
			return length;
		}
	}

	public static class BlockWrite extends DataOutputStream implements ABlockWriter {
		BCFile.Writer.BlockAppender _ba;

		public BlockWrite(BCFile.Writer.BlockAppender ba) {
			super(ba);
			this._ba = ba;
		}

		@Override
		public long getCompressedSize() throws IOException {
			return _ba.getCompressedSize();
		}

		@Override
		public long getRawSize() throws IOException {
			return _ba.getRawSize();
		}

		@Override
		public void close() throws IOException {
			_ba.close();
		}

		@Override
		public long getStartPos() throws IOException {
			return _ba.getStartPos();
		}
	}

	public static class Reader implements BlockFileReader {
		private final RateLimiter readLimiter;

		private BCFile.Reader _bc;

		private final String fileName;

		private BlockCache _dCache = null;

		private BlockCache _iCache = null;

		private Cache<String, Long> fileLenCache = null;

		private InputStream fin = null;

		private FileSystem fs;

		private Configuration conf;

		private boolean closed = false;

		private AccumuloConfiguration accumuloConfiguration = null;

		private static final int MAX_ARRAY_SIZE = (Integer.MAX_VALUE) - 8;

		private interface BlockLoader {
			BCFile.Reader.BlockReader get() throws IOException;

			String getInfo();
		}

		private class OffsetBlockLoader implements CachableBlockFile.Reader.BlockLoader {
			private int blockIndex;

			OffsetBlockLoader(int blockIndex) {
				this.blockIndex = blockIndex;
			}

			@Override
			public BCFile.Reader.BlockReader get() throws IOException {
				return getBCFile(accumuloConfiguration).getDataBlock(blockIndex);
			}

			@Override
			public String getInfo() {
				return "" + (blockIndex);
			}
		}

		private class RawBlockLoader implements CachableBlockFile.Reader.BlockLoader {
			private long offset;

			private long compressedSize;

			private long rawSize;

			RawBlockLoader(long offset, long compressedSize, long rawSize) {
				this.offset = offset;
				this.compressedSize = compressedSize;
				this.rawSize = rawSize;
			}

			@Override
			public BCFile.Reader.BlockReader get() throws IOException {
				return getBCFile(accumuloConfiguration).getDataBlock(offset, compressedSize, rawSize);
			}

			@Override
			public String getInfo() {
				return (((("" + (offset)) + ",") + (compressedSize)) + ",") + (rawSize);
			}
		}

		private class MetaBlockLoader implements CachableBlockFile.Reader.BlockLoader {
			private String name;

			private AccumuloConfiguration accumuloConfiguration;

			MetaBlockLoader(String name, AccumuloConfiguration accumuloConfiguration) {
				this.name = name;
				this.accumuloConfiguration = accumuloConfiguration;
			}

			@Override
			public BCFile.Reader.BlockReader get() throws IOException {
				return getBCFile(accumuloConfiguration).getMetaBlock(name);
			}

			@Override
			public String getInfo() {
				return name;
			}
		}

		public Reader(FileSystem fs, Path dataFile, Configuration conf, BlockCache data, BlockCache index, AccumuloConfiguration accumuloConfiguration) throws IOException {
			this(fs, dataFile, conf, null, data, index, null, accumuloConfiguration);
		}

		public Reader(FileSystem fs, Path dataFile, Configuration conf, Cache<String, Long> fileLenCache, BlockCache data, BlockCache index, RateLimiter readLimiter, AccumuloConfiguration accumuloConfiguration) throws IOException {
			fileName = dataFile.toString();
			this._dCache = data;
			this._iCache = index;
			this.fileLenCache = fileLenCache;
			this.fs = fs;
			this.conf = conf;
			this.accumuloConfiguration = accumuloConfiguration;
			this.readLimiter = readLimiter;
		}

		public <InputStreamType extends InputStream & Seekable> Reader(String cacheId, InputStreamType fsin, long len, Configuration conf, BlockCache data, BlockCache index, AccumuloConfiguration accumuloConfiguration) throws IOException {
			this.fileName = cacheId;
			this._dCache = data;
			this._iCache = index;
			this.readLimiter = null;
			init(fsin, len, conf, accumuloConfiguration);
		}

		public <InputStreamType extends InputStream & Seekable> Reader(String cacheId, InputStreamType fsin, long len, Configuration conf, AccumuloConfiguration accumuloConfiguration) throws IOException {
			this.fileName = cacheId;
			this.readLimiter = null;
			init(fsin, len, conf, accumuloConfiguration);
		}

		private <InputStreamT extends InputStream & Seekable> void init(InputStreamT fsin, long len, Configuration conf, AccumuloConfiguration accumuloConfiguration) throws IOException {
		}

		private long getFileLen(final Path path) throws IOException {
			try {
				return fileLenCache.get(fileName, new Callable<Long>() {
					@Override
					public Long call() throws Exception {
						return fs.getFileStatus(path).getLen();
					}
				});
			} catch (ExecutionException e) {
				throw new IOException((("Failed to get " + path) + " len from cache "), e);
			}
		}

		private synchronized BCFile.Reader getBCFile(AccumuloConfiguration accumuloConfiguration) throws IOException {
			if (closed)
				throw new IllegalStateException((("File " + (fileName)) + " is closed"));

			if ((_bc) == null) {
				final Path path = new Path(fileName);
				RateLimitedInputStream fsIn = new RateLimitedInputStream(fs.open(path), this.readLimiter);
				fin = fsIn;
				if ((fileLenCache) != null) {
					try {
						init(fsIn, getFileLen(path), conf, accumuloConfiguration);
					} catch (Exception e) {
						CachableBlockFile.log.debug("Failed to open {}, clearing file length cache and retrying", fileName, e);
						fileLenCache.invalidate(fileName);
					}
					if ((_bc) == null) {
						init(fsIn, getFileLen(path), conf, accumuloConfiguration);
					}
				}else {
					init(fsIn, fs.getFileStatus(path).getLen(), conf, accumuloConfiguration);
				}
			}
			return _bc;
		}

		public CachableBlockFile.BlockRead getCachedMetaBlock(String blockName) throws IOException {
			String _lookup = ((fileName) + "M") + blockName;
			if ((_iCache) != null) {
				CacheEntry cacheEntry = _iCache.getBlock(_lookup);
				if (cacheEntry != null) {
					return new CachableBlockFile.CachedBlockRead(cacheEntry, cacheEntry.getBuffer());
				}
			}
			return null;
		}

		public CachableBlockFile.BlockRead cacheMetaBlock(String blockName, BCFile.Reader.BlockReader _currBlock) throws IOException {
			String _lookup = ((fileName) + "M") + blockName;
			return cacheBlock(_lookup, _iCache, _currBlock, blockName);
		}

		public void cacheMetaBlock(String blockName, byte[] b) {
			if ((_iCache) == null)
				return;

			String _lookup = ((fileName) + "M") + blockName;
			try {
				_iCache.cacheBlock(_lookup, b);
			} catch (Exception e) {
				CachableBlockFile.log.warn(("Already cached block: " + _lookup), e);
			}
		}

		private CachableBlockFile.BlockRead getBlock(String _lookup, BlockCache cache, CachableBlockFile.Reader.BlockLoader loader) throws IOException {
			BCFile.Reader.BlockReader _currBlock;
			if (cache != null) {
				CacheEntry cb = null;
				cb = cache.getBlock(_lookup);
				if (cb != null) {
					return new CachableBlockFile.CachedBlockRead(cb, cb.getBuffer());
				}
			}
			_currBlock = loader.get();
			return cacheBlock(_lookup, cache, _currBlock, loader.getInfo());
		}

		private CachableBlockFile.BlockRead cacheBlock(String _lookup, BlockCache cache, BCFile.Reader.BlockReader _currBlock, String block) throws IOException {
			if ((cache == null) || ((_currBlock.getRawSize()) > (Math.min(cache.getMaxSize(), CachableBlockFile.Reader.MAX_ARRAY_SIZE)))) {
				return new CachableBlockFile.BlockRead(_currBlock, _currBlock.getRawSize());
			}else {
				byte[] b = null;
				try {
					b = new byte[((int) (_currBlock.getRawSize()))];
					_currBlock.readFully(b);
				} catch (IOException e) {
					CachableBlockFile.log.debug(((("Error full blockRead for file " + (fileName)) + " for block ") + block), e);
					throw e;
				} finally {
					_currBlock.close();
				}
				CacheEntry ce = null;
				try {
					ce = cache.cacheBlock(_lookup, b);
				} catch (Exception e) {
					CachableBlockFile.log.warn(("Already cached block: " + _lookup), e);
				}
				if (ce == null)
					return new CachableBlockFile.BlockRead(new DataInputStream(new ByteArrayInputStream(b)), b.length);
				else
					return new CachableBlockFile.CachedBlockRead(ce, ce.getBuffer());

			}
		}

		@Override
		public CachableBlockFile.BlockRead getMetaBlock(String blockName) throws IOException {
			String _lookup = ((this.fileName) + "M") + blockName;
			return getBlock(_lookup, _iCache, new CachableBlockFile.Reader.MetaBlockLoader(blockName, accumuloConfiguration));
		}

		@Override
		public ABlockReader getMetaBlock(long offset, long compressedSize, long rawSize) throws IOException {
			String _lookup = ((this.fileName) + "R") + offset;
			return getBlock(_lookup, _iCache, new CachableBlockFile.Reader.RawBlockLoader(offset, compressedSize, rawSize));
		}

		@Override
		public CachableBlockFile.BlockRead getDataBlock(int blockIndex) throws IOException {
			String _lookup = ((this.fileName) + "O") + blockIndex;
			return getBlock(_lookup, _dCache, new CachableBlockFile.Reader.OffsetBlockLoader(blockIndex));
		}

		@Override
		public ABlockReader getDataBlock(long offset, long compressedSize, long rawSize) throws IOException {
			String _lookup = ((this.fileName) + "R") + offset;
			return getBlock(_lookup, _dCache, new CachableBlockFile.Reader.RawBlockLoader(offset, compressedSize, rawSize));
		}

		@Override
		public synchronized void close() throws IOException {
			if (closed)
				return;

			closed = true;
			if ((_bc) != null)
				_bc.close();

			if ((fin) != null) {
				synchronized(fin) {
					fin.close();
				}
			}
		}
	}

	public static class CachedBlockRead extends CachableBlockFile.BlockRead {
		private SeekableByteArrayInputStream seekableInput;

		private final CacheEntry cb;

		public CachedBlockRead(CacheEntry cb, byte[] buf) {
			this(new SeekableByteArrayInputStream(buf), buf.length, cb);
		}

		private CachedBlockRead(SeekableByteArrayInputStream seekableInput, long size, CacheEntry cb) {
			super(seekableInput, size);
			this.seekableInput = seekableInput;
			this.cb = cb;
		}

		@Override
		public void seek(int position) {
			seekableInput.seek(position);
		}

		@Override
		public int getPosition() {
			return seekableInput.getPosition();
		}

		@Override
		public boolean isIndexable() {
			return true;
		}

		@Override
		public byte[] getBuffer() {
			return null;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T getIndex(Class<T> clazz) {
			T bi = null;
			synchronized(cb) {
				SoftReference<T> softRef = ((SoftReference<T>) (cb.getIndex()));
				if (softRef != null)
					bi = softRef.get();

				if (bi == null) {
					try {
						bi = clazz.newInstance();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					cb.setIndex(new SoftReference<>(bi));
				}
			}
			return bi;
		}
	}

	public static class BlockRead extends DataInputStream implements ABlockReader {
		public BlockRead(InputStream in, long size) {
			super(in);
		}

		@Override
		public DataInputStream getStream() throws IOException {
			return this;
		}

		@Override
		public boolean isIndexable() {
			return false;
		}

		@Override
		public void seek(int position) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getPosition() {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T getIndex(Class<T> clazz) {
			throw new UnsupportedOperationException();
		}

		@Override
		public byte[] getBuffer() {
			throw new UnsupportedOperationException();
		}
	}
}

