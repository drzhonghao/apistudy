

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LiveIndexWriterConfig;
import org.apache.lucene.index.SegmentCommitInfo;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.index.StandardDirectoryReader;
import org.apache.lucene.replicator.nrt.CopyState;
import org.apache.lucene.replicator.nrt.FileMetaData;
import org.apache.lucene.replicator.nrt.Node;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ReferenceManager;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMFile;
import org.apache.lucene.store.RAMOutputStream;
import org.apache.lucene.util.ThreadInterruptedException;


public abstract class PrimaryNode extends Node {
	private SegmentInfos curInfos;

	protected final IndexWriter writer;

	private CopyState copyState;

	protected final long primaryGen;

	final Set<String> finishedMergedFiles = Collections.synchronizedSet(new HashSet<String>());

	private final AtomicInteger copyingCount = new AtomicInteger();

	public PrimaryNode(IndexWriter writer, int id, long primaryGen, long forcePrimaryVersion, SearcherFactory searcherFactory, PrintStream printStream) throws IOException {
		super(id, writer.getDirectory(), searcherFactory, printStream);
		message("top: now init primary");
		this.writer = writer;
		this.primaryGen = primaryGen;
		try {
			message(("IWC:\n" + (writer.getConfig())));
			message(("dir:\n" + (writer.getDirectory())));
			message(("commitData: " + (writer.getLiveCommitData())));
			Map<String, String> commitData = new HashMap<>();
			Iterable<Map.Entry<String, String>> iter = writer.getLiveCommitData();
			if (iter != null) {
				for (Map.Entry<String, String> ent : iter) {
					commitData.put(ent.getKey(), ent.getValue());
				}
			}
			commitData.put(Node.PRIMARY_GEN_KEY, Long.toString(primaryGen));
			if ((commitData.get(Node.VERSION_KEY)) == null) {
				commitData.put(Node.VERSION_KEY, "0");
				message("add initial commitData version=0");
			}else {
				message(("keep current commitData version=" + (commitData.get(Node.VERSION_KEY))));
			}
			writer.setLiveCommitData(commitData.entrySet(), false);
			if (forcePrimaryVersion != (-1)) {
				message(("now forcePrimaryVersion to version=" + forcePrimaryVersion));
				writer.advanceSegmentInfosVersion(forcePrimaryVersion);
			}
			mgr = new SearcherManager(writer, true, true, searcherFactory);
			setCurrentInfos(Collections.<String>emptySet());
			message(("init: infos version=" + (curInfos.getVersion())));
		} catch (Throwable t) {
			message("init: exception");
			t.printStackTrace(printStream);
			throw new RuntimeException(t);
		}
	}

	public long getPrimaryGen() {
		return primaryGen;
	}

	public boolean flushAndRefresh() throws IOException {
		message("top: now flushAndRefresh");
		Set<String> completedMergeFiles;
		synchronized(finishedMergedFiles) {
			completedMergeFiles = Collections.unmodifiableSet(new HashSet<>(finishedMergedFiles));
		}
		mgr.maybeRefreshBlocking();
		boolean result = setCurrentInfos(completedMergeFiles);
		if (result) {
			message(("top: opened NRT reader version=" + (curInfos.getVersion())));
			finishedMergedFiles.removeAll(completedMergeFiles);
			message(((((("flushAndRefresh: version=" + (curInfos.getVersion())) + " completedMergeFiles=") + completedMergeFiles) + " finishedMergedFiles=") + (finishedMergedFiles)));
		}else {
			message(("top: no changes in flushAndRefresh; still version=" + (curInfos.getVersion())));
		}
		return result;
	}

	public long getCopyStateVersion() {
		return copyState.version;
	}

	public synchronized long getLastCommitVersion() {
		Iterable<Map.Entry<String, String>> iter = writer.getLiveCommitData();
		assert iter != null;
		for (Map.Entry<String, String> ent : iter) {
			if (ent.getKey().equals(Node.VERSION_KEY)) {
				return Long.parseLong(ent.getValue());
			}
		}
		throw new AssertionError("missing VERSION_KEY");
	}

