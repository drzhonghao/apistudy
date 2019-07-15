

import com.google.common.cache.Cache;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.file.FileSKVIterator;
import org.apache.accumulo.core.file.FileSKVWriter;
import org.apache.accumulo.core.file.blockfile.cache.BlockCache;
import org.apache.accumulo.core.file.rfile.RFile;
import org.apache.accumulo.core.util.ratelimit.RateLimiter;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;


public abstract class FileOperations {
	private static final HashSet<String> validExtensions = new HashSet<>(Arrays.asList(Constants.MAPFILE_EXTENSION, RFile.EXTENSION));

	public static Set<String> getValidExtensions() {
		return FileOperations.validExtensions;
	}

	public static String getNewFileExtension(AccumuloConfiguration acuconf) {
		return acuconf.get(Property.TABLE_FILE_TYPE);
	}

	public static FileOperations getInstance() {
		return null;
	}

	protected abstract long getFileSize(FileOperations.GetFileSizeOperation options) throws IOException;

	protected abstract FileSKVWriter openWriter(FileOperations.OpenWriterOperation options) throws IOException;

	protected abstract FileSKVIterator openIndex(FileOperations.OpenIndexOperation options) throws IOException;

	protected abstract FileSKVIterator openScanReader(FileOperations.OpenScanReaderOperation options) throws IOException;

	protected abstract FileSKVIterator openReader(FileOperations.OpenReaderOperation options) throws IOException;

	public FileOperations.NeedsFile<FileOperations.GetFileSizeOperationBuilder> getFileSize() {
		return new FileOperations.GetFileSizeOperation();
	}

	public FileOperations.NeedsFileOrOuputStream<FileOperations.OpenWriterOperationBuilder> newWriterBuilder() {
		return new FileOperations.OpenWriterOperation();
	}

	public FileOperations.NeedsFile<FileOperations.OpenIndexOperationBuilder> newIndexReaderBuilder() {
		return new FileOperations.OpenIndexOperation();
	}

	@SuppressWarnings("unchecked")
	public FileOperations.NeedsFile<FileOperations.NeedsRange<FileOperations.OpenScanReaderOperationBuilder>> newScanReaderBuilder() {
		return ((FileOperations.NeedsFile<FileOperations.NeedsRange<FileOperations.OpenScanReaderOperationBuilder>>) ((FileOperations.NeedsFile<?>) (new FileOperations.OpenScanReaderOperation())));
	}

	public FileOperations.NeedsFile<FileOperations.OpenReaderOperationBuilder> newReaderBuilder() {
		return new FileOperations.OpenReaderOperation();
	}

	protected static class FileAccessOperation<SubclassType extends FileOperations.FileAccessOperation<SubclassType>> {
		private AccumuloConfiguration tableConfiguration;

		private String filename;

		private FileSystem fs;

		private Configuration fsConf;

		@SuppressWarnings("unchecked")
		public SubclassType withTableConfiguration(AccumuloConfiguration tableConfiguration) {
			this.tableConfiguration = tableConfiguration;
			return ((SubclassType) (this));
		}

		@SuppressWarnings("unchecked")
		public SubclassType forFile(String filename, FileSystem fs, Configuration fsConf) {
			this.filename = filename;
			this.fs = fs;
			this.fsConf = fsConf;
			return ((SubclassType) (this));
		}

		@SuppressWarnings("unchecked")
		public SubclassType forFile(String filename) {
			this.filename = filename;
			return ((SubclassType) (this));
		}

		@SuppressWarnings("unchecked")
		public SubclassType inFileSystem(FileSystem fs, Configuration fsConf) {
			this.fs = fs;
			this.fsConf = fsConf;
			return ((SubclassType) (this));
		}

		protected void setFilename(String filename) {
			this.filename = filename;
		}

		public String getFilename() {
			return filename;
		}

		public FileSystem getFileSystem() {
			return fs;
		}

		protected void setConfiguration(Configuration fsConf) {
			this.fsConf = fsConf;
		}

		public Configuration getConfiguration() {
			return fsConf;
		}

		public AccumuloConfiguration getTableConfiguration() {
			return tableConfiguration;
		}

		protected void validate() {
			Objects.requireNonNull(getFilename());
			Objects.requireNonNull(getFileSystem());
			Objects.requireNonNull(getConfiguration());
			Objects.requireNonNull(getTableConfiguration());
		}
	}

