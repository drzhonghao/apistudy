

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.index.Term;
import org.apache.lucene.replicator.nrt.CopyJob;
import org.apache.lucene.replicator.nrt.CopyState;
import org.apache.lucene.replicator.nrt.FileMetaData;
import org.apache.lucene.replicator.nrt.Node;
import org.apache.lucene.replicator.nrt.NodeCommunicationException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ReferenceManager;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.BufferedChecksumIndexInput;
import org.apache.lucene.store.ByteArrayIndexInput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.Version;


public abstract class ReplicaNode extends Node {
	private final Collection<String> lastCommitFiles = new HashSet<>();

	protected final Collection<String> lastNRTFiles = new HashSet<>();

	protected final Set<CopyJob> mergeCopyJobs = Collections.synchronizedSet(new HashSet<>());

	protected CopyJob curNRTCopy;

	private final Lock writeFileLock;

	final Set<String> pendingMergeFiles = Collections.synchronizedSet(new HashSet<String>());

	protected long lastPrimaryGen;

	public ReplicaNode(int id, Directory dir, SearcherFactory searcherFactory, PrintStream printStream) throws IOException {
		super(id, dir, searcherFactory, printStream);
		if ((dir.getPendingDeletions().isEmpty()) == false) {
			throw new IllegalArgumentException((("Directory " + dir) + " still has pending deleted files; cannot initialize IndexWriter"));
		}
		boolean success = false;
		try {
			message(("top: init replica dir=" + dir));
			writeFileLock = dir.obtainLock(IndexWriter.WRITE_LOCK_NAME);
			success = true;
		} catch (Throwable t) {
			message("exc on init:");
			t.printStackTrace(printStream);
			throw t;
		} finally {
			if (success == false) {
				IOUtils.closeWhileHandlingException(this);
			}
		}
	}

