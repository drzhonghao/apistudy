

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.apache.lucene.index.BaseCompositeReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ClosedListener;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.SegmentCommitInfo;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.IOUtils;


public final class StandardDirectoryReader extends DirectoryReader {
	final IndexWriter writer;

	final SegmentInfos segmentInfos;

	private final boolean applyAllDeletes;

	private final boolean writeAllDeletes;

	StandardDirectoryReader(Directory directory, LeafReader[] readers, IndexWriter writer, SegmentInfos sis, boolean applyAllDeletes, boolean writeAllDeletes) throws IOException {
		super(directory, readers);
		this.writer = writer;
		this.segmentInfos = sis;
		this.applyAllDeletes = applyAllDeletes;
		this.writeAllDeletes = writeAllDeletes;
	}

	static DirectoryReader open(final Directory directory, final IndexCommit commit) throws IOException {
		return new SegmentInfos.FindSegmentsFile<DirectoryReader>(directory) {
			@Override
			protected DirectoryReader doBody(String segmentFileName) throws IOException {
				SegmentInfos sis = SegmentInfos.readCommit(directory, segmentFileName);
				final SegmentReader[] readers = new SegmentReader[sis.size()];
				boolean success = false;
				try {
					for (int i = (sis.size()) - 1; i >= 0; i--) {
					}
					DirectoryReader reader = new StandardDirectoryReader(directory, readers, null, sis, false, false);
					success = true;
					return reader;
				} finally {
					if (success == false) {
						IOUtils.closeWhileHandlingException(readers);
					}
				}
			}
		}.run(commit);
	}

	static DirectoryReader open(IndexWriter writer, SegmentInfos infos, boolean applyAllDeletes, boolean writeAllDeletes) throws IOException {
		final int numSegments = infos.size();
		final List<SegmentReader> readers = new ArrayList<>(numSegments);
		final Directory dir = writer.getDirectory();
		final SegmentInfos segmentInfos = infos.clone();
		int infosUpto = 0;
		try {
			for (int i = 0; i < numSegments; i++) {
				final SegmentCommitInfo info = infos.info(i);
				assert (info.info.dir) == dir;
				try {
				} finally {
				}
			}
			writer.incRefDeleter(segmentInfos);
			StandardDirectoryReader result = new StandardDirectoryReader(dir, readers.toArray(new SegmentReader[readers.size()]), writer, segmentInfos, applyAllDeletes, writeAllDeletes);
			return result;
		} catch (Throwable t) {
			try {
				IOUtils.applyToAll(readers, SegmentReader::decRef);
			} catch (Throwable t1) {
				t.addSuppressed(t1);
			}
			throw t;
		}
	}

	public static DirectoryReader open(Directory directory, SegmentInfos infos, List<? extends LeafReader> oldReaders) throws IOException {
		final Map<String, Integer> segmentReaders = (oldReaders == null) ? Collections.emptyMap() : new HashMap<>(oldReaders.size());
		if (oldReaders != null) {
			for (int i = 0, c = oldReaders.size(); i < c; i++) {
				final SegmentReader sr = ((SegmentReader) (oldReaders.get(i)));
				segmentReaders.put(sr.getSegmentName(), Integer.valueOf(i));
			}
		}
		SegmentReader[] newReaders = new SegmentReader[infos.size()];
		for (int i = (infos.size()) - 1; i >= 0; i--) {
			SegmentCommitInfo commitInfo = infos.info(i);
			Integer oldReaderIndex = segmentReaders.get(commitInfo.info.name);
			SegmentReader oldReader;
			if (oldReaderIndex == null) {
				oldReader = null;
			}else {
				oldReader = ((SegmentReader) (oldReaders.get(oldReaderIndex.intValue())));
			}
			if ((oldReader != null) && ((Arrays.equals(commitInfo.info.getId(), oldReader.getSegmentInfo().info.getId())) == false)) {
				throw new IllegalStateException((("same segment " + (commitInfo.info.name)) + " has invalid doc count change; likely you are re-opening a reader after illegally removing index files yourself and building a new index in their place.  Use IndexWriter.deleteAll or open a new IndexWriter using OpenMode.CREATE instead"));
			}
			boolean success = false;
			try {
				SegmentReader newReader;
				if ((oldReader == null) || ((commitInfo.info.getUseCompoundFile()) != (oldReader.getSegmentInfo().info.getUseCompoundFile()))) {
					newReader = null;
					newReaders[i] = newReader;
				}else {
				}
				success = true;
			} finally {
				if (!success) {
					StandardDirectoryReader.decRefWhileHandlingException(newReaders);
				}
			}
		}
		return new StandardDirectoryReader(directory, newReaders, null, infos, false, false);
	}

	private static void decRefWhileHandlingException(SegmentReader[] readers) {
		for (SegmentReader reader : readers) {
			if (reader != null) {
				try {
					reader.decRef();
				} catch (Throwable t) {
				}
			}
		}
	}

	@Override
	public String toString() {
		final StringBuilder buffer = new StringBuilder();
		buffer.append(getClass().getSimpleName());
		buffer.append('(');
		final String segmentsFile = segmentInfos.getSegmentsFileName();
		if (segmentsFile != null) {
			buffer.append(segmentsFile).append(":").append(segmentInfos.getVersion());
		}
		if ((writer) != null) {
			buffer.append(":nrt");
		}
		for (final LeafReader r : getSequentialSubReaders()) {
			buffer.append(' ');
			buffer.append(r);
		}
		buffer.append(')');
		return buffer.toString();
	}

