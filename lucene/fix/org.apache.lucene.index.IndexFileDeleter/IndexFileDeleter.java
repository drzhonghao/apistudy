

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexDeletionPolicy;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.SegmentCommitInfo;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.CollectionUtil;
import org.apache.lucene.util.Constants;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.InfoStream;


final class IndexFileDeleter implements Closeable {
	private Map<String, IndexFileDeleter.RefCount> refCounts = new HashMap<>();

	private List<IndexFileDeleter.CommitPoint> commits = new ArrayList<>();

	private final List<String> lastFiles = new ArrayList<>();

	private List<IndexFileDeleter.CommitPoint> commitsToDelete = new ArrayList<>();

	private final InfoStream infoStream;

	private final Directory directoryOrig;

	private final Directory directory;

	private final IndexDeletionPolicy policy;

	final boolean startingCommitDeleted;

	private SegmentInfos lastSegmentInfos;

	public static boolean VERBOSE_REF_COUNTS = false;

	private final IndexWriter writer;

	private boolean locked() {
		return ((writer) == null) || (Thread.holdsLock(writer));
	}

	public IndexFileDeleter(String[] files, Directory directoryOrig, Directory directory, IndexDeletionPolicy policy, SegmentInfos segmentInfos, InfoStream infoStream, IndexWriter writer, boolean initialIndexExists, boolean isReaderInit) throws IOException {
		Objects.requireNonNull(writer);
		this.infoStream = infoStream;
		this.writer = writer;
		final String currentSegmentsFile = segmentInfos.getSegmentsFileName();
		if (infoStream.isEnabled("IFD")) {
			infoStream.message("IFD", ((("init: current segments file is \"" + currentSegmentsFile) + "\"; deletionPolicy=") + policy));
		}
		this.policy = policy;
		this.directoryOrig = directoryOrig;
		this.directory = directory;
		IndexFileDeleter.CommitPoint currentCommitPoint = null;
		if (currentSegmentsFile != null) {
			Matcher m = IndexFileNames.CODEC_FILE_PATTERN.matcher("");
			for (String fileName : files) {
				m.reset(fileName);
				if ((!(fileName.endsWith("write.lock"))) && (((m.matches()) || (fileName.startsWith(IndexFileNames.SEGMENTS))) || (fileName.startsWith(IndexFileNames.PENDING_SEGMENTS)))) {
					getRefCount(fileName);
					if ((fileName.startsWith(IndexFileNames.SEGMENTS)) && (!(fileName.equals(IndexFileNames.OLD_SEGMENTS_GEN)))) {
						if (infoStream.isEnabled("IFD")) {
							infoStream.message("IFD", (("init: load commit \"" + fileName) + "\""));
						}
						SegmentInfos sis = SegmentInfos.readCommit(directoryOrig, fileName);
						final IndexFileDeleter.CommitPoint commitPoint = new IndexFileDeleter.CommitPoint(commitsToDelete, directoryOrig, sis);
						if ((sis.getGeneration()) == (segmentInfos.getGeneration())) {
							currentCommitPoint = commitPoint;
						}
						commits.add(commitPoint);
						incRef(sis, true);
						if (((lastSegmentInfos) == null) || ((sis.getGeneration()) > (lastSegmentInfos.getGeneration()))) {
							lastSegmentInfos = sis;
						}
					}
				}
			}
		}
		if (((currentCommitPoint == null) && (currentSegmentsFile != null)) && initialIndexExists) {
			SegmentInfos sis = null;
			try {
				sis = SegmentInfos.readCommit(directoryOrig, currentSegmentsFile);
			} catch (IOException e) {
				throw new CorruptIndexException("unable to read current segments_N file", currentSegmentsFile, e);
			}
			if (infoStream.isEnabled("IFD")) {
				infoStream.message("IFD", ("forced open of current segments file " + (segmentInfos.getSegmentsFileName())));
			}
			currentCommitPoint = new IndexFileDeleter.CommitPoint(commitsToDelete, directoryOrig, sis);
			commits.add(currentCommitPoint);
			incRef(sis, true);
		}
		if (isReaderInit) {
			checkpoint(segmentInfos, false);
		}
		CollectionUtil.timSort(commits);
		Collection<String> relevantFiles = new HashSet<>(refCounts.keySet());
		Set<String> pendingDeletions = directoryOrig.getPendingDeletions();
		if ((pendingDeletions.isEmpty()) == false) {
			relevantFiles.addAll(pendingDeletions);
		}
		IndexFileDeleter.inflateGens(segmentInfos, relevantFiles, infoStream);
		Set<String> toDelete = new HashSet<>();
		for (Map.Entry<String, IndexFileDeleter.RefCount> entry : refCounts.entrySet()) {
			IndexFileDeleter.RefCount rc = entry.getValue();
			final String fileName = entry.getKey();
			if (0 == (rc.count)) {
				if (fileName.startsWith(IndexFileNames.SEGMENTS)) {
					throw new IllegalStateException((("file \"" + fileName) + "\" has refCount=0, which should never happen on init"));
				}
				if (infoStream.isEnabled("IFD")) {
					infoStream.message("IFD", (("init: removing unreferenced file \"" + fileName) + "\""));
				}
				toDelete.add(fileName);
			}
		}
		deleteFiles(toDelete);
		policy.onInit(commits);
		checkpoint(segmentInfos, false);
		if (currentCommitPoint == null) {
			startingCommitDeleted = false;
		}else {
			startingCommitDeleted = currentCommitPoint.isDeleted();
		}
		deleteCommits();
	}

