

import java.io.IOException;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.Counter;
import org.apache.lucene.util.packed.PackedInts;
import org.apache.lucene.util.packed.PackedLongValues;


class NormValuesWriter {
	private PackedLongValues.Builder pending;

	private final Counter iwBytesUsed;

	private long bytesUsed;

	private final FieldInfo fieldInfo;

	private int lastDocID = -1;

	public NormValuesWriter(FieldInfo fieldInfo, Counter iwBytesUsed) {
		pending = PackedLongValues.deltaPackedBuilder(PackedInts.COMPACT);
		this.fieldInfo = fieldInfo;
		this.iwBytesUsed = iwBytesUsed;
		iwBytesUsed.addAndGet(bytesUsed);
	}

	public void addValue(int docID, long value) {
		if (docID <= (lastDocID)) {
			throw new IllegalArgumentException((("Norm for \"" + (fieldInfo.name)) + "\" appears more than once in this document (only one value is allowed per field)"));
		}
		pending.add(value);
		updateBytesUsed();
		lastDocID = docID;
	}

	private void updateBytesUsed() {
		final long newBytesUsed = pending.ramBytesUsed();
		iwBytesUsed.addAndGet((newBytesUsed - (bytesUsed)));
		bytesUsed = newBytesUsed;
	}

	public void finish(int maxDoc) {
	}

	private static class BufferedNorms extends NumericDocValues {
		final PackedLongValues.Iterator iter;

		final DocIdSetIterator docsWithField;

		private long value;

		BufferedNorms(PackedLongValues values, DocIdSetIterator docsWithFields) {
			this.iter = values.iterator();
			this.docsWithField = docsWithFields;
		}

		@Override
		public int docID() {
			return docsWithField.docID();
		}

		@Override
		public int nextDoc() throws IOException {
			int docID = docsWithField.nextDoc();
			if (docID != (DocIdSetIterator.NO_MORE_DOCS)) {
				value = iter.next();
			}
			return docID;
		}

		@Override
		public int advance(int target) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean advanceExact(int target) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public long cost() {
			return docsWithField.cost();
		}

		@Override
		public long longValue() {
			return value;
		}
	}
}

