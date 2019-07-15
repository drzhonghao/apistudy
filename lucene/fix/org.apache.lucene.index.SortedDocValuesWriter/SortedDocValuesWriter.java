

import java.io.IOException;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.ByteBlockPool;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefHash;
import org.apache.lucene.util.Counter;
import org.apache.lucene.util.packed.PackedInts;
import org.apache.lucene.util.packed.PackedLongValues;


class SortedDocValuesWriter {
	final BytesRefHash hash;

	private PackedLongValues.Builder pending;

	private final Counter iwBytesUsed;

	private long bytesUsed;

	private final FieldInfo fieldInfo;

	private int lastDocID = -1;

	private PackedLongValues finalOrds;

	private int[] finalSortedValues;

	private int[] finalOrdMap;

	public SortedDocValuesWriter(FieldInfo fieldInfo, Counter iwBytesUsed) {
		this.fieldInfo = fieldInfo;
		this.iwBytesUsed = iwBytesUsed;
		hash = new BytesRefHash(new ByteBlockPool(new ByteBlockPool.DirectTrackingAllocator(iwBytesUsed)), BytesRefHash.DEFAULT_CAPACITY, new BytesRefHash.DirectBytesStartArray(BytesRefHash.DEFAULT_CAPACITY, iwBytesUsed));
		pending = PackedLongValues.deltaPackedBuilder(PackedInts.COMPACT);
		iwBytesUsed.addAndGet(bytesUsed);
	}

	public void addValue(int docID, BytesRef value) {
		if (docID <= (lastDocID)) {
			throw new IllegalArgumentException((("DocValuesField \"" + (fieldInfo.name)) + "\" appears more than once in this document (only one value is allowed per field)"));
		}
		if (value == null) {
			throw new IllegalArgumentException((("field \"" + (fieldInfo.name)) + "\": null value not allowed"));
		}
		if ((value.length) > ((ByteBlockPool.BYTE_BLOCK_SIZE) - 2)) {
			throw new IllegalArgumentException(((("DocValuesField \"" + (fieldInfo.name)) + "\" is too large, must be <= ") + ((ByteBlockPool.BYTE_BLOCK_SIZE) - 2)));
		}
		addOneValue(value);
		lastDocID = docID;
	}

	public void finish(int maxDoc) {
		updateBytesUsed();
	}

	private void addOneValue(BytesRef value) {
		int termID = hash.add(value);
		if (termID < 0) {
			termID = (-termID) - 1;
		}else {
			iwBytesUsed.addAndGet((2 * (Integer.BYTES)));
		}
		pending.add(termID);
		updateBytesUsed();
	}

	private void updateBytesUsed() {
	}

	private static class BufferedSortedDocValues extends SortedDocValues {
		final BytesRefHash hash;

		final BytesRef scratch = new BytesRef();

		final int[] sortedValues;

		final int[] ordMap;

		final int valueCount;

		private int ord;

		final PackedLongValues.Iterator iter;

		final DocIdSetIterator docsWithField;

		public BufferedSortedDocValues(BytesRefHash hash, int valueCount, PackedLongValues docToOrd, int[] sortedValues, int[] ordMap, DocIdSetIterator docsWithField) {
			this.hash = hash;
			this.valueCount = valueCount;
			this.sortedValues = sortedValues;
			this.iter = docToOrd.iterator();
			this.ordMap = ordMap;
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
				ord = Math.toIntExact(iter.next());
				ord = ordMap[ord];
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
		public int ordValue() {
			return ord;
		}

		@Override
		public BytesRef lookupOrd(int ord) {
			assert (ord >= 0) && (ord < (sortedValues.length));
			assert ((sortedValues[ord]) >= 0) && ((sortedValues[ord]) < (sortedValues.length));
			hash.get(sortedValues[ord], scratch);
			return scratch;
		}

		@Override
		public int getValueCount() {
			return valueCount;
		}
	}

	DocIdSetIterator getDocIdSet() {
		return null;
	}
}