	static void inflateGens(SegmentInfos infos, Collection<String> files, InfoStream infoStream) {
		long maxSegmentGen = Long.MIN_VALUE;
		long maxSegmentName = Long.MIN_VALUE;
		Map<String, Long> maxPerSegmentGen = new HashMap<>();
		for (String fileName : files) {
			if ((fileName.equals(IndexFileNames.OLD_SEGMENTS_GEN)) || (fileName.equals(IndexWriter.WRITE_LOCK_NAME))) {
			}else
				if (fileName.startsWith(IndexFileNames.SEGMENTS)) {
					try {
						maxSegmentGen = Math.max(SegmentInfos.generationFromSegmentsFileName(fileName), maxSegmentGen);
					} catch (NumberFormatException ignore) {
					}
				}else
					if (fileName.startsWith(IndexFileNames.PENDING_SEGMENTS)) {
						try {
							maxSegmentGen = Math.max(SegmentInfos.generationFromSegmentsFileName(fileName.substring(8)), maxSegmentGen);
						} catch (NumberFormatException ignore) {
						}
					}else {
						String segmentName = IndexFileNames.parseSegmentName(fileName);
						assert segmentName.startsWith("_") : "wtf? file=" + fileName;
						if (fileName.toLowerCase(Locale.ROOT).endsWith(".tmp")) {
							continue;
						}
						maxSegmentName = Math.max(maxSegmentName, Long.parseLong(segmentName.substring(1), Character.MAX_RADIX));
						Long curGen = maxPerSegmentGen.get(segmentName);
						if (curGen == null) {
							curGen = 0L;
						}
						try {
							curGen = Math.max(curGen, IndexFileNames.parseGeneration(fileName));
						} catch (NumberFormatException ignore) {
						}
						maxPerSegmentGen.put(segmentName, curGen);
					}


		}
		infos.setNextWriteGeneration(Math.max(infos.getGeneration(), maxSegmentGen));
		if ((infos.counter) < (1 + maxSegmentName)) {
			if (infoStream.isEnabled("IFD")) {
				infoStream.message("IFD", ((("init: inflate infos.counter to " + (1 + maxSegmentName)) + " vs current=") + (infos.counter)));
			}
			infos.counter = 1 + maxSegmentName;
		}
		for (SegmentCommitInfo info : infos) {
			Long gen = maxPerSegmentGen.get(info.info.name);
			assert gen != null;
			long genLong = gen;
		}
	}

	void ensureOpen() throws AlreadyClosedException {
	}

	boolean isClosed() {
		try {
			ensureOpen();
			return false;
		} catch (AlreadyClosedException ace) {
			return true;
		}
	}

	private void deleteCommits() throws IOException {
		int size = commitsToDelete.size();
		if (size > 0) {
			Throwable firstThrowable = null;
			for (int i = 0; i < size; i++) {
				IndexFileDeleter.CommitPoint commit = commitsToDelete.get(i);
				if (infoStream.isEnabled("IFD")) {
					infoStream.message("IFD", (("deleteCommits: now decRef commit \"" + (commit.getSegmentsFileName())) + "\""));
				}
				try {
					decRef(commit.files);
				} catch (Throwable t) {
					firstThrowable = IOUtils.useOrSuppress(firstThrowable, t);
				}
			}
			commitsToDelete.clear();
			if (firstThrowable != null) {
				throw IOUtils.rethrowAlways(firstThrowable);
			}
			size = commits.size();
			int readFrom = 0;
			int writeTo = 0;
			while (readFrom < size) {
				IndexFileDeleter.CommitPoint commit = commits.get(readFrom);
				if (!(commit.deleted)) {
					if (writeTo != readFrom) {
						commits.set(writeTo, commits.get(readFrom));
					}
					writeTo++;
				}
				readFrom++;
			} 
			while (size > writeTo) {
				commits.remove((size - 1));
				size--;
			} 
		}
	}

