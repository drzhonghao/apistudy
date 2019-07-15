

import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.InPlaceMergeSorter;
import org.apache.lucene.util.PriorityQueue;
import org.apache.lucene.util.RamUsageEstimator;
import org.apache.lucene.util.packed.PackedInts;
import org.apache.lucene.util.packed.PagedMutable;


abstract class DocValuesFieldUpdates implements Accountable {
	protected static final int PAGE_SIZE = 1024;

	private static final long HAS_VALUE_MASK = 1;

	private static final long HAS_NO_VALUE_MASK = 0;

	private static final int SHIFT = 1;

	static abstract class Iterator {
		public final boolean advanceExact(int target) {
			throw new UnsupportedOperationException();
		}

		public final int advance(int target) {
			throw new UnsupportedOperationException();
		}

		public final long cost() {
			throw new UnsupportedOperationException();
		}

		public abstract int nextDoc();

		abstract long longValue();

		abstract BytesRef binaryValue();

		abstract long delGen();

		abstract boolean hasValue();

		static BinaryDocValues asBinaryDocValues(DocValuesFieldUpdates.Iterator iterator) {
			return new BinaryDocValues() {
				@Override
				public int docID() {
					return 0;
				}

				@Override
				public BytesRef binaryValue() {
					return iterator.binaryValue();
				}

				@Override
				public boolean advanceExact(int target) {
					return iterator.advanceExact(target);
				}

				@Override
				public int nextDoc() {
					return iterator.nextDoc();
				}

				@Override
				public int advance(int target) {
					return iterator.advance(target);
				}

				@Override
				public long cost() {
					return iterator.cost();
				}
			};
		}

		static NumericDocValues asNumericDocValues(DocValuesFieldUpdates.Iterator iterator) {
			return new NumericDocValues() {
				@Override
				public long longValue() {
					return iterator.longValue();
				}

				@Override
				public boolean advanceExact(int target) {
					throw new UnsupportedOperationException();
				}

				@Override
				public int docID() {
					return 0;
				}

				@Override
				public int nextDoc() {
					return iterator.nextDoc();
				}

				@Override
				public int advance(int target) {
					return iterator.advance(target);
				}

				@Override
				public long cost() {
					return iterator.cost();
				}
			};
		}
	}

	public static DocValuesFieldUpdates.Iterator mergedIterator(DocValuesFieldUpdates.Iterator[] subs) {
		if ((subs.length) == 1) {
			return subs[0];
		}
		PriorityQueue<DocValuesFieldUpdates.Iterator> queue = new PriorityQueue<DocValuesFieldUpdates.Iterator>(subs.length) {
			@Override
			protected boolean lessThan(DocValuesFieldUpdates.Iterator a, DocValuesFieldUpdates.Iterator b) {
				return false;
			}
		};
		for (DocValuesFieldUpdates.Iterator sub : subs) {
			if ((sub.nextDoc()) != (DocIdSetIterator.NO_MORE_DOCS)) {
				queue.add(sub);
			}
		}
		if ((queue.size()) == 0) {
			return null;
		}
		return new DocValuesFieldUpdates.Iterator() {
			private int doc = -1;

			@Override
			public int nextDoc() {
				while (true) {
					if ((queue.size()) == 0) {
						doc = DocIdSetIterator.NO_MORE_DOCS;
						break;
					}
					if ((queue.top().nextDoc()) == (DocIdSetIterator.NO_MORE_DOCS)) {
						queue.pop();
					}else {
						queue.updateTop();
					}
				} 
				return doc;
			}

			public int docID() {
				return doc;
			}

			@Override
			long longValue() {
				return queue.top().longValue();
			}

			@Override
			BytesRef binaryValue() {
				return queue.top().binaryValue();
			}

			@Override
			public long delGen() {
				throw new UnsupportedOperationException();
			}

			@Override
			boolean hasValue() {
				return queue.top().hasValue();
			}
		};
	}

	final String field;

	final DocValuesType type;

	final long delGen;

	private final int bitsPerValue;

	private boolean finished;

	protected final int maxDoc;

	protected PagedMutable docs;

	protected int size;