	protected synchronized void start(long curPrimaryGen) throws IOException {
		message("top: now start");
		try {
			String segmentsFileName = SegmentInfos.getLastCommitSegmentsFileName(dir);
			long maxPendingGen = -1;
			for (String fileName : dir.listAll()) {
				if (fileName.startsWith(IndexFileNames.PENDING_SEGMENTS)) {
					long gen = Long.parseLong(fileName.substring(((IndexFileNames.PENDING_SEGMENTS.length()) + 1)), Character.MAX_RADIX);
					if (gen > maxPendingGen) {
						maxPendingGen = gen;
					}
				}
			}
			SegmentInfos infos;
			if (segmentsFileName == null) {
				infos = new SegmentInfos(Version.LATEST.major);
				message("top: init: no segments in index");
			}else {
				message(("top: init: read existing segments commit " + segmentsFileName));
				infos = SegmentInfos.readCommit(dir, segmentsFileName);
				message(((("top: init: segments: " + (infos.toString())) + " version=") + (infos.getVersion())));
				Collection<String> indexFiles = infos.files(false);
				lastCommitFiles.add(segmentsFileName);
				lastCommitFiles.addAll(indexFiles);
				lastNRTFiles.addAll(indexFiles);
				message(("top: commitFiles=" + (lastCommitFiles)));
				message(("top: nrtFiles=" + (lastNRTFiles)));
			}
			message(("top: delete unknown files on init: all files=" + (Arrays.toString(dir.listAll()))));
			message(("top: done delete unknown files on init: all files=" + (Arrays.toString(dir.listAll()))));
			String s = infos.getUserData().get(Node.PRIMARY_GEN_KEY);
			long myPrimaryGen;
			if (s == null) {
				assert (infos.size()) == 0;
				myPrimaryGen = -1;
			}else {
				myPrimaryGen = Long.parseLong(s);
			}
			message(("top: myPrimaryGen=" + myPrimaryGen));
			boolean doCommit;
			if ((((infos.size()) > 0) && (myPrimaryGen != (-1))) && (myPrimaryGen != curPrimaryGen)) {
				assert myPrimaryGen < curPrimaryGen;
				final long initSyncStartNS = System.nanoTime();
				message((((("top: init: primary changed while we were down myPrimaryGen=" + myPrimaryGen) + " vs curPrimaryGen=") + curPrimaryGen) + "; sync now before mgr init"));
				CopyJob job = null;
				message(("top: now delete starting commit point " + segmentsFileName));
				if ((dir.getPendingDeletions().isEmpty()) == false) {
					throw new RuntimeException((("replica cannot start: existing segments file=" + segmentsFileName) + " must be removed in order to start, but the file delete failed"));
				}
				boolean didRemove = lastCommitFiles.remove(segmentsFileName);
				assert didRemove;
				while (true) {
					job = newCopyJob(((("sync on startup replica=" + (name())) + " myVersion=") + (infos.getVersion())), null, null, true, null);
					job.start();
					message(("top: init: sync sis.version=" + (job.getCopyState().version)));
					try {
						job.runBlocking();
						job.finish();
						break;
					} catch (IOException ioe) {
						job.cancel("startup failed", ioe);
						if (ioe.getMessage().contains("checksum mismatch after file copy")) {
							message((("top: failed to copy: " + ioe) + "; retrying"));
						}else {
							throw ioe;
						}
					}
				} 
				lastPrimaryGen = job.getCopyState().primaryGen;
				byte[] infosBytes = job.getCopyState().infosBytes;
				SegmentInfos syncInfos = SegmentInfos.readCommit(dir, new BufferedChecksumIndexInput(new ByteArrayIndexInput("SegmentInfos", job.getCopyState().infosBytes)), job.getCopyState().gen);
				syncInfos.updateGeneration(infos);
				infos = syncInfos;
				assert (infos.getVersion()) == (job.getCopyState().version);
				message(((("  version=" + (infos.getVersion())) + " segments=") + (infos.toString())));
				message(("top: init: incRef nrtFiles=" + (job.getFileNames())));
				message(("top: init: decRef lastNRTFiles=" + (lastNRTFiles)));
				lastNRTFiles.clear();
				lastNRTFiles.addAll(job.getFileNames());
				message(("top: init: set lastNRTFiles=" + (lastNRTFiles)));
				lastFileMetaData = job.getCopyState().files;
				message(String.format(Locale.ROOT, "top: %d: start: done sync: took %.3fs for %s, opened NRT reader version=%d", id, (((System.nanoTime()) - initSyncStartNS) / 1.0E9), Node.bytesToString(job.getTotalBytesCopied()), job.getCopyState().version));
				doCommit = true;
			}else {
				doCommit = false;
				lastPrimaryGen = curPrimaryGen;
				message("top: same primary as before");
			}
			if ((infos.getGeneration()) < maxPendingGen) {
				message(((("top: move infos generation from " + (infos.getGeneration())) + " to ") + maxPendingGen));
				infos.setNextWriteGeneration(maxPendingGen);
			}
			sendNewReplica();
			if (doCommit) {
				commit();
			}
			message("top: done start");
		} catch (Throwable t) {
			if ((t.getMessage().startsWith("replica cannot start")) == false) {
				message("exc on start:");
				t.printStackTrace(printStream);
			}else {
				dir.close();
			}
			throw IOUtils.rethrowAlways(t);
		}
	}

	final Object commitLock = new Object();

