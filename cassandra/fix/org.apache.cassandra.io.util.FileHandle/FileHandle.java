

import com.google.common.util.concurrent.RateLimiter;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.cassandra.cache.ChunkCache;
import org.apache.cassandra.io.compress.BufferType;
import org.apache.cassandra.io.compress.CompressionMetadata;
import org.apache.cassandra.io.util.ChannelProxy;
import org.apache.cassandra.io.util.ChunkReader;
import org.apache.cassandra.io.util.CompressedChunkReader;
import org.apache.cassandra.io.util.DiskOptimizationStrategy;
import org.apache.cassandra.io.util.FileDataInput;
import org.apache.cassandra.io.util.LimitingRebufferer;
import org.apache.cassandra.io.util.MmappedRegions;
import org.apache.cassandra.io.util.RandomAccessReader;
import org.apache.cassandra.io.util.ReaderFileProxy;
import org.apache.cassandra.io.util.Rebufferer;
import org.apache.cassandra.io.util.RebuffererFactory;
import org.apache.cassandra.utils.NativeLibrary;
import org.apache.cassandra.utils.Throwables;
import org.apache.cassandra.utils.concurrent.Ref;
import org.apache.cassandra.utils.concurrent.RefCounted;
import org.apache.cassandra.utils.concurrent.SharedCloseableImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FileHandle extends SharedCloseableImpl {
	private static final Logger logger = LoggerFactory.getLogger(FileHandle.class);

	public final ChannelProxy channel;

	public final long onDiskLength;

	private final RebuffererFactory rebuffererFactory;

	private final Optional<CompressionMetadata> compressionMetadata;

	private FileHandle(FileHandle.Cleanup cleanup, ChannelProxy channel, RebuffererFactory rebuffererFactory, CompressionMetadata compressionMetadata, long onDiskLength) {
		super(cleanup);
		this.rebuffererFactory = rebuffererFactory;
		this.channel = channel;
		this.compressionMetadata = Optional.ofNullable(compressionMetadata);
		this.onDiskLength = onDiskLength;
	}

	private FileHandle(FileHandle copy) {
		super(copy);
		channel = copy.channel;
		rebuffererFactory = copy.rebuffererFactory;
		compressionMetadata = copy.compressionMetadata;
		onDiskLength = copy.onDiskLength;
	}

	public String path() {
		return channel.filePath();
	}

	public long dataLength() {
		return compressionMetadata.map(( c) -> c.dataLength).orElseGet(rebuffererFactory::fileLength);
	}

	public RebuffererFactory rebuffererFactory() {
		return rebuffererFactory;
	}

	public Optional<CompressionMetadata> compressionMetadata() {
		return compressionMetadata;
	}

	@Override
	public void addTo(Ref.IdentityCollection identities) {
		super.addTo(identities);
		compressionMetadata.ifPresent(( metadata) -> metadata.addTo(identities));
	}

	@Override
	public FileHandle sharedCopy() {
		return new FileHandle(this);
	}

	public RandomAccessReader createReader() {
		return createReader(null);
	}

	public RandomAccessReader createReader(RateLimiter limiter) {
		return null;
	}

	public FileDataInput createReader(long position) {
		RandomAccessReader reader = createReader();
		reader.seek(position);
		return reader;
	}

	public void dropPageCache(long before) {
		long position = compressionMetadata.map(( metadata) -> {
			if (before >= (metadata.dataLength))
				return 0L;
			else
				return metadata.chunkFor(before).offset;

		}).orElse(before);
		NativeLibrary.trySkipCache(channel.getFileDescriptor(), 0, position, path());
	}

	private Rebufferer instantiateRebufferer(RateLimiter limiter) {
		Rebufferer rebufferer = rebuffererFactory.instantiateRebufferer();
		if (limiter != null)
			rebufferer = new LimitingRebufferer(rebufferer, limiter, DiskOptimizationStrategy.MAX_BUFFER_SIZE);

		return rebufferer;
	}

	private static class Cleanup implements RefCounted.Tidy {
		final ChannelProxy channel;

		final RebuffererFactory rebufferer;

		final CompressionMetadata compressionMetadata;

		final Optional<ChunkCache> chunkCache;

		private Cleanup(ChannelProxy channel, RebuffererFactory rebufferer, CompressionMetadata compressionMetadata, ChunkCache chunkCache) {
			this.channel = channel;
			this.rebufferer = rebufferer;
			this.compressionMetadata = compressionMetadata;
			this.chunkCache = Optional.ofNullable(chunkCache);
		}

		public String name() {
			return channel.filePath();
		}

		public void tidy() {
			chunkCache.ifPresent(( cache) -> cache.invalidateFile(name()));
			try {
				if ((compressionMetadata) != null) {
					compressionMetadata.close();
				}
			} finally {
				try {
					channel.close();
				} finally {
					rebufferer.close();
				}
			}
		}
	}

	public static class Builder implements AutoCloseable {
		private final String path;

		private ChannelProxy channel;

		private CompressionMetadata compressionMetadata;

		private MmappedRegions regions;

		private ChunkCache chunkCache;

		private int bufferSize = RandomAccessReader.DEFAULT_BUFFER_SIZE;

		private BufferType bufferType = BufferType.OFF_HEAP;

		private boolean mmapped = false;

		private boolean compressed = false;

		public Builder(String path) {
			this.path = path;
		}

		public Builder(ChannelProxy channel) {
			this.channel = channel;
			this.path = channel.filePath();
		}

		public FileHandle.Builder compressed(boolean compressed) {
			this.compressed = compressed;
			return this;
		}

		public FileHandle.Builder withChunkCache(ChunkCache chunkCache) {
			this.chunkCache = chunkCache;
			return this;
		}

		public FileHandle.Builder withCompressionMetadata(CompressionMetadata metadata) {
			this.compressed = Objects.nonNull(metadata);
			this.compressionMetadata = metadata;
			return this;
		}

		public FileHandle.Builder mmapped(boolean mmapped) {
			this.mmapped = mmapped;
			return this;
		}

		public FileHandle.Builder bufferSize(int bufferSize) {
			this.bufferSize = bufferSize;
			return this;
		}

		public FileHandle.Builder bufferType(BufferType bufferType) {
			this.bufferType = bufferType;
			return this;
		}

		public FileHandle complete() {
			return complete((-1L));
		}

		@SuppressWarnings("resource")
		public FileHandle complete(long overrideLength) {
			if ((channel) == null) {
				channel = new ChannelProxy(path);
			}
			ChannelProxy channelCopy = channel.sharedCopy();
			try {
				if ((compressed) && ((compressionMetadata) == null))
					compressionMetadata = CompressionMetadata.create(channelCopy.filePath());

				long length = (overrideLength > 0) ? overrideLength : compressed ? compressionMetadata.compressedFileLength : channelCopy.size();
				RebuffererFactory rebuffererFactory;
				if (mmapped) {
					if (compressed) {
						regions = MmappedRegions.map(channelCopy, compressionMetadata);
						rebuffererFactory = maybeCached(new CompressedChunkReader.Mmap(channelCopy, compressionMetadata, regions));
					}else {
						updateRegions(channelCopy, length);
					}
				}else {
					regions = null;
					if (compressed) {
						rebuffererFactory = maybeCached(new CompressedChunkReader.Standard(channelCopy, compressionMetadata));
					}else {
						int chunkSize = DiskOptimizationStrategy.roundForCaching(bufferSize, ChunkCache.roundUp);
					}
				}
				rebuffererFactory = null;
				FileHandle.Cleanup cleanup = new FileHandle.Cleanup(channelCopy, rebuffererFactory, compressionMetadata, chunkCache);
				return new FileHandle(cleanup, channelCopy, rebuffererFactory, compressionMetadata, length);
			} catch (Throwable t) {
				channelCopy.close();
				throw t;
			}
		}

		public Throwable close(Throwable accumulate) {
			if ((!(compressed)) && ((regions) != null))
				accumulate = regions.close(accumulate);

			if ((channel) != null)
				return channel.close(accumulate);

			return accumulate;
		}

		public void close() {
			Throwables.maybeFail(close(null));
		}

		private RebuffererFactory maybeCached(ChunkReader reader) {
			if (((chunkCache) != null) && ((chunkCache.capacity()) > 0))
				return chunkCache.wrap(reader);

			return reader;
		}

		private void updateRegions(ChannelProxy channel, long length) {
			if (((regions) != null) && (!(regions.isValid(channel)))) {
				Throwable err = regions.close(null);
				if (err != null)
					FileHandle.logger.error("Failed to close mapped regions", err);

				regions = null;
			}
			if ((regions) == null)
				regions = MmappedRegions.map(channel, length);
			else
				regions.extend(length);

		}
	}

	@Override
	public String toString() {
		return ((((((getClass().getSimpleName()) + "(path='") + (path())) + '\'') + ", length=") + (rebuffererFactory.fileLength())) + ')';
	}
}

