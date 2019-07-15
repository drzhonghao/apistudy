

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.store.BaseDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.RAMFile;
import org.apache.lucene.store.RAMInputStream;
import org.apache.lucene.store.RAMOutputStream;
import org.apache.lucene.store.SingleInstanceLockFactory;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.Accountables;


public class RAMDirectory extends BaseDirectory implements Accountable {
	protected final Map<String, RAMFile> fileMap = new ConcurrentHashMap<>();

	protected final AtomicLong sizeInBytes = new AtomicLong();

	private final AtomicLong nextTempFileCounter = new AtomicLong();

	public RAMDirectory() {
		this(new SingleInstanceLockFactory());
	}

	public RAMDirectory(LockFactory lockFactory) {
		super(lockFactory);
	}

	public RAMDirectory(FSDirectory dir, IOContext context) throws IOException {
		this(dir, false, context);
	}

	private RAMDirectory(FSDirectory dir, boolean closeDir, IOContext context) throws IOException {
		this();
		for (String file : dir.listAll()) {
			if (!(Files.isDirectory(dir.getDirectory().resolve(file)))) {
				copyFrom(dir, file, file, context);
			}
		}
		if (closeDir) {
			dir.close();
		}
	}

	@Override
	public final String[] listAll() {
		ensureOpen();
		Set<String> fileNames = fileMap.keySet();
		List<String> names = new ArrayList<>(fileNames.size());
		for (String name : fileNames) {
			names.add(name);
		}
		String[] namesArray = names.toArray(new String[names.size()]);
		Arrays.sort(namesArray);
		return namesArray;
	}

	public final boolean fileNameExists(String name) {
		ensureOpen();
		return fileMap.containsKey(name);
	}

	@Override
	public final long fileLength(String name) throws IOException {
		ensureOpen();
		RAMFile file = fileMap.get(name);
		if (file == null) {
			throw new FileNotFoundException(name);
		}
		return file.getLength();
	}

	@Override
	public final long ramBytesUsed() {
		ensureOpen();
		return sizeInBytes.get();
	}

	@Override
	public Collection<Accountable> getChildResources() {
		return Accountables.namedAccountables("file", fileMap);
	}

	@Override
	public void deleteFile(String name) throws IOException {
		ensureOpen();
		RAMFile file = fileMap.remove(name);
		if (file != null) {
		}else {
			throw new FileNotFoundException(name);
		}
	}

	@Override
	public IndexOutput createOutput(String name, IOContext context) throws IOException {
		ensureOpen();
		RAMFile file = newRAMFile();
		if ((fileMap.putIfAbsent(name, file)) != null) {
			throw new FileAlreadyExistsException(name);
		}
		return new RAMOutputStream(name, file, true);
	}

	@Override
	public IndexOutput createTempOutput(String prefix, String suffix, IOContext context) throws IOException {
		ensureOpen();
		RAMFile file = newRAMFile();
		while (true) {
			String name = IndexFileNames.segmentFileName(prefix, ((suffix + "_") + (Long.toString(nextTempFileCounter.getAndIncrement(), Character.MAX_RADIX))), "tmp");
			if ((fileMap.putIfAbsent(name, file)) == null) {
				return new RAMOutputStream(name, file, true);
			}
		} 
	}

	protected RAMFile newRAMFile() {
		return null;
	}

	@Override
	public void sync(Collection<String> names) throws IOException {
	}

	@Override
	public void rename(String source, String dest) throws IOException {
		ensureOpen();
		RAMFile file = fileMap.get(source);
		if (file == null) {
			throw new FileNotFoundException(source);
		}
		if ((fileMap.putIfAbsent(dest, file)) != null) {
			throw new FileAlreadyExistsException(dest);
		}
		if (!(fileMap.remove(source, file))) {
			throw new IllegalStateException(("file was unexpectedly replaced: " + source));
		}
		fileMap.remove(source);
	}

	@Override
	public void syncMetaData() throws IOException {
	}

	@Override
	public IndexInput openInput(String name, IOContext context) throws IOException {
		ensureOpen();
		RAMFile file = fileMap.get(name);
		if (file == null) {
			throw new FileNotFoundException(name);
		}
		return new RAMInputStream(name, file);
	}

	@Override
	public void close() {
		isOpen = false;
		fileMap.clear();
	}
}