	@Override
	public void commit() throws IOException {
		synchronized(commitLock) {
			SegmentInfos infos;
			Collection<String> indexFiles;
			synchronized(this) {
				infos = null;
				indexFiles = infos.files(false);
			}
			infos = null;
			message(((((("top: commit primaryGen=" + (lastPrimaryGen)) + " infos=") + (infos.toString())) + " files=") + indexFiles));
			dir.sync(indexFiles);
			Map<String, String> commitData = new HashMap<>();
			commitData.put(Node.PRIMARY_GEN_KEY, Long.toString(lastPrimaryGen));
			commitData.put(Node.VERSION_KEY, Long.toString(getCurrentSearchingVersion()));
			infos.setUserData(commitData, false);
			infos.commit(dir);
			if ((mgr) != null) {
			}
			String segmentsFileName = infos.getSegmentsFileName();
			message(((((((("top: commit wrote segments file " + segmentsFileName) + " version=") + (infos.getVersion())) + " sis=") + (infos.toString())) + " commitData=") + commitData));
			message(("top: commit decRef lastCommitFiles=" + (lastCommitFiles)));
			lastCommitFiles.clear();
			lastCommitFiles.addAll(indexFiles);
			lastCommitFiles.add(segmentsFileName);
			message(((("top: commit version=" + (infos.getVersion())) + " files now ") + (lastCommitFiles)));
		}
	}

	protected void finishNRTCopy(CopyJob job, long startNS) throws IOException {
		CopyState copyState = job.getCopyState();
		message((((("top: finishNRTCopy: version=" + (copyState.version)) + (job.getFailed() ? " FAILED" : "")) + " job=") + job));
		synchronized(this) {
			if ((curNRTCopy) == job) {
				message(("top: now clear curNRTCopy; job=" + job));
				curNRTCopy = null;
			}else {
				assert job.getFailed();
				message(("top: skip clear curNRTCopy: we were cancelled; job=" + job));
			}
			if (job.getFailed()) {
				return;
			}
			job.finish();
			byte[] infosBytes = copyState.infosBytes;
			SegmentInfos infos = SegmentInfos.readCommit(dir, new BufferedChecksumIndexInput(new ByteArrayIndexInput("SegmentInfos", copyState.infosBytes)), copyState.gen);
			assert (infos.getVersion()) == (copyState.version);
			message(((("  version=" + (infos.getVersion())) + " segments=") + (infos.toString())));
			if ((mgr) != null) {
			}
			Collection<String> newFiles = copyState.files.keySet();
			message(("top: incRef newNRTFiles=" + newFiles));
			pendingMergeFiles.removeAll(newFiles);
			message(("top: after remove from pending merges pendingMergeFiles=" + (pendingMergeFiles)));
			message(("top: decRef lastNRTFiles=" + (lastNRTFiles)));
			lastNRTFiles.clear();
			lastNRTFiles.addAll(newFiles);
			message(("top: set lastNRTFiles=" + (lastNRTFiles)));
			if ((copyState.completedMergeFiles.isEmpty()) == false) {
				message(("now remove-if-not-ref'd completed merge files: " + (copyState.completedMergeFiles)));
				for (String fileName : copyState.completedMergeFiles) {
					if (pendingMergeFiles.contains(fileName)) {
						pendingMergeFiles.remove(fileName);
					}
				}
			}
			lastFileMetaData = copyState.files;
		}
		int markerCount;
		IndexSearcher s = mgr.acquire();
		try {
			markerCount = s.count(new TermQuery(new Term("marker", "marker")));
		} finally {
			mgr.release(s);
		}
		message(String.format(Locale.ROOT, "top: done sync: took %.3fs for %s, opened NRT reader version=%d markerCount=%d", (((System.nanoTime()) - startNS) / 1.0E9), Node.bytesToString(job.getTotalBytesCopied()), copyState.version, markerCount));
	}

	protected abstract CopyJob newCopyJob(String reason, Map<String, FileMetaData> files, Map<String, FileMetaData> prevFiles, boolean highPriority, CopyJob.OnceDone onceDone) throws IOException;

	protected abstract void launch(CopyJob job);

	protected abstract void sendNewReplica() throws IOException;

