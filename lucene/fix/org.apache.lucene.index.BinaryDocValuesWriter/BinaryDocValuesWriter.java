

import java.io.IOException;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.Counter;
import org.apache.lucene.util.PagedBytes;
import org.apache.lucene.util.packed.PackedInts;
import org.apache.lucene.util.packed.PackedLongValues;


class BinaryDocValuesWriter {
	private static final int MAX_LENGTH = ArrayUtil.MAX_ARRAY_LENGTH;

	private static final int BLOCK_BITS = 15;

	private final PagedBytes bytes;

	private final DataOutput bytesOut;

	private final Counter iwBytesUsed;

	private final PackedLongValues.Builder lengths;

	private final FieldInfo fieldInfo;

	private long bytesUsed;

	private int lastDocID = -1;

	private int maxLength = 0;

	public BinaryDocValuesWriter(FieldInfo fieldInfo, Counter iwBytesUsed) {
		this.fieldInfo = fieldInfo;
		this.bytes = new PagedBytes(BinaryDocValuesWriter.BLOCK_BITS);
		this.bytesOut = bytes.getDataOutput();
		this.lengths = PackedLongValues.deltaPackedBuilder(PackedInts.COMPACT);
		this.iwBytesUsed = iwBytesUsed;
		iwBytesUsed.addAndGet(bytesUsed);
	}

	public void addValue(int docID, BytesRef value) {
		if (docID <= (lastDocID)) {
			throw new IllegalArgumentException((("DocValuesField \"" + (fieldInfo.name)) + "\" appears more than once in this document (only one value is allowed per field)"));
		}
		if (value == null) {
			throw new IllegalArgumentException((("field=\"" + (fieldInfo.name)) + "\": null value not allowed"));
		}
		if ((value.length) > (BinaryDocValuesWriter.MAX_LENGTH)) {
			throw new IllegalArgumentException(((("DocValuesField \"" + (fieldInfo.name)) + "\" is too large, must be <= ") + (BinaryDocValuesWriter.MAX_LENGTH)));
		}
		maxLength = Math.max(value.length, maxLength);
		lengths.add(value.length);
		try {
			bytesOut.writeBytes(value.bytes, value.offset, value.length);
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
		updateBytesUsed();
		lastDocID = docID;
	}

	private void updateBytesUsed() {
	}

	public void finish(int maxDoc) {
	}

	private static class BufferedBinaryDocValues extends BinaryDocValues {
		final BytesRefBuilder value;

		final PackedLongValues.Iterator lengthsIterator;

		final DocIdSetIterator docsWithField;

		final DataInput bytesIterator;

		BufferedBinaryDocValues(PackedLongValues lengths, int maxLength, DataInput bytesIterator, DocIdSetIterator docsWithFields) {
			this.value = new BytesRefBuilder();
			this.value.grow(maxLength);
			this.lengthsIterator = lengths.iterator();
			this.bytesIterator = bytesIterator;
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
				int length = Math.toIntExact(lengthsIterator.next());
				value.setLength(length);
				bytesIterator.readBytes(value.bytes(), 0, length);
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
		public BytesRef binaryValue() {
			return value.get();
		}
	}

	DocIdSetIterator getDocIdSet() {
		return null;
	}
}

