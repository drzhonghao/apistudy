

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.apache.accumulo.core.client.sample.Sampler;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.file.FileOperations;
import org.apache.accumulo.core.file.FileSKVIterator;
import org.apache.accumulo.core.file.FileSKVWriter;
import org.apache.accumulo.core.file.blockfile.impl.CachableBlockFile;
import org.apache.accumulo.core.file.streams.RateLimitedOutputStream;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
import org.apache.accumulo.core.sample.impl.SamplerFactory;
import org.apache.accumulo.core.util.ratelimit.RateLimiter;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;


public class RFileOperations extends FileOperations {
	private static final Collection<ByteSequence> EMPTY_CF_SET = Collections.emptySet();

	@Override
	protected long getFileSize(FileOperations.GetFileSizeOperation options) throws IOException {
		return options.getFileSystem().getFileStatus(new Path(options.getFilename())).getLen();
	}

	@Override
	protected FileSKVIterator openIndex(FileOperations.OpenIndexOperation options) throws IOException {
		return null;
	}

	@Override
	protected FileSKVIterator openReader(FileOperations.OpenReaderOperation options) throws IOException {
		if (options.isSeekToBeginning()) {
		}
		return null;
	}

	@Override
	protected FileSKVIterator openScanReader(FileOperations.OpenScanReaderOperation options) throws IOException {
		return null;
	}

	@Override
	protected FileSKVWriter openWriter(FileOperations.OpenWriterOperation options) throws IOException {
		AccumuloConfiguration acuconf = options.getTableConfiguration();
		long blockSize = acuconf.getMemoryInBytes(Property.TABLE_FILE_COMPRESSED_BLOCK_SIZE);
		long indexBlockSize = acuconf.getMemoryInBytes(Property.TABLE_FILE_COMPRESSED_BLOCK_SIZE_INDEX);
		SamplerConfigurationImpl samplerConfig = SamplerConfigurationImpl.newSamplerConfig(acuconf);
		Sampler sampler = null;
		if (samplerConfig != null) {
			sampler = SamplerFactory.newSampler(samplerConfig, acuconf);
		}
		String compression = options.getCompression();
		compression = (compression == null) ? options.getTableConfiguration().get(Property.TABLE_FILE_COMPRESSION_TYPE) : compression;
		FSDataOutputStream outputStream = options.getOutputStream();
		Configuration conf = options.getConfiguration();
		if (outputStream == null) {
			int hrep = conf.getInt("dfs.replication", (-1));
			int trep = acuconf.getCount(Property.TABLE_FILE_REPLICATION);
			int rep = hrep;
			if ((trep > 0) && (trep != hrep)) {
				rep = trep;
			}
			long hblock = conf.getLong("dfs.block.size", (1 << 26));
			long tblock = acuconf.getMemoryInBytes(Property.TABLE_FILE_BLOCK_SIZE);
			long block = hblock;
			if (tblock > 0)
				block = tblock;

			int bufferSize = conf.getInt("io.file.buffer.size", 4096);
			String file = options.getFilename();
			FileSystem fs = options.getFileSystem();
			outputStream = fs.create(new Path(file), false, bufferSize, ((short) (rep)), block);
		}
		CachableBlockFile.Writer _cbw = new CachableBlockFile.Writer(new RateLimitedOutputStream(outputStream, options.getRateLimiter()), compression, conf, acuconf);
		return null;
	}
}

