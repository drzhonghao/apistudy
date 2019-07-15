

import java.io.FileNotFoundException;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.store.BaseDirectory;
import org.apache.lucene.store.FSLockFactory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.OutputStreamIndexOutput;
import org.apache.lucene.util.Constants;
import org.apache.lucene.util.IOUtils;


public abstract class FSDirectory extends BaseDirectory {
	protected final Path directory;

	private final Set<String> pendingDeletes = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

	private final AtomicInteger opsSinceLastDelete = new AtomicInteger();

	private final AtomicLong nextTempFileCounter = new AtomicLong();

	protected FSDirectory(Path path, LockFactory lockFactory) throws IOException {
		super(lockFactory);
		if (!(Files.isDirectory(path))) {
			Files.createDirectories(path);
		}
		directory = path.toRealPath();
	}

	public static FSDirectory open(Path path) throws IOException {
		return FSDirectory.open(path, FSLockFactory.getDefault());
	}

	public static FSDirectory open(Path path, LockFactory lockFactory) throws IOException {
		if ((Constants.JRE_IS_64BIT) && (MMapDirectory.UNMAP_SUPPORTED)) {
		}else
			if (Constants.WINDOWS) {
			}else {
			}

		return null;
	}

	public static String[] listAll(Path dir) throws IOException {
		return FSDirectory.listAll(dir, null);
	}

	private static String[] listAll(Path dir, Set<String> skipNames) throws IOException {
		List<String> entries = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			for (Path path : stream) {
				String name = path.getFileName().toString();
				if ((skipNames == null) || ((skipNames.contains(name)) == false)) {
					entries.add(name);
				}
			}
		}
		String[] array = entries.toArray(new String[entries.size()]);
		Arrays.sort(array);
		return array;
	}

	@Override
	public String[] listAll() throws IOException {
		ensureOpen();
		return FSDirectory.listAll(directory, pendingDeletes);
	}

	@Override
	public long fileLength(String name) throws IOException {
		ensureOpen();
		if (pendingDeletes.contains(name)) {
			throw new NoSuchFileException((("file \"" + name) + "\" is pending delete"));
		}
		return Files.size(directory.resolve(name));
	}

	@Override
	public IndexOutput createOutput(String name, IOContext context) throws IOException {
		ensureOpen();
		maybeDeletePendingFiles();
		if (pendingDeletes.remove(name)) {
			privateDeleteFile(name, true);
			pendingDeletes.remove(name);
		}
		return new FSDirectory.FSIndexOutput(name);
	}

	@Override
	public IndexOutput createTempOutput(String prefix, String suffix, IOContext context) throws IOException {
		ensureOpen();
		maybeDeletePendingFiles();
		while (true) {
			try {
				String name = IndexFileNames.segmentFileName(prefix, ((suffix + "_") + (Long.toString(nextTempFileCounter.getAndIncrement(), Character.MAX_RADIX))), "tmp");
				if (pendingDeletes.contains(name)) {
					continue;
				}
				return new FSDirectory.FSIndexOutput(name, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
			} catch (FileAlreadyExistsException faee) {
			}
		} 
	}

	protected void ensureCanRead(String name) throws IOException {
		if (pendingDeletes.contains(name)) {
			throw new NoSuchFileException((("file \"" + name) + "\" is pending delete and cannot be opened for read"));
		}
	}

	@Override
	public void sync(Collection<String> names) throws IOException {
		ensureOpen();
		for (String name : names) {
			fsync(name);
		}
		maybeDeletePendingFiles();
	}

	@Override
	public void rename(String source, String dest) throws IOException {
		ensureOpen();
		if (pendingDeletes.contains(source)) {
			throw new NoSuchFileException((("file \"" + source) + "\" is pending delete and cannot be moved"));
		}
		maybeDeletePendingFiles();
		if (pendingDeletes.remove(dest)) {
			privateDeleteFile(dest, true);
			pendingDeletes.remove(dest);
		}
		Files.move(directory.resolve(source), directory.resolve(dest), StandardCopyOption.ATOMIC_MOVE);
	}

	@Override
	public void syncMetaData() throws IOException {
		ensureOpen();
		IOUtils.fsync(directory, true);
		maybeDeletePendingFiles();
	}

	@Override
	public synchronized void close() throws IOException {
		isOpen = false;
		deletePendingFiles();
	}

	public Path getDirectory() {
		ensureOpen();
		return directory;
	}

	@Override
	public String toString() {
		return ((((this.getClass().getSimpleName()) + "@") + (directory)) + " lockFactory=") + (lockFactory);
	}

	protected void fsync(String name) throws IOException {
		IOUtils.fsync(directory.resolve(name), false);
	}

	@Override
	public void deleteFile(String name) throws IOException {
		if (pendingDeletes.contains(name)) {
			throw new NoSuchFileException((("file \"" + name) + "\" is already pending delete"));
		}
		privateDeleteFile(name, false);
		maybeDeletePendingFiles();
	}

	public synchronized void deletePendingFiles() throws IOException {
		if ((pendingDeletes.isEmpty()) == false) {
			for (String name : new HashSet<>(pendingDeletes)) {
				privateDeleteFile(name, true);
			}
		}
	}

	private void maybeDeletePendingFiles() throws IOException {
		if ((pendingDeletes.isEmpty()) == false) {
			int count = opsSinceLastDelete.incrementAndGet();
			if (count >= (pendingDeletes.size())) {
				opsSinceLastDelete.addAndGet((-count));
				deletePendingFiles();
			}
		}
	}

	private void privateDeleteFile(String name, boolean isPendingDelete) throws IOException {
		try {
			Files.delete(directory.resolve(name));
			pendingDeletes.remove(name);
		} catch (NoSuchFileException | FileNotFoundException e) {
			pendingDeletes.remove(name);
			if (isPendingDelete && (Constants.WINDOWS)) {
			}else {
				throw e;
			}
		} catch (IOException ioe) {
			pendingDeletes.add(name);
		}
	}

	final class FSIndexOutput extends OutputStreamIndexOutput {
		static final int CHUNK_SIZE = 8192;

		public FSIndexOutput(String name) throws IOException {
			this(name, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
		}

		FSIndexOutput(String name, OpenOption... options) throws IOException {
			super((("FSIndexOutput(path=\"" + (directory.resolve(name))) + "\")"), name, new FilterOutputStream(Files.newOutputStream(directory.resolve(name), options)) {
				@Override
				public void write(byte[] b, int offset, int length) throws IOException {
					while (length > 0) {
						final int chunk = Math.min(length, FSDirectory.FSIndexOutput.CHUNK_SIZE);
						out.write(b, offset, chunk);
						length -= chunk;
						offset += chunk;
					} 
				}
			}, FSDirectory.FSIndexOutput.CHUNK_SIZE);
		}
	}

	@Override
	public synchronized Set<String> getPendingDeletions() throws IOException {
		deletePendingFiles();
		if (pendingDeletes.isEmpty()) {
			return Collections.emptySet();
		}else {
			return Collections.unmodifiableSet(new HashSet<>(pendingDeletes));
		}
	}
}

