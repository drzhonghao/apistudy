

import java.io.IOException;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.file.FileOperations;
import org.apache.accumulo.core.file.FileSKVIterator;
import org.apache.accumulo.core.file.FileSKVWriter;
import org.apache.accumulo.core.file.map.MapFileOperations;
import org.apache.accumulo.core.file.rfile.RFile;
import org.apache.accumulo.core.file.rfile.RFileOperations;
import org.apache.hadoop.fs.Path;


class DispatchingFileFactory extends FileOperations {
	private FileOperations findFileFactory(FileOperations.FileAccessOperation<?> options) {
		String file = options.getFilename();
		Path p = new Path(file);
		String name = p.getName();
		if (name.startsWith(((Constants.MAPFILE_EXTENSION) + "_"))) {
			return new MapFileOperations();
		}
		String[] sp = name.split("\\.");
		if ((sp.length) < 2) {
			throw new IllegalArgumentException((("File name " + name) + " has no extension"));
		}
		String extension = sp[((sp.length) - 1)];
		if ((extension.equals(Constants.MAPFILE_EXTENSION)) || (extension.equals(((Constants.MAPFILE_EXTENSION) + "_tmp")))) {
			return new MapFileOperations();
		}else
			if ((extension.equals(RFile.EXTENSION)) || (extension.equals(((RFile.EXTENSION) + "_tmp")))) {
				return new RFileOperations();
			}else {
				throw new IllegalArgumentException((("File type " + extension) + " not supported"));
			}

	}

	private static <T extends FileOperations.FileReaderOperation<T>> T selectivelyDisableCaches(T input) {
		if (!(input.getTableConfiguration().getBoolean(Property.TABLE_INDEXCACHE_ENABLED))) {
			input = input.withIndexCache(null);
		}
		if (!(input.getTableConfiguration().getBoolean(Property.TABLE_BLOCKCACHE_ENABLED))) {
			input = input.withDataCache(null);
		}
		return input;
	}

	@Override
	protected long getFileSize(FileOperations.GetFileSizeOperation options) throws IOException {
		return 0l;
	}

	@Override
	protected FileSKVWriter openWriter(FileOperations.OpenWriterOperation options) throws IOException {
		if (options.getTableConfiguration().getBoolean(Property.TABLE_BLOOM_ENABLED)) {
		}else {
		}
		return null;
	}

	@Override
	protected FileSKVIterator openIndex(FileOperations.OpenIndexOperation options) throws IOException {
		options = DispatchingFileFactory.selectivelyDisableCaches(options);
		return null;
	}

	@Override
	protected FileSKVIterator openReader(FileOperations.OpenReaderOperation options) throws IOException {
		options = DispatchingFileFactory.selectivelyDisableCaches(options);
		if (options.getTableConfiguration().getBoolean(Property.TABLE_BLOOM_ENABLED)) {
		}else {
		}
		return null;
	}

	@Override
	protected FileSKVIterator openScanReader(FileOperations.OpenScanReaderOperation options) throws IOException {
		options = DispatchingFileFactory.selectivelyDisableCaches(options);
		return null;
	}
}