	@Override
	protected DirectoryReader doOpenIfChanged() throws IOException {
		return doOpenIfChanged(((IndexCommit) (null)));
	}

	@Override
	protected DirectoryReader doOpenIfChanged(final IndexCommit commit) throws IOException {
		ensureOpen();
		if ((writer) != null) {
			return doOpenFromWriter(commit);
		}else {
			return doOpenNoWriter(commit);
		}
	}

	@Override
	protected DirectoryReader doOpenIfChanged(IndexWriter writer, boolean applyAllDeletes) throws IOException {
		ensureOpen();
		if ((writer == (this.writer)) && (applyAllDeletes == (this.applyAllDeletes))) {
			return doOpenFromWriter(null);
		}else {
		}
		return null;
	}

	private DirectoryReader doOpenFromWriter(IndexCommit commit) throws IOException {
		if (commit != null) {
			return doOpenFromCommit(commit);
		}
		return null;
	}

	private DirectoryReader doOpenNoWriter(IndexCommit commit) throws IOException {
		if (commit == null) {
			if (isCurrent()) {
				return null;
			}
		}else {
			if ((directory) != (commit.getDirectory())) {
				throw new IOException("the specified commit does not match the specified Directory");
			}
			if (((segmentInfos) != null) && (commit.getSegmentsFileName().equals(segmentInfos.getSegmentsFileName()))) {
				return null;
			}
		}
		return doOpenFromCommit(commit);
	}

	private DirectoryReader doOpenFromCommit(IndexCommit commit) throws IOException {
		return new SegmentInfos.FindSegmentsFile<DirectoryReader>(directory) {
			@Override
			protected DirectoryReader doBody(String segmentFileName) throws IOException {
				final SegmentInfos infos = SegmentInfos.readCommit(StandardDirectoryReader.this.directory, segmentFileName);
				return doOpenIfChanged(infos);
			}
		}.run(commit);
	}

	DirectoryReader doOpenIfChanged(SegmentInfos infos) throws IOException {
		return StandardDirectoryReader.open(directory, infos, getSequentialSubReaders());
	}

	@Override
	public long getVersion() {
		ensureOpen();
		return segmentInfos.getVersion();
	}

	public SegmentInfos getSegmentInfos() {
		return segmentInfos;
	}

	@Override
	public boolean isCurrent() throws IOException {
		ensureOpen();
		return false;
	}

	@Override
	@SuppressWarnings("try")
	protected void doClose() throws IOException {
		Closeable decRefDeleter = () -> {
			if ((writer) != null) {
				try {
					writer.decRefDeleter(segmentInfos);
				} catch (AlreadyClosedException ex) {
				}
			}
		};
		try (Closeable finalizer = decRefDeleter) {
			final List<? extends LeafReader> sequentialSubReaders = getSequentialSubReaders();
			IOUtils.applyToAll(sequentialSubReaders, LeafReader::decRef);
		}
	}

	@Override
	public IndexCommit getIndexCommit() throws IOException {
		ensureOpen();
		return new StandardDirectoryReader.ReaderCommit(this, segmentInfos, directory);
	}

	static final class ReaderCommit extends IndexCommit {
		private String segmentsFileName;

		Collection<String> files;

		Directory dir;

		long generation;

		final Map<String, String> userData;

		private final int segmentCount;

		private final StandardDirectoryReader reader;

		ReaderCommit(StandardDirectoryReader reader, SegmentInfos infos, Directory dir) throws IOException {
			segmentsFileName = infos.getSegmentsFileName();
			this.dir = dir;
			userData = infos.getUserData();
			files = Collections.unmodifiableCollection(infos.files(true));
			generation = infos.getGeneration();
			segmentCount = infos.size();
			this.reader = reader;
		}

		@Override
		public String toString() {
			return ((("StandardDirectoryReader.ReaderCommit(" + (segmentsFileName)) + " files=") + (files)) + ")";
		}

		@Override
		public int getSegmentCount() {
			return segmentCount;
		}

		@Override
		public String getSegmentsFileName() {
			return segmentsFileName;
		}

		@Override
		public Collection<String> getFileNames() {
			return files;
		}

		@Override
		public Directory getDirectory() {
			return dir;
		}

		@Override
		public long getGeneration() {
			return generation;
		}

		@Override
		public boolean isDeleted() {
			return false;
		}

		@Override
		public Map<String, String> getUserData() {
			return userData;
		}

		@Override
		public void delete() {
			throw new UnsupportedOperationException("This IndexCommit does not support deletions");
		}

		StandardDirectoryReader getReader() {
			return reader;
		}
	}

	private final Set<IndexReader.ClosedListener> readerClosedListeners = new CopyOnWriteArraySet<>();

	private final IndexReader.CacheHelper cacheHelper = new IndexReader.CacheHelper() {
		@Override
		public IndexReader.CacheKey getKey() {
			return null;
		}

		@Override
		public void addClosedListener(IndexReader.ClosedListener listener) {
			ensureOpen();
			readerClosedListeners.add(listener);
		}
	};

	void notifyReaderClosedListeners() throws IOException {
		synchronized(readerClosedListeners) {
			IOUtils.applyToAll(readerClosedListeners, ( l) -> l.onClose(cacheHelper.getKey()));
		}
	}

	@Override
	public IndexReader.CacheHelper getReaderCacheHelper() {
		return cacheHelper;
	}
}

