

import java.io.IOException;
import java.util.Arrays;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.Counter;
import org.apache.lucene.util.packed.PackedInts;
import org.apache.lucene.util.packed.PackedLongValues;


class SortedNumericDocValuesWriter {
	private PackedLongValues.Builder pending;

	private PackedLongValues.Builder pendingCounts;

	private final Counter iwBytesUsed;

	private long bytesUsed;

	private final FieldInfo fieldInfo;

	private int currentDoc = -1;

	private long[] currentValues = new long[8];

	private int currentUpto = 0;

	private PackedLongValues finalValues;

	private PackedLongValues finalValuesCount;

	public SortedNumericDocValuesWriter(FieldInfo fieldInfo, Counter iwBytesUsed) {
		this.fieldInfo = fieldInfo;
		this.iwBytesUsed = iwBytesUsed;
		pending = PackedLongValues.deltaPackedBuilder(PackedInts.COMPACT);
		pendingCounts = PackedLongValues.deltaPackedBuilder(PackedInts.COMPACT);
		iwBytesUsed.addAndGet(bytesUsed);
	}

	public void addValue(int docID, long value) {
		assert docID >= (currentDoc);
		if (docID != (currentDoc)) {
			finishCurrentDoc();
			currentDoc = docID;
		}
		addOneValue(value);
		updateBytesUsed();
	}

	private void finishCurrentDoc() {
		if ((currentDoc) == (-1)) {
			return;
		}
		Arrays.sort(currentValues, 0, currentUpto);
		for (int i = 0; i < (currentUpto); i++) {
			pending.add(currentValues[i]);
		}
		pendingCounts.add(currentUpto);
		currentUpto = 0;
	}

	public void finish(int maxDoc) {
		finishCurrentDoc();
	}

	private void addOneValue(long value) {
		if ((currentUpto) == (currentValues.length)) {
			currentValues = ArrayUtil.grow(currentValues, ((currentValues.length) + 1));
		}
		currentValues[currentUpto] = value;
		(currentUpto)++;
	}

	private void updateBytesUsed() {
	}

	private static class BufferedSortedNumericDocValues extends SortedNumericDocValues {
		final PackedLongValues.Iterator valuesIter;

		final PackedLongValues.Iterator valueCountsIter;

		final DocIdSetIterator docsWithField;

		private int valueCount;

		private int valueUpto;

		public BufferedSortedNumericDocValues(PackedLongValues values, PackedLongValues valueCounts, DocIdSetIterator docsWithField) {
			valuesIter = values.iterator();
			valueCountsIter = valueCounts.iterator();
			this.docsWithField = docsWithField;
		}

		@Override
		public int docID() {
			return docsWithField.docID();
		}

		@Override
		public int nextDoc() throws IOException {
			for (int i = valueUpto; i < (valueCount); ++i) {
				valuesIter.next();
			}
			int docID = docsWithField.nextDoc();
			if (docID != (DocIdSetIterator.NO_MORE_DOCS)) {
				valueCount = Math.toIntExact(valueCountsIter.next());
				valueUpto = 0;
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
		public int docValueCount() {
			return valueCount;
		}

		@Override
		public long nextValue() {
			if ((valueUpto) == (valueCount)) {
				throw new IllegalStateException();
			}
			(valueUpto)++;
			return valuesIter.next();
		}

		@Override
		public long cost() {
			return docsWithField.cost();
		}
	}

	DocIdSetIterator getDocIdSet() {
		return null;
	}
}