	public synchronized CopyJob newNRTPoint(long newPrimaryGen, long version) throws IOException {
		if (isClosed()) {
		}
		maybeNewPrimary(newPrimaryGen);
		assert (mgr) != null;
		long curVersion = getCurrentSearchingVersion();
		message(("top: start sync sis.version=" + version));
		if (version == curVersion) {
			message("top: new NRT point has same version as current; skipping");
			return null;
		}
		if (version < curVersion) {
			message((((("top: new NRT point (version=" + version) + ") is older than current (version=") + curVersion) + "); skipping"));
			return null;
		}
		final long startNS = System.nanoTime();
		message("top: newNRTPoint");
		CopyJob job = null;
		try {
			job = newCopyJob(("NRT point sync version=" + version), null, lastFileMetaData, true, new CopyJob.OnceDone() {
				@Override
				public void run(CopyJob job) {
					try {
						finishNRTCopy(job, startNS);
					} catch (IOException ioe) {
						throw new RuntimeException(ioe);
					}
				}
			});
		} catch (NodeCommunicationException nce) {
			message(("top: ignoring communication exception creating CopyJob: " + nce));
			return null;
		}
		assert newPrimaryGen == (job.getCopyState().primaryGen);
		Collection<String> newNRTFiles = job.getFileNames();
		message(("top: newNRTPoint: job files=" + newNRTFiles));
		if ((curNRTCopy) != null) {
			job.transferAndCancel(curNRTCopy);
			assert curNRTCopy.getFailed();
		}
		curNRTCopy = job;
		for (String fileName : curNRTCopy.getFileNamesToCopy()) {
			assert (lastCommitFiles.contains(fileName)) == false : ("fileName=" + fileName) + " is in lastCommitFiles and is being copied?";
			synchronized(mergeCopyJobs) {
				for (CopyJob mergeJob : mergeCopyJobs) {
					if (mergeJob.getFileNames().contains(fileName)) {
						message((((("top: now cancel merge copy job=" + mergeJob) + ": file ") + fileName) + " is now being copied via NRT point"));
						mergeJob.cancel("newNRTPoint is copying over the same file", null);
					}
				}
			}
		}
		try {
			job.start();
		} catch (NodeCommunicationException nce) {
			message(("top: ignoring exception starting CopyJob: " + nce));
			nce.printStackTrace(printStream);
			return null;
		}
		launch(curNRTCopy);
		return curNRTCopy;
	}

