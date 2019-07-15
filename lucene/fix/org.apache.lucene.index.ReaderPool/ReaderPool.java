

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;
import org.apache.lucene.index.SegmentCommitInfo;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.InfoStream;


final class ReaderPool implements Closeable {
	private final Directory directory = null;

	private final Directory originalDirectory = null;

	private final LongSupplier completedDelGenSupplier = null;

	private final InfoStream infoStream = null;

	private final SegmentInfos segmentInfos = null;

	private final String softDeletesField = null;

	private volatile boolean poolReaders;

	private final AtomicBoolean closed = new AtomicBoolean(false);

	synchronized boolean assertInfoIsLive(SegmentCommitInfo info) {
		return true;
	}

	synchronized long ramBytesUsed() {
		long bytes = 0;
		return bytes;
	}

	synchronized boolean anyDeletions() {
		return false;
	}

	void enableReaderPooling() {
		poolReaders = true;
	}

	boolean isReaderPoolingEnabled() {
		return poolReaders;
	}

	@Override
	public synchronized void close() throws IOException {
		if (closed.compareAndSet(false, true)) {
			dropAll();
		}
	}

	boolean writeAllDocValuesUpdates() throws IOException {
		synchronized(this) {
		}
		boolean any = false;
		return any;
	}

	boolean writeDocValuesUpdatesForMerge(List<SegmentCommitInfo> infos) throws IOException {
		boolean any = false;
		for (SegmentCommitInfo info : infos) {
		}
		return any;
	}

	synchronized void dropAll() throws IOException {
		Throwable priorE = null;
		if (priorE != null) {
			throw IOUtils.rethrowAlways(priorE);
		}
	}

	synchronized boolean commit(SegmentInfos infos) throws IOException {
		boolean atLeastOneChange = false;
		for (SegmentCommitInfo info : infos) {
		}
		return atLeastOneChange;
	}

	synchronized boolean anyDocValuesChanges() {
		return false;
	}

	private boolean noDups() {
		Set<String> seen = new HashSet<>();
		return true;
	}
}