	protected static interface FileAccessOperationBuilder<SubbuilderType> extends FileOperations.NeedsFile<SubbuilderType> , FileOperations.NeedsFileSystem<SubbuilderType> , FileOperations.NeedsTableConfiguration<SubbuilderType> {}

	protected class GetFileSizeOperation extends FileOperations.FileAccessOperation<FileOperations.GetFileSizeOperation> implements FileOperations.GetFileSizeOperationBuilder {
		@Override
		public long execute() throws IOException {
			validate();
			return getFileSize(this);
		}
	}

	public static interface GetFileSizeOperationBuilder extends FileOperations.FileAccessOperationBuilder<FileOperations.GetFileSizeOperationBuilder> {
		public long execute() throws IOException;
	}

	protected static class FileIOOperation<SubclassType extends FileOperations.FileIOOperation<SubclassType>> extends FileOperations.FileAccessOperation<SubclassType> {
		private RateLimiter rateLimiter;

		@SuppressWarnings("unchecked")
		public SubclassType withRateLimiter(RateLimiter rateLimiter) {
			this.rateLimiter = rateLimiter;
			return ((SubclassType) (this));
		}

		public RateLimiter getRateLimiter() {
			return rateLimiter;
		}
	}

	protected static interface FileIOOperationBuilder<SubbuilderType> extends FileOperations.FileAccessOperationBuilder<SubbuilderType> {
		public SubbuilderType withRateLimiter(RateLimiter rateLimiter);
	}

	protected class OpenWriterOperation extends FileOperations.FileIOOperation<FileOperations.OpenWriterOperation> implements FileOperations.NeedsFileOrOuputStream<FileOperations.OpenWriterOperationBuilder> , FileOperations.OpenWriterOperationBuilder {
		private String compression;

		private FSDataOutputStream outputStream;

		@Override
		public FileOperations.NeedsTableConfiguration<FileOperations.OpenWriterOperationBuilder> forOutputStream(String extenstion, FSDataOutputStream outputStream, Configuration fsConf) {
			this.outputStream = outputStream;
			setConfiguration(fsConf);
			setFilename(("foo" + extenstion));
			return this;
		}

		@Override
		public FileOperations.OpenWriterOperation withCompression(String compression) {
			this.compression = compression;
			return this;
		}

		public String getCompression() {
			return compression;
		}

		public FSDataOutputStream getOutputStream() {
			return outputStream;
		}

		@Override
		protected void validate() {
			if ((outputStream) == null) {
				super.validate();
			}else {
				Objects.requireNonNull(getConfiguration());
				Objects.requireNonNull(getTableConfiguration());
			}
		}

		@Override
		public FileSKVWriter build() throws IOException {
			validate();
			return openWriter(this);
		}
	}

	public static interface OpenWriterOperationBuilder extends FileOperations.FileIOOperationBuilder<FileOperations.OpenWriterOperationBuilder> {
		public FileOperations.OpenWriterOperationBuilder withCompression(String compression);

		public FileSKVWriter build() throws IOException;
	}

	protected static class FileReaderOperation<SubclassType extends FileOperations.FileReaderOperation<SubclassType>> extends FileOperations.FileIOOperation<SubclassType> {
		private BlockCache dataCache;

		private BlockCache indexCache;

		private Cache<String, Long> fileLenCache;

		@SuppressWarnings("unchecked")
		public SubclassType withBlockCache(BlockCache dataCache, BlockCache indexCache) {
			this.dataCache = dataCache;
			this.indexCache = indexCache;
			return ((SubclassType) (this));
		}

		@SuppressWarnings("unchecked")
		public SubclassType withDataCache(BlockCache dataCache) {
			this.dataCache = dataCache;
			return ((SubclassType) (this));
		}

		@SuppressWarnings("unchecked")
		public SubclassType withIndexCache(BlockCache indexCache) {
			this.indexCache = indexCache;
			return ((SubclassType) (this));
		}

		@SuppressWarnings("unchecked")
		public SubclassType withFileLenCache(Cache<String, Long> fileLenCache) {
			this.fileLenCache = fileLenCache;
			return ((SubclassType) (this));
		}

		public BlockCache getDataCache() {
			return dataCache;
		}

		public BlockCache getIndexCache() {
			return indexCache;
		}

		public Cache<String, Long> getFileLenCache() {
			return fileLenCache;
		}
	}