	@Override
	public void commit() throws IOException {
		Map<String, String> commitData = new HashMap<>();
		commitData.put(Node.PRIMARY_GEN_KEY, Long.toString(primaryGen));
		commitData.put(Node.VERSION_KEY, Long.toString(copyState.version));
		message(("top: commit commitData=" + commitData));
		writer.setLiveCommitData(commitData.entrySet(), false);
		writer.commit();
	}

	public synchronized CopyState getCopyState() throws IOException {
		ensureOpen(false);
		assert (curInfos) == (copyState.infos);
		writer.incRefDeleter(copyState.infos);
		int count = copyingCount.incrementAndGet();
		assert count > 0;
		return copyState;
	}

	public void releaseCopyState(CopyState copyState) throws IOException {
		assert (copyState.infos) != null;
		writer.decRefDeleter(copyState.infos);
		int count = copyingCount.decrementAndGet();
		assert count >= 0;
	}

	@Override
	public boolean isClosed() {
		return isClosed(false);
	}

	boolean isClosed(boolean allowClosing) {
		return false;
	}

	private void ensureOpen(boolean allowClosing) {
		if (isClosed(allowClosing)) {
		}
	}

	private synchronized boolean setCurrentInfos(Set<String> completedMergeFiles) throws IOException {
		IndexSearcher searcher = null;
		SegmentInfos infos;
		try {
			searcher = mgr.acquire();
			infos = ((StandardDirectoryReader) (searcher.getIndexReader())).getSegmentInfos();
		} finally {
			if (searcher != null) {
				mgr.release(searcher);
			}
		}
		if (((curInfos) != null) && ((infos.getVersion()) == (curInfos.getVersion()))) {
			message(((("top: skip switch to infos: version=" + (infos.getVersion())) + " is unchanged: ") + (infos.toString())));
			return false;
		}
		SegmentInfos oldInfos = curInfos;
		writer.incRefDeleter(infos);
		curInfos = infos;
		if (oldInfos != null) {
			writer.decRefDeleter(oldInfos);
		}
		message(((("top: switch to infos=" + (infos.toString())) + " version=") + (infos.getVersion())));
		RAMOutputStream out = new RAMOutputStream(new RAMFile(), true);
		infos.write(dir, out);
		byte[] infosBytes = new byte[((int) (out.getFilePointer()))];
		out.writeTo(infosBytes, 0);
		Map<String, FileMetaData> filesMetaData = new HashMap<String, FileMetaData>();
		for (SegmentCommitInfo info : infos) {
			for (String fileName : info.files()) {
				FileMetaData metaData = readLocalFileMetaData(fileName);
				assert metaData != null;
				assert (filesMetaData.containsKey(fileName)) == false;
				filesMetaData.put(fileName, metaData);
			}
		}
		lastFileMetaData = Collections.unmodifiableMap(filesMetaData);
		message(((((("top: set copyState primaryGen=" + (primaryGen)) + " version=") + (infos.getVersion())) + " files=") + (filesMetaData.keySet())));
		copyState = new CopyState(lastFileMetaData, infos.getVersion(), infos.getGeneration(), infosBytes, completedMergeFiles, primaryGen, curInfos);
		return true;
	}

	private synchronized void waitForAllRemotesToClose() throws IOException {
		while (true) {
			int count = copyingCount.get();
			if (count == 0) {
				return;
			}
			message(("pendingCopies: " + count));
			try {
				wait(10);
			} catch (InterruptedException ie) {
				throw new ThreadInterruptedException(ie);
			}
		} 
	}

	@Override
	public void close() throws IOException {
		message("top: close primary");
		synchronized(this) {
			waitForAllRemotesToClose();
			if ((curInfos) != null) {
				writer.decRefDeleter(curInfos);
				curInfos = null;
			}
		}
		mgr.close();
		writer.rollback();
		dir.close();
	}

	protected abstract void preCopyMergedSegmentFiles(SegmentCommitInfo info, Map<String, FileMetaData> files) throws IOException;
}

