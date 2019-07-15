

import java.io.IOException;
import java.util.Set;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.LiveDocsFormat;
import org.apache.lucene.index.CodecReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.index.SegmentCommitInfo;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.TrackingDirectoryWrapper;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.IOSupplier;
import org.apache.lucene.util.IOUtils;


class PendingDeletes {
	protected final SegmentCommitInfo info;

	private Bits liveDocs;

	private FixedBitSet writeableLiveDocs;

	protected int pendingDeleteCount;

	private boolean liveDocsInitialized;

	PendingDeletes(SegmentReader reader, SegmentCommitInfo info) {
		this(info, reader.getLiveDocs(), true);
		pendingDeleteCount = (reader.numDeletedDocs()) - (info.getDelCount());
	}

	PendingDeletes(SegmentCommitInfo info) {
		this(info, null, ((info.hasDeletions()) == false));
	}

	private PendingDeletes(SegmentCommitInfo info, Bits liveDocs, boolean liveDocsInitialized) {
		this.info = info;
		this.liveDocs = liveDocs;
		pendingDeleteCount = 0;
		this.liveDocsInitialized = liveDocsInitialized;
	}

	protected FixedBitSet getMutableBits() {
		assert liveDocsInitialized : "can't delete if liveDocs are not initialized";
		if ((writeableLiveDocs) == null) {
			if ((liveDocs) != null) {
				writeableLiveDocs = FixedBitSet.copyOf(liveDocs);
			}else {
				writeableLiveDocs = new FixedBitSet(info.info.maxDoc());
				writeableLiveDocs.set(0, info.info.maxDoc());
			}
			liveDocs = writeableLiveDocs.asReadOnlyBits();
		}
		return writeableLiveDocs;
	}

	boolean delete(int docID) throws IOException {
		assert (info.info.maxDoc()) > 0;
		FixedBitSet mutableBits = getMutableBits();
		assert mutableBits != null;
		assert (docID >= 0) && (docID < (mutableBits.length())) : (((((("out of bounds: docid=" + docID) + " liveDocsLength=") + (mutableBits.length())) + " seg=") + (info.info.name)) + " maxDoc=") + (info.info.maxDoc());
		final boolean didDelete = mutableBits.get(docID);
		if (didDelete) {
			mutableBits.clear(docID);
			(pendingDeleteCount)++;
		}
		return didDelete;
	}

	Bits getLiveDocs() {
		writeableLiveDocs = null;
		return liveDocs;
	}

	Bits getHardLiveDocs() {
		return getLiveDocs();
	}

	protected int numPendingDeletes() {
		return pendingDeleteCount;
	}

	void onNewReader(CodecReader reader, SegmentCommitInfo info) throws IOException {
		if ((liveDocsInitialized) == false) {
			assert (writeableLiveDocs) == null;
			if (reader.hasDeletions()) {
				assert (pendingDeleteCount) == 0 : "pendingDeleteCount: " + (pendingDeleteCount);
				liveDocs = reader.getLiveDocs();
				assert ((liveDocs) == null) || (assertCheckLiveDocs(liveDocs, info.info.maxDoc(), info.getDelCount()));
			}
			liveDocsInitialized = true;
		}
	}

	private boolean assertCheckLiveDocs(Bits bits, int expectedLength, int expectedDeleteCount) {
		assert (bits.length()) == expectedLength;
		int deletedCount = 0;
		for (int i = 0; i < (bits.length()); i++) {
			if ((bits.get(i)) == false) {
				deletedCount++;
			}
		}
		assert deletedCount == expectedDeleteCount : (("deleted: " + deletedCount) + " != expected: ") + expectedDeleteCount;
		return true;
	}

	void dropChanges() {
		pendingDeleteCount = 0;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("PendingDeletes(seg=").append(info);
		sb.append(" numPendingDeletes=").append(pendingDeleteCount);
		sb.append(" writeable=").append(((writeableLiveDocs) != null));
		return sb.toString();
	}

	boolean writeLiveDocs(Directory dir) throws IOException {
		if ((pendingDeleteCount) == 0) {
			return false;
		}
		Bits liveDocs = this.liveDocs;
		assert liveDocs != null;
		assert (liveDocs.length()) == (info.info.maxDoc());
		TrackingDirectoryWrapper trackingDir = new TrackingDirectoryWrapper(dir);
		boolean success = false;
		try {
			Codec codec = info.info.getCodec();
			codec.liveDocsFormat().writeLiveDocs(liveDocs, trackingDir, info, pendingDeleteCount, IOContext.DEFAULT);
			success = true;
		} finally {
			if (!success) {
				for (String fileName : trackingDir.getCreatedFiles()) {
					IOUtils.deleteFilesIgnoringExceptions(dir, fileName);
				}
			}
		}
		dropChanges();
		return true;
	}

	boolean isFullyDeleted(IOSupplier<CodecReader> readerIOSupplier) throws IOException {
		return (getDelCount()) == (info.info.maxDoc());
	}

	int numDeletesToMerge(MergePolicy policy, IOSupplier<CodecReader> readerIOSupplier) throws IOException {
		return policy.numDeletesToMerge(info, getDelCount(), readerIOSupplier);
	}

	final boolean needsRefresh(CodecReader reader) {
		return ((reader.getLiveDocs()) != (getLiveDocs())) || ((reader.numDeletedDocs()) != (getDelCount()));
	}

	final int getDelCount() {
		int delCount = ((info.getDelCount()) + (info.getSoftDelCount())) + (numPendingDeletes());
		return delCount;
	}

	final int numDocs() {
		return (info.info.maxDoc()) - (getDelCount());
	}

	boolean verifyDocCounts(CodecReader reader) {
		int count = 0;
		Bits liveDocs = getLiveDocs();
		if (liveDocs != null) {
			for (int docID = 0; docID < (info.info.maxDoc()); docID++) {
				if (liveDocs.get(docID)) {
					count++;
				}
			}
		}else {
			count = info.info.maxDoc();
		}
		assert (numDocs()) == count : (((((((((("info.maxDoc=" + (info.info.maxDoc())) + " info.getDelCount()=") + (info.getDelCount())) + " info.getSoftDelCount()=") + (info.getSoftDelCount())) + " pendingDeletes=") + (toString())) + " count=") + count) + " numDocs: ") + (numDocs());
		assert (reader.numDocs()) == (numDocs()) : (("reader.numDocs() = " + (reader.numDocs())) + " numDocs() ") + (numDocs());
		assert (reader.numDeletedDocs()) <= (info.info.maxDoc()) : (((((("delCount=" + (reader.numDeletedDocs())) + " info.maxDoc=") + (info.info.maxDoc())) + " rld.pendingDeleteCount=") + (numPendingDeletes())) + " info.getDelCount()=") + (info.getDelCount());
		return true;
	}
}