	public synchronized boolean isCopying() {
		return (curNRTCopy) != null;
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public void close() throws IOException {
		message("top: now close");
		synchronized(this) {
			if ((curNRTCopy) != null) {
				curNRTCopy.cancel("closing", null);
			}
		}
		synchronized(this) {
			message("top: close mgr");
			mgr.close();
			message(("top: decRef lastNRTFiles=" + (lastNRTFiles)));
			lastNRTFiles.clear();
			lastCommitFiles.clear();
			message(("top: delete if no ref pendingMergeFiles=" + (pendingMergeFiles)));
			for (String fileName : pendingMergeFiles) {
			}
			pendingMergeFiles.clear();
			message("top: close dir");
			IOUtils.close(writeFileLock, dir);
		}
		message("top: done close");
	}

	protected synchronized void maybeNewPrimary(long newPrimaryGen) throws IOException {
		if (newPrimaryGen != (lastPrimaryGen)) {
			message(((((("top: now change lastPrimaryGen from " + (lastPrimaryGen)) + " to ") + newPrimaryGen) + " pendingMergeFiles=") + (pendingMergeFiles)));
			message(("top: delete if no ref pendingMergeFiles=" + (pendingMergeFiles)));
			for (String fileName : pendingMergeFiles) {
			}
			assert newPrimaryGen > (lastPrimaryGen) : (("newPrimaryGen=" + newPrimaryGen) + " vs lastPrimaryGen=") + (lastPrimaryGen);
			lastPrimaryGen = newPrimaryGen;
			pendingMergeFiles.clear();
		}else {
			message(("top: keep current lastPrimaryGen=" + (lastPrimaryGen)));
		}
	}

	protected synchronized CopyJob launchPreCopyMerge(AtomicBoolean finished, long newPrimaryGen, Map<String, FileMetaData> files) throws IOException {
		CopyJob job;
		maybeNewPrimary(newPrimaryGen);
		final long primaryGenStart = lastPrimaryGen;
		Set<String> fileNames = files.keySet();
		message(((("now pre-copy warm merge files=" + fileNames) + " primaryGen=") + newPrimaryGen));
		for (String fileName : fileNames) {
			assert (pendingMergeFiles.contains(fileName)) == false : ("file \"" + fileName) + "\" is already being warmed!";
			assert (lastNRTFiles.contains(fileName)) == false : ("file \"" + fileName) + "\" is already NRT visible!";
		}
		job = newCopyJob(((("warm merge on " + (name())) + " filesNames=") + fileNames), files, null, false, new CopyJob.OnceDone() {
			@Override
			public void run(CopyJob job) throws IOException {
				mergeCopyJobs.remove(job);
				message(((("done warming merge " + fileNames) + " failed?=") + (job.getFailed())));
				synchronized(this) {
					if ((job.getFailed()) == false) {
						if ((lastPrimaryGen) != primaryGenStart) {
							message(("merge pre copy finished but primary has changed; cancelling job files=" + fileNames));
							job.cancel("primary changed during merge copy", null);
						}else {
							boolean abort = false;
							for (String fileName : fileNames) {
								if (lastNRTFiles.contains(fileName)) {
									message((("abort merge finish: file " + fileName) + " is referenced by last NRT point"));
									abort = true;
								}
								if (lastCommitFiles.contains(fileName)) {
									message((("abort merge finish: file " + fileName) + " is referenced by last commit point"));
									abort = true;
								}
							}
							if (abort) {
								job.cancel("merged segment was separately copied via NRT point", null);
							}else {
								job.finish();
								message(("merge pre copy finished files=" + fileNames));
								for (String fileName : fileNames) {
									assert (pendingMergeFiles.contains(fileName)) == false : ("file \"" + fileName) + "\" is already in pendingMergeFiles";
									message((("add file " + fileName) + " to pendingMergeFiles"));
									pendingMergeFiles.add(fileName);
								}
							}
						}
					}else {
						message("merge copy finished with failure");
					}
				}
				finished.set(true);
			}
		});
		job.start();
		assert (job.getFileNamesToCopy().size()) == (files.size());
		mergeCopyJobs.add(job);
		launch(job);
		return job;
	}

	public IndexOutput createTempOutput(String prefix, String suffix, IOContext ioContext) throws IOException {
		return dir.createTempOutput(prefix, suffix, IOContext.DEFAULT);
	}

	public List<Map.Entry<String, FileMetaData>> getFilesToCopy(Map<String, FileMetaData> files) throws IOException {
		List<Map.Entry<String, FileMetaData>> toCopy = new ArrayList<>();
		for (Map.Entry<String, FileMetaData> ent : files.entrySet()) {
			String fileName = ent.getKey();
			FileMetaData fileMetaData = ent.getValue();
			if ((fileIsIdentical(fileName, fileMetaData)) == false) {
				toCopy.add(ent);
			}
		}
		return toCopy;
	}

	private boolean fileIsIdentical(String fileName, FileMetaData srcMetaData) throws IOException {
		FileMetaData destMetaData = readLocalFileMetaData(fileName);
		if (destMetaData == null) {
			return false;
		}
		if (((Arrays.equals(destMetaData.header, srcMetaData.header)) == false) || ((Arrays.equals(destMetaData.footer, srcMetaData.footer)) == false)) {
			if (Node.VERBOSE_FILES) {
				message((("file " + fileName) + ": will copy [header/footer is different]"));
			}
			return false;
		}else {
			return true;
		}
	}

	private ConcurrentMap<String, Boolean> copying = new ConcurrentHashMap<>();

	public void startCopyFile(String name) {
		if ((copying.putIfAbsent(name, Boolean.TRUE)) != null) {
			throw new IllegalStateException((("file " + name) + " is being copied in two places!"));
		}
	}

	public void finishCopyFile(String name) {
		if ((copying.remove(name)) == null) {
			throw new IllegalStateException((("file " + name) + " was not actually being copied?"));
		}
	}
}