	void refresh() throws IOException {
		assert locked();
		Set<String> toDelete = new HashSet<>();
		String[] files = directory.listAll();
		Matcher m = IndexFileNames.CODEC_FILE_PATTERN.matcher("");
		for (int i = 0; i < (files.length); i++) {
			String fileName = files[i];
			m.reset(fileName);
			if (((!(fileName.endsWith("write.lock"))) && (!(refCounts.containsKey(fileName)))) && (((m.matches()) || (fileName.startsWith(IndexFileNames.SEGMENTS))) || (fileName.startsWith(IndexFileNames.PENDING_SEGMENTS)))) {
				if (infoStream.isEnabled("IFD")) {
					infoStream.message("IFD", (("refresh: removing newly created unreferenced file \"" + fileName) + "\""));
				}
				toDelete.add(fileName);
			}
		}
		deleteFiles(toDelete);
	}

	@Override
	public void close() throws IOException {
		assert locked();
		if (!(lastFiles.isEmpty())) {
			try {
				decRef(lastFiles);
			} finally {
				lastFiles.clear();
			}
		}
	}

	void revisitPolicy() throws IOException {
		assert locked();
		if (infoStream.isEnabled("IFD")) {
			infoStream.message("IFD", "now revisitPolicy");
		}
		if ((commits.size()) > 0) {
			policy.onCommit(commits);
			deleteCommits();
		}
	}

	public void checkpoint(SegmentInfos segmentInfos, boolean isCommit) throws IOException {
		assert locked();
		assert Thread.holdsLock(writer);
		long t0 = System.nanoTime();
		if (infoStream.isEnabled("IFD")) {
		}
		incRef(segmentInfos, isCommit);
		if (isCommit) {
			commits.add(new IndexFileDeleter.CommitPoint(commitsToDelete, directoryOrig, segmentInfos));
			policy.onCommit(commits);
			deleteCommits();
		}else {
			try {
				decRef(lastFiles);
			} finally {
				lastFiles.clear();
			}
			lastFiles.addAll(segmentInfos.files(false));
		}
		if (infoStream.isEnabled("IFD")) {
			long t1 = System.nanoTime();
			infoStream.message("IFD", (((t1 - t0) / 1000000) + " msec to checkpoint"));
		}
	}

	void incRef(SegmentInfos segmentInfos, boolean isCommit) throws IOException {
		assert locked();
		for (final String fileName : segmentInfos.files(isCommit)) {
			incRef(fileName);
		}
	}

	void incRef(Collection<String> files) {
		assert locked();
		for (final String file : files) {
			incRef(file);
		}
	}

	void incRef(String fileName) {
		assert locked();
		IndexFileDeleter.RefCount rc = getRefCount(fileName);
		if (infoStream.isEnabled("IFD")) {
			if (IndexFileDeleter.VERBOSE_REF_COUNTS) {
				infoStream.message("IFD", ((("  IncRef \"" + fileName) + "\": pre-incr count is ") + (rc.count)));
			}
		}
		rc.IncRef();
	}

	void decRef(Collection<String> files) throws IOException {
		assert locked();
		Set<String> toDelete = new HashSet<>();
		Throwable firstThrowable = null;
		for (final String file : files) {
			try {
				if (decRef(file)) {
					toDelete.add(file);
				}
			} catch (Throwable t) {
				firstThrowable = IOUtils.useOrSuppress(firstThrowable, t);
			}
		}
		try {
			deleteFiles(toDelete);
		} catch (Throwable t) {
			firstThrowable = IOUtils.useOrSuppress(firstThrowable, t);
		}
		if (firstThrowable != null) {
			throw IOUtils.rethrowAlways(firstThrowable);
		}
	}

