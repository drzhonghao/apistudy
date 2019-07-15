

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.packed.PagedGrowableWriter;
import org.apache.lucene.util.packed.PagedMutable;


final class BinaryDocValuesFieldUpdates {
	static final class Iterator {
		private final PagedGrowableWriter offsets;

		private final PagedGrowableWriter lengths;

		private final BytesRef value;

		private int offset;

		private int length;

		Iterator(int size, PagedGrowableWriter offsets, PagedGrowableWriter lengths, PagedMutable docs, BytesRef values, long delGen) {
			this.offsets = offsets;
			this.lengths = lengths;
			value = values.clone();
		}

		BytesRef binaryValue() {
			value.offset = offset;
			value.length = length;
			return value;
		}

		protected void set(long idx) {
			offset = ((int) (offsets.get(idx)));
			length = ((int) (lengths.get(idx)));
		}

		long longValue() {
			throw new UnsupportedOperationException();
		}
	}

	private PagedGrowableWriter offsets;

	private PagedGrowableWriter lengths;

	private BytesRefBuilder values;

	public BinaryDocValuesFieldUpdates(long delGen, String field, int maxDoc) {
		values = new BytesRefBuilder();
	}

	public void add(int doc, long value) {
		throw new UnsupportedOperationException();
	}

	public synchronized void add(int doc, BytesRef value) {
		values.append(value);
	}

	protected void swap(int i, int j) {
		long tmpOffset = offsets.get(j);
		offsets.set(j, offsets.get(i));
		offsets.set(i, tmpOffset);
		long tmpLength = lengths.get(j);
		lengths.set(j, lengths.get(i));
		lengths.set(i, tmpLength);
	}

	protected void grow(int size) {
		offsets = offsets.grow(size);
		lengths = lengths.grow(size);
	}

	protected void resize(int size) {
		offsets = offsets.resize(size);
		lengths = lengths.resize(size);
	}

	public BinaryDocValuesFieldUpdates.Iterator iterator() {
		return null;
	}

	public long ramBytesUsed() {
		return 0L;
	}
}

