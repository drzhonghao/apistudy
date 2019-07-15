

import java.io.IOException;
import java.util.Arrays;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.ByteBlockPool;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefHash;
import org.apache.lucene.util.Counter;
import org.apache.lucene.util.packed.PackedInts;
import org.apache.lucene.util.packed.PackedLongValues;


class SortedSetDocValuesWriter {
	final BytesRefHash hash;

	private PackedLongValues.Builder pending;

	private PackedLongValues.Builder pendingCounts;

	private final Counter iwBytesUsed;

	private long bytesUsed;

	private final FieldInfo fieldInfo;

	private int currentDoc = -1;

	private int[] currentValues = new int[8];

	private int currentUpto;

	private int maxCount;

	private PackedLongValues finalOrds;

	private PackedLongValues finalOrdCounts;

	private int[] finalSortedValues;

	private int[] finalOrdMap;

	public SortedSetDocValuesWriter(FieldInfo fieldInfo, Counter iwBytesUsed) {
		this.fieldInfo = fieldInfo;
		this.iwBytesUsed = iwBytesUsed;
		hash = new BytesRefHash(new ByteBlockPool(new ByteBlockPool.DirectTrackingAllocator(iwBytesUsed)), BytesRefHash.DEFAULT_CAPACITY, new BytesRefHash.DirectBytesStartArray(BytesRefHash.DEFAULT_CAPACITY, iwBytesUsed));
		pending = PackedLongValues.packedBuilder(PackedInts.COMPACT);
		pendingCounts = PackedLongValues.deltaPackedBuilder(PackedInts.COMPACT);
		bytesUsed = (pending.ramBytesUsed()) + (pendingCounts.ramBytesUsed());
		iwBytesUsed.addAndGet(bytesUsed);
	}

	public void addValue(int docID, BytesRef value) {
		assert docID >= (currentDoc);
		if (value == null) {
			throw new IllegalArgumentException((("field \"" + (fieldInfo.name)) + "\": null value not allowed"));
		}
		if ((value.length) > ((ByteBlockPool.BYTE_BLOCK_SIZE) - 2)) {
			throw new IllegalArgumentException(((("DocValuesField \"" + (fieldInfo.name)) + "\" is too large, must be <= ") + ((ByteBlockPool.BYTE_BLOCK_SIZE) - 2)));
		}
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
		int lastValue = -1;
		int count = 0;
		for (int i = 0; i < (currentUpto); i++) {
			int termID = currentValues[i];
			if (termID != lastValue) {
				pending.add(termID);
				count++;
			}
			lastValue = termID;
		}
		pendingCounts.add(count);
		maxCount = Math.max(maxCount, count);
		currentUpto = 0;
	}

	public void finish(int maxDoc) {
		finishCurrentDoc();
	}

	private void addOneValue(BytesRef value) {
		int termID = hash.add(value);
		if (termID < 0) {
			termID = (-termID) - 1;
		}else {
			iwBytesUsed.addAndGet((2 * (Integer.BYTES)));
		}
		if ((currentUpto) == (currentValues.length)) {
			currentValues = ArrayUtil.grow(currentValues, ((currentValues.length) + 1));
			iwBytesUsed.addAndGet((((currentValues.length) - (currentUpto)) * (Integer.BYTES)));
		}
		currentValues[currentUpto] = termID;
		(currentUpto)++;
	}

	private void updateBytesUsed() {
		final long newBytesUsed = (pending.ramBytesUsed()) + (pendingCounts.ramBytesUsed());
		iwBytesUsed.addAndGet((newBytesUsed - (bytesUsed)));
		bytesUsed = newBytesUsed;
	}

	private static class BufferedSortedSetDocValues extends SortedSetDocValues {
		final int[] sortedValues;

		final int[] ordMap;

		final BytesRefHash hash;

		final BytesRef scratch = new BytesRef();

		final PackedLongValues.Iterator ordsIter;

		final PackedLongValues.Iterator ordCountsIter;

		final DocIdSetIterator docsWithField;

		final int[] currentDoc;

		private int ordCount;

		private int ordUpto;

		public BufferedSortedSetDocValues(int[] sortedValues, int[] ordMap, BytesRefHash hash, PackedLongValues ords, PackedLongValues ordCounts, int maxCount, DocIdSetIterator docsWithField) {
			this.currentDoc = new int[maxCount];
			this.sortedValues = sortedValues;
			this.ordMap = ordMap;
			this.hash = hash;
			this.ordsIter = ords.iterator();
			this.ordCountsIter = ordCounts.iterator();
			this.docsWithField = docsWithField;
		}

		@Override
		public int docID() {
			return docsWithField.docID();
		}

		@Override
		public int nextDoc() throws IOException {
			int docID = docsWithField.nextDoc();
			if (docID != (DocIdSetIterator.NO_MORE_DOCS)) {
				ordCount = ((int) (ordCountsIter.next()));
				assert (ordCount) > 0;
				for (int i = 0; i < (ordCount); i++) {
					currentDoc[i] = ordMap[Math.toIntExact(ordsIter.next())];
				}
				Arrays.sort(currentDoc, 0, ordCount);
				ordUpto = 0;
			}
			return docID;
		}

		@Override
		public long nextOrd() {
			if ((ordUpto) == (ordCount)) {
				return SortedSetDocValues.NO_MORE_ORDS;
			}else {
				return currentDoc[((ordUpto)++)];
			}
		}

		@Override
		public long cost() {
			return docsWithField.cost();
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
		public long getValueCount() {
			return ordMap.length;
		}

		@Override
		public BytesRef lookupOrd(long ord) {
			assert (ord >= 0) && (ord < (ordMap.length)) : (("ord=" + ord) + " is out of bounds 0 .. ") + ((ordMap.length) - 1);
			hash.get(sortedValues[Math.toIntExact(ord)], scratch);
			return scratch;
		}
	}

	DocIdSetIterator getDocIdSet() {
		return null;
	}
}

