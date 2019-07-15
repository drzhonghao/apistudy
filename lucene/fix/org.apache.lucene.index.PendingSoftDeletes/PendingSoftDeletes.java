

import java.io.IOException;
import org.apache.lucene.index.CodecReader;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.index.SegmentCommitInfo;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.DocValuesFieldExistsQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.IOSupplier;


final class PendingSoftDeletes {
	private final String field;

	private long dvGeneration = -2;

	PendingSoftDeletes(String field, SegmentCommitInfo info) {
		this.field = field;
	}

	PendingSoftDeletes(String field, SegmentReader reader, SegmentCommitInfo info) {
		this.field = field;
	}

	boolean delete(int docID) throws IOException {
		return false;
	}

	protected int numPendingDeletes() {
		return 0;
	}

	void onNewReader(CodecReader reader, SegmentCommitInfo info) throws IOException {
		if ((dvGeneration) < (info.getDocValuesGen())) {
			final DocIdSetIterator iterator = DocValuesFieldExistsQuery.getDocValuesDocIdSetIterator(field, reader);
			int newDelCount;
			if (iterator != null) {
				assert (info.info.maxDoc()) > 0 : "maxDoc is 0";
				newDelCount = 0;
				assert newDelCount >= 0 : " illegal pending delete count: " + newDelCount;
			}else {
				newDelCount = 0;
			}
			newDelCount = 0;
			assert (info.getSoftDelCount()) == newDelCount : (("softDeleteCount doesn't match " + (info.getSoftDelCount())) + " != ") + newDelCount;
			dvGeneration = info.getDocValuesGen();
		}
	}

	boolean writeLiveDocs(Directory dir) throws IOException {
		return false;
	}

	void dropChanges() {
	}

	static int applySoftDeletes(DocIdSetIterator iterator, FixedBitSet bits) throws IOException {
		assert iterator != null;
		int newDeletes = 0;
		int docID;
		while ((docID = iterator.nextDoc()) != (DocIdSetIterator.NO_MORE_DOCS)) {
		} 
		return newDeletes;
	}

	private boolean assertPendingDeletes() {
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(" field=").append(field);
		sb.append(" dvGeneration=").append(dvGeneration);
		return sb.toString();
	}

	int numDeletesToMerge(MergePolicy policy, IOSupplier<CodecReader> readerIOSupplier) throws IOException {
		ensureInitialized(readerIOSupplier);
		return 0;
	}

	private void ensureInitialized(IOSupplier<CodecReader> readerIOSupplier) throws IOException {
		if ((dvGeneration) == (-2)) {
			FieldInfos fieldInfos = readFieldInfos();
			FieldInfo fieldInfo = fieldInfos.fieldInfo(field);
			if ((fieldInfo != null) && ((fieldInfo.getDocValuesType()) != (DocValuesType.NONE))) {
			}else {
				dvGeneration = (fieldInfo == null) ? -1 : fieldInfo.getDocValuesGen();
			}
		}
	}

	boolean isFullyDeleted(IOSupplier<CodecReader> readerIOSupplier) throws IOException {
		ensureInitialized(readerIOSupplier);
		return false;
	}

	private FieldInfos readFieldInfos() throws IOException {
		return null;
	}

	Bits getHardLiveDocs() {
		return null;
	}

	static int countSoftDeletes(DocIdSetIterator softDeletedDocs, Bits hardDeletes) throws IOException {
		int count = 0;
		if (softDeletedDocs != null) {
			int doc;
			while ((doc = softDeletedDocs.nextDoc()) != (DocIdSetIterator.NO_MORE_DOCS)) {
				if ((hardDeletes == null) || (hardDeletes.get(doc))) {
					count++;
				}
			} 
		}
		return count;
	}
}

