

import java.io.Closeable;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.NoSuchFileException;
import java.util.Locale;
import java.util.Map;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.replicator.nrt.FileMetaData;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ReferenceManager;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;


public abstract class Node implements Closeable {
	public static boolean VERBOSE_FILES = true;

	public static boolean VERBOSE_CONNECTIONS = false;

	public static String PRIMARY_GEN_KEY = "__primaryGen";

	public static String VERSION_KEY = "__version";

	protected final int id;

	protected final Directory dir;

	protected final SearcherFactory searcherFactory;

	protected ReferenceManager<IndexSearcher> mgr;

	public static long globalStartNS;

	public static final long localStartNS = System.nanoTime();

	protected final PrintStream printStream;

	volatile String state = "idle";

	protected volatile Map<String, FileMetaData> lastFileMetaData;

	public Node(int id, Directory dir, SearcherFactory searcherFactory, PrintStream printStream) {
		this.id = id;
		this.dir = dir;
		this.searcherFactory = searcherFactory;
		this.printStream = printStream;
	}

	public ReferenceManager<IndexSearcher> getSearcherManager() {
		return mgr;
	}

	public Directory getDirectory() {
		return dir;
	}

	@Override
	public String toString() {
		return (((getClass().getSimpleName()) + "(id=") + (id)) + ")";
	}

	public abstract void commit() throws IOException;

	public static void nodeMessage(PrintStream printStream, String message) {
		if (printStream != null) {
			long now = System.nanoTime();
			printStream.println(String.format(Locale.ROOT, "%5.3fs %5.1fs:           [%11s] %s", ((now - (Node.globalStartNS)) / 1.0E9), ((now - (Node.localStartNS)) / 1.0E9), Thread.currentThread().getName(), message));
		}
	}

	public static void nodeMessage(PrintStream printStream, int id, String message) {
		if (printStream != null) {
			long now = System.nanoTime();
			printStream.println(String.format(Locale.ROOT, "%5.3fs %5.1fs:         N%d [%11s] %s", ((now - (Node.globalStartNS)) / 1.0E9), ((now - (Node.localStartNS)) / 1.0E9), id, Thread.currentThread().getName(), message));
		}
	}

	public void message(String message) {
		if ((printStream) != null) {
			long now = System.nanoTime();
			printStream.println(String.format(Locale.ROOT, "%5.3fs %5.1fs: %7s %2s [%11s] %s", ((now - (Node.globalStartNS)) / 1.0E9), ((now - (Node.localStartNS)) / 1.0E9), state, name(), Thread.currentThread().getName(), message));
		}
	}

	public String name() {
		return null;
	}

	public abstract boolean isClosed();

	public long getCurrentSearchingVersion() throws IOException {
		IndexSearcher searcher = mgr.acquire();
		try {
			return ((DirectoryReader) (searcher.getIndexReader())).getVersion();
		} finally {
			mgr.release(searcher);
		}
	}

	public static String bytesToString(long bytes) {
		if (bytes < 1024) {
			return bytes + " b";
		}else
			if (bytes < (1024 * 1024)) {
				return String.format(Locale.ROOT, "%.1f KB", (bytes / 1024.0));
			}else
				if (bytes < ((1024 * 1024) * 1024)) {
					return String.format(Locale.ROOT, "%.1f MB", ((bytes / 1024.0) / 1024.0));
				}else {
					return String.format(Locale.ROOT, "%.1f GB", (((bytes / 1024.0) / 1024.0) / 1024.0));
				}


	}

	public FileMetaData readLocalFileMetaData(String fileName) throws IOException {
		Map<String, FileMetaData> cache = lastFileMetaData;
		FileMetaData result;
		if (cache != null) {
			result = cache.get(fileName);
		}else {
			result = null;
		}
		if (result == null) {
			long checksum;
			long length;
			byte[] header;
			byte[] footer;
			try (IndexInput in = dir.openInput(fileName, IOContext.DEFAULT)) {
				try {
					length = in.length();
					header = CodecUtil.readIndexHeader(in);
					footer = CodecUtil.readFooter(in);
					checksum = CodecUtil.retrieveChecksum(in);
				} catch (EOFException | CorruptIndexException cie) {
					if (Node.VERBOSE_FILES) {
						message((("file " + fileName) + ": will copy [existing file is corrupt]"));
					}
					return null;
				}
				if (Node.VERBOSE_FILES) {
					message(((("file " + fileName) + " has length=") + (Node.bytesToString(length))));
				}
			} catch (FileNotFoundException | NoSuchFileException e) {
				if (Node.VERBOSE_FILES) {
					message((("file " + fileName) + ": will copy [file does not exist]"));
				}
				return null;
			}
			result = new FileMetaData(header, footer, length, checksum);
		}
		return result;
	}
}