	protected static interface FileReaderOperationBuilder<SubbuilderType> extends FileOperations.FileIOOperationBuilder<SubbuilderType> {
		public SubbuilderType withBlockCache(BlockCache dataCache, BlockCache indexCache);

		public SubbuilderType withDataCache(BlockCache dataCache);

		public SubbuilderType withIndexCache(BlockCache indexCache);

		public SubbuilderType withFileLenCache(Cache<String, Long> fileLenCache);
	}

	protected class OpenIndexOperation extends FileOperations.FileReaderOperation<FileOperations.OpenIndexOperation> implements FileOperations.OpenIndexOperationBuilder {
		@Override
		public FileSKVIterator build() throws IOException {
			validate();
			return openIndex(this);
		}
	}

	public static interface OpenIndexOperationBuilder extends FileOperations.FileReaderOperationBuilder<FileOperations.OpenIndexOperationBuilder> {
		public FileSKVIterator build() throws IOException;
	}

	protected class OpenScanReaderOperation extends FileOperations.FileReaderOperation<FileOperations.OpenScanReaderOperation> implements FileOperations.OpenScanReaderOperationBuilder {
		private Range range;

		private Set<ByteSequence> columnFamilies;

		private boolean inclusive;

		@Override
		public FileOperations.OpenScanReaderOperation overRange(Range range, Set<ByteSequence> columnFamilies, boolean inclusive) {
			this.range = range;
			this.columnFamilies = columnFamilies;
			this.inclusive = inclusive;
			return this;
		}

		public Range getRange() {
			return range;
		}

		public Set<ByteSequence> getColumnFamilies() {
			return columnFamilies;
		}

		public boolean isRangeInclusive() {
			return inclusive;
		}

		@Override
		protected void validate() {
			super.validate();
			Objects.requireNonNull(range);
			Objects.requireNonNull(columnFamilies);
		}

		@Override
		public FileSKVIterator build() throws IOException {
			validate();
			return openScanReader(this);
		}
	}

	public static interface OpenScanReaderOperationBuilder extends FileOperations.FileReaderOperationBuilder<FileOperations.OpenScanReaderOperationBuilder> , FileOperations.NeedsRange<FileOperations.OpenScanReaderOperationBuilder> {
		public FileSKVIterator build() throws IOException;
	}

	protected class OpenReaderOperation extends FileOperations.FileReaderOperation<FileOperations.OpenReaderOperation> implements FileOperations.OpenReaderOperationBuilder {
		private boolean seekToBeginning = false;

		@Override
		public FileOperations.OpenReaderOperation seekToBeginning() {
			return seekToBeginning(true);
		}

		@Override
		public FileOperations.OpenReaderOperation seekToBeginning(boolean seekToBeginning) {
			this.seekToBeginning = seekToBeginning;
			return this;
		}

		public boolean isSeekToBeginning() {
			return seekToBeginning;
		}

		@Override
		public FileSKVIterator build() throws IOException {
			validate();
			return openReader(this);
		}
	}

	public static interface OpenReaderOperationBuilder extends FileOperations.FileReaderOperationBuilder<FileOperations.OpenReaderOperationBuilder> {
		public FileOperations.OpenReaderOperationBuilder seekToBeginning();

		public FileOperations.OpenReaderOperationBuilder seekToBeginning(boolean seekToBeginning);

		public FileSKVIterator build() throws IOException;
	}

	public static interface NeedsFile<ReturnType> {
		public FileOperations.NeedsTableConfiguration<ReturnType> forFile(String filename, FileSystem fs, Configuration fsConf);

		public FileOperations.NeedsFileSystem<ReturnType> forFile(String filename);
	}

	public static interface NeedsFileOrOuputStream<ReturnType> extends FileOperations.NeedsFile<ReturnType> {
		public FileOperations.NeedsTableConfiguration<ReturnType> forOutputStream(String extenstion, FSDataOutputStream out, Configuration fsConf);
	}

	public static interface NeedsFileSystem<ReturnType> {
		public FileOperations.NeedsTableConfiguration<ReturnType> inFileSystem(FileSystem fs, Configuration fsConf);
	}

	public static interface NeedsTableConfiguration<ReturnType> {
		public ReturnType withTableConfiguration(AccumuloConfiguration tableConfiguration);
	}

	public static interface NeedsRange<ReturnType> {
		public ReturnType overRange(Range range, Set<ByteSequence> columnFamilies, boolean inclusive);
	}
}

