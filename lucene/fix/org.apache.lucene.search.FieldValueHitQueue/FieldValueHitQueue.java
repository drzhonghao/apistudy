

import java.io.IOException;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.LeafFieldComparator;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.PriorityQueue;


public abstract class FieldValueHitQueue<T extends FieldValueHitQueue.Entry> extends PriorityQueue<T> {
	public static class Entry extends ScoreDoc {
		public int slot;

		public Entry(int slot, int doc, float score) {
			super(doc, score);
			this.slot = slot;
		}

		@Override
		public String toString() {
			return (("slot:" + (slot)) + " ") + (super.toString());
		}
	}

	private static final class OneComparatorFieldValueHitQueue<T extends FieldValueHitQueue.Entry> extends FieldValueHitQueue<T> {
		private final int oneReverseMul;

		private final FieldComparator<?> oneComparator;

		public OneComparatorFieldValueHitQueue(SortField[] fields, int size) {
			super(fields, size);
			assert (fields.length) == 1;
			oneComparator = comparators[0];
			oneReverseMul = reverseMul[0];
		}

		@Override
		protected boolean lessThan(final FieldValueHitQueue.Entry hitA, final FieldValueHitQueue.Entry hitB) {
			assert hitA != hitB;
			assert (hitA.slot) != (hitB.slot);
			final int c = (oneReverseMul) * (oneComparator.compare(hitA.slot, hitB.slot));
			if (c != 0) {
				return c > 0;
			}
			return (hitA.doc) > (hitB.doc);
		}
	}

	private static final class MultiComparatorsFieldValueHitQueue<T extends FieldValueHitQueue.Entry> extends FieldValueHitQueue<T> {
		public MultiComparatorsFieldValueHitQueue(SortField[] fields, int size) {
			super(fields, size);
		}

		@Override
		protected boolean lessThan(final FieldValueHitQueue.Entry hitA, final FieldValueHitQueue.Entry hitB) {
			assert hitA != hitB;
			assert (hitA.slot) != (hitB.slot);
			int numComparators = comparators.length;
			for (int i = 0; i < numComparators; ++i) {
				final int c = (reverseMul[i]) * (comparators[i].compare(hitA.slot, hitB.slot));
				if (c != 0) {
					return c > 0;
				}
			}
			return (hitA.doc) > (hitB.doc);
		}
	}

	private FieldValueHitQueue(SortField[] fields, int size) {
		super(size);
		this.fields = fields;
		int numComparators = fields.length;
		comparators = new FieldComparator<?>[numComparators];
		reverseMul = new int[numComparators];
		for (int i = 0; i < numComparators; ++i) {
			SortField field = fields[i];
			comparators[i] = field.getComparator(size, i);
		}
	}

	public static <T extends FieldValueHitQueue.Entry> FieldValueHitQueue<T> create(SortField[] fields, int size) {
		if ((fields.length) == 0) {
			throw new IllegalArgumentException("Sort must contain at least one field");
		}
		if ((fields.length) == 1) {
			return new FieldValueHitQueue.OneComparatorFieldValueHitQueue<>(fields, size);
		}else {
			return new FieldValueHitQueue.MultiComparatorsFieldValueHitQueue<>(fields, size);
		}
	}

	public FieldComparator<?>[] getComparators() {
		return comparators;
	}

	public int[] getReverseMul() {
		return reverseMul;
	}

	public LeafFieldComparator[] getComparators(LeafReaderContext context) throws IOException {
		LeafFieldComparator[] comparators = new LeafFieldComparator[this.comparators.length];
		for (int i = 0; i < (comparators.length); ++i) {
			comparators[i] = this.comparators[i].getLeafComparator(context);
		}
		return comparators;
	}

	protected final SortField[] fields;

	protected final FieldComparator<?>[] comparators;

	protected final int[] reverseMul;

	@Override
	protected abstract boolean lessThan(final FieldValueHitQueue.Entry a, final FieldValueHitQueue.Entry b);

	FieldDoc fillFields(final FieldValueHitQueue.Entry entry) {
		final int n = comparators.length;
		final Object[] fields = new Object[n];
		for (int i = 0; i < n; ++i) {
			fields[i] = comparators[i].value(entry.slot);
		}
		return new FieldDoc(entry.doc, entry.score, fields);
	}

	SortField[] getFields() {
		return fields;
	}
}

