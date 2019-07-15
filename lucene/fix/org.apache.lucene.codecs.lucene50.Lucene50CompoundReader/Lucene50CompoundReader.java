

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.store.ChecksumIndexInput;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.apache.lucene.util.IOUtils;


final class Lucene50CompoundReader extends Directory {
	public static final class FileEntry {
		long offset;

		long length;
	}

	private final Directory directory;

	private final String segmentName;

	private final Map<String, Lucene50CompoundReader.FileEntry> entries = null;

	private IndexInput handle = null;

	private int version;

	public Lucene50CompoundReader(Directory directory, SegmentInfo si, IOContext context) throws IOException {
		this.directory = directory;
		this.segmentName = si.name;
		boolean success = false;
		for (Map.Entry<String, Lucene50CompoundReader.FileEntry> ent : entries.entrySet()) {
		}
		try {
			CodecUtil.retrieveChecksum(handle);
			success = true;
		} finally {
			if (!success) {
				IOUtils.closeWhileHandlingException(handle);
			}
		}
		handle = null;
	}

	private Map<String, Lucene50CompoundReader.FileEntry> readEntries(byte[] segmentID, Directory dir, String entriesFileName) throws IOException {
		Map<String, Lucene50CompoundReader.FileEntry> mapping = null;
		try (ChecksumIndexInput entriesStream = dir.openChecksumInput(entriesFileName, IOContext.READONCE)) {
			Throwable priorE = null;
			try {
				final int numEntries = entriesStream.readVInt();
				mapping = new HashMap<>(numEntries);
				for (int i = 0; i < numEntries; i++) {
					final Lucene50CompoundReader.FileEntry fileEntry = new Lucene50CompoundReader.FileEntry();
					final String id = entriesStream.readString();
					Lucene50CompoundReader.FileEntry previous = mapping.put(id, fileEntry);
					if (previous != null) {
						throw new CorruptIndexException((("Duplicate cfs entry id=" + id) + " in CFS "), entriesStream);
					}
					fileEntry.offset = entriesStream.readLong();
					fileEntry.length = entriesStream.readLong();
				}
			} catch (Throwable exception) {
				priorE = exception;
			} finally {
				CodecUtil.checkFooter(entriesStream, priorE);
			}
		}
		return Collections.unmodifiableMap(mapping);
	}

	@Override
	public void close() throws IOException {
		IOUtils.close(handle);
	}

	@Override
	public IndexInput openInput(String name, IOContext context) throws IOException {
		ensureOpen();
		final String id = IndexFileNames.stripSegmentName(name);
		final Lucene50CompoundReader.FileEntry entry = entries.get(id);
		if (entry == null) {
		}
		return handle.slice(name, entry.offset, entry.length);
	}

	@Override
	public String[] listAll() {
		ensureOpen();
		String[] res = entries.keySet().toArray(new String[entries.size()]);
		for (int i = 0; i < (res.length); i++) {
			res[i] = (segmentName) + (res[i]);
		}
		return res;
	}

	@Override
	public void deleteFile(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void rename(String from, String to) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void syncMetaData() {
	}

	@Override
	public long fileLength(String name) throws IOException {
		ensureOpen();
		Lucene50CompoundReader.FileEntry e = entries.get(IndexFileNames.stripSegmentName(name));
		if (e == null)
			throw new FileNotFoundException(name);

		return e.length;
	}

	@Override
	public IndexOutput createOutput(String name, IOContext context) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IndexOutput createTempOutput(String prefix, String suffix, IOContext context) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sync(Collection<String> names) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Lock obtainLock(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return ((("CompoundFileDirectory(segment=\"" + (segmentName)) + "\" in dir=") + (directory)) + ")";
	}
}