	protected DocValuesFieldUpdates(int maxDoc, long delGen, String field, DocValuesType type) {
		this.maxDoc = maxDoc;
		this.delGen = delGen;
		this.field = field;
		if (type == null) {
			throw new NullPointerException("DocValuesType must not be null");
		}
		this.type = type;
		bitsPerValue = (PackedInts.bitsRequired((maxDoc - 1))) + (DocValuesFieldUpdates.SHIFT);
		docs = new PagedMutable(1, DocValuesFieldUpdates.PAGE_SIZE, bitsPerValue, PackedInts.COMPACT);
	}

	final boolean getFinished() {
		return finished;
	}

	abstract void add(int doc, long value);

	abstract void add(int doc, BytesRef value);

	abstract void add(int docId, DocValuesFieldUpdates.Iterator iterator);

	abstract DocValuesFieldUpdates.Iterator iterator();

	synchronized final void finish() {
		if (finished) {
			throw new IllegalStateException("already finished");
		}
		finished = true;
		if ((size) < (docs.size())) {
			resize(size);
		}
		new InPlaceMergeSorter() {
			@Override
			protected void swap(int i, int j) {
				DocValuesFieldUpdates.this.swap(i, j);
			}

			@Override
			protected int compare(int i, int j) {
				return Long.compare(docs.get(i), docs.get(j));
			}
		}.sort(0, size);
	}

	synchronized final boolean any() {
		return (size) > 0;
	}

	synchronized final int size() {
		return size;
	}

	synchronized final void reset(int doc) {
		addInternal(doc, DocValuesFieldUpdates.HAS_NO_VALUE_MASK);
	}

	synchronized final int add(int doc) {
		return addInternal(doc, DocValuesFieldUpdates.HAS_VALUE_MASK);
	}

	private synchronized int addInternal(int doc, long hasValueMask) {
		if (finished) {
			throw new IllegalStateException("already finished");
		}
		assert doc < (maxDoc);
		if ((size) == (Integer.MAX_VALUE)) {
			throw new IllegalStateException("cannot support more than Integer.MAX_VALUE doc/value entries");
		}
		if ((docs.size()) == (size)) {
			grow(((size) + 1));
		}
		docs.set(size, ((((long) (doc)) << (DocValuesFieldUpdates.SHIFT)) | hasValueMask));
		++(size);
		return (size) - 1;
	}

	protected void swap(int i, int j) {
		long tmpDoc = docs.get(j);
		docs.set(j, docs.get(i));
		docs.set(i, tmpDoc);
	}

	protected void grow(int size) {
		docs = docs.grow(size);
	}

	protected void resize(int size) {
		docs = docs.resize(size);
	}

	protected final void ensureFinished() {
		if ((finished) == false) {
			throw new IllegalStateException("call finish first");
		}
	}

	@Override
	public long ramBytesUsed() {
		return (((((docs.ramBytesUsed()) + (RamUsageEstimator.NUM_BYTES_OBJECT_HEADER)) + (2 * (Integer.BYTES))) + 2) + (Long.BYTES)) + (RamUsageEstimator.NUM_BYTES_OBJECT_REF);
	}

	protected static abstract class AbstractIterator extends DocValuesFieldUpdates.Iterator {
		private final int size;

		private final PagedMutable docs;

		private long idx = 0;

		private int doc = -1;

		private final long delGen;

		private boolean hasValue;

		AbstractIterator(int size, PagedMutable docs, long delGen) {
			this.size = size;
			this.docs = docs;
			this.delGen = delGen;
		}

		@Override
		public final int nextDoc() {
			if ((idx) >= (size)) {
				return doc = DocIdSetIterator.NO_MORE_DOCS;
			}
			long longDoc = docs.get(idx);
			++(idx);
			while (((idx) < (size)) && ((docs.get(idx)) == longDoc)) {
				++(idx);
			} 
			hasValue = (longDoc & (DocValuesFieldUpdates.HAS_VALUE_MASK)) > 0;
			if (hasValue) {
				set(((idx) - 1));
			}
			doc = ((int) (longDoc >> (DocValuesFieldUpdates.SHIFT)));
			return doc;
		}

		protected abstract void set(long idx);

		public final int docID() {
			return doc;
		}

		@Override
		final long delGen() {
			return delGen;
		}

		@Override
		final boolean hasValue() {
			return hasValue;
		}
	}
}