	private boolean decRef(String fileName) {
		assert locked();
		IndexFileDeleter.RefCount rc = getRefCount(fileName);
		if (infoStream.isEnabled("IFD")) {
			if (IndexFileDeleter.VERBOSE_REF_COUNTS) {
				infoStream.message("IFD", ((("  DecRef \"" + fileName) + "\": pre-decr count is ") + (rc.count)));
			}
		}
		if ((rc.DecRef()) == 0) {
			refCounts.remove(fileName);
			return true;
		}else {
			return false;
		}
	}

	void decRef(SegmentInfos segmentInfos) throws IOException {
		assert locked();
		decRef(segmentInfos.files(false));
	}

	public boolean exists(String fileName) {
		assert locked();
		if (!(refCounts.containsKey(fileName))) {
			return false;
		}else {
			return (getRefCount(fileName).count) > 0;
		}
	}

	private IndexFileDeleter.RefCount getRefCount(String fileName) {
		assert locked();
		IndexFileDeleter.RefCount rc;
		if (!(refCounts.containsKey(fileName))) {
			rc = new IndexFileDeleter.RefCount(fileName);
			refCounts.put(fileName, rc);
		}else {
			rc = refCounts.get(fileName);
		}
		return rc;
	}

	void deleteNewFiles(Collection<String> files) throws IOException {
		assert locked();
		Set<String> toDelete = new HashSet<>();
		for (final String fileName : files) {
			if ((!(refCounts.containsKey(fileName))) || ((refCounts.get(fileName).count) == 0)) {
				if (infoStream.isEnabled("IFD")) {
					infoStream.message("IFD", (("will delete new file \"" + fileName) + "\""));
				}
				toDelete.add(fileName);
			}
		}
		deleteFiles(toDelete);
	}

	private void deleteFiles(Collection<String> names) throws IOException {
		assert locked();
		ensureOpen();
		if (infoStream.isEnabled("IFD")) {
			if ((names.size()) > 0) {
				infoStream.message("IFD", (("delete " + names) + ""));
			}
		}
		for (String name : names) {
			if ((name.startsWith(IndexFileNames.SEGMENTS)) == false) {
				continue;
			}
			deleteFile(name);
		}
		for (String name : names) {
			if ((name.startsWith(IndexFileNames.SEGMENTS)) == true) {
				continue;
			}
			deleteFile(name);
		}
	}

	private void deleteFile(String fileName) throws IOException {
		try {
			directory.deleteFile(fileName);
		} catch (NoSuchFileException | FileNotFoundException e) {
			if (Constants.WINDOWS) {
			}else {
				throw e;
			}
		}
	}

	private static final class RefCount {
		final String fileName;

		boolean initDone;

		RefCount(String fileName) {
			this.fileName = fileName;
		}

		int count;

		public int IncRef() {
			if (!(initDone)) {
				initDone = true;
			}else {
				assert (count) > 0 : (((Thread.currentThread().getName()) + ": RefCount is 0 pre-increment for file \"") + (fileName)) + "\"";
			}
			return ++(count);
		}

		public int DecRef() {
			assert (count) > 0 : (((Thread.currentThread().getName()) + ": RefCount is 0 pre-decrement for file \"") + (fileName)) + "\"";
			return --(count);
		}
	}

	private static final class CommitPoint extends IndexCommit {
		Collection<String> files;

		String segmentsFileName;

		boolean deleted;

		Directory directoryOrig;

		Collection<IndexFileDeleter.CommitPoint> commitsToDelete;

		long generation;

		final Map<String, String> userData;

		private final int segmentCount;

		public CommitPoint(Collection<IndexFileDeleter.CommitPoint> commitsToDelete, Directory directoryOrig, SegmentInfos segmentInfos) throws IOException {
			this.directoryOrig = directoryOrig;
			this.commitsToDelete = commitsToDelete;
			userData = segmentInfos.getUserData();
			segmentsFileName = segmentInfos.getSegmentsFileName();
			generation = segmentInfos.getGeneration();
			files = Collections.unmodifiableCollection(segmentInfos.files(true));
			segmentCount = segmentInfos.size();
		}

		@Override
		public String toString() {
			return ("IndexFileDeleter.CommitPoint(" + (segmentsFileName)) + ")";
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
			return directoryOrig;
		}

		@Override
		public long getGeneration() {
			return generation;
		}

		@Override
		public Map<String, String> getUserData() {
			return userData;
		}

		@Override
		public void delete() {
			if (!(deleted)) {
				deleted = true;
				commitsToDelete.add(this);
			}
		}

		@Override
		public boolean isDeleted() {
			return deleted;
		}
	}
}

