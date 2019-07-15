

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.cassandra.utils.IndexedSearchIterator;
import org.apache.cassandra.utils.btree.BTree;

import static org.apache.cassandra.utils.btree.BTree.Dir.ASC;


public class BTreeSearchIterator<K, V> implements Iterator<V> , IndexedSearchIterator<K, V> {
	private final boolean forwards;

	private int index;

	private byte state;

	private final int lowerBound;

	private final int upperBound;

	private static final int MIDDLE = 0;

	private static final int ON_ITEM = 1;

	private static final int BEFORE_FIRST = 2;

	private static final int LAST = 4;

	private static final int END = 5;

	public BTreeSearchIterator(Object[] btree, Comparator<? super K> comparator, BTree.Dir dir) {
		this(btree, comparator, dir, 0, ((BTree.size(btree)) - 1));
	}

	BTreeSearchIterator(Object[] btree, Comparator<? super K> comparator, BTree.Dir dir, int lowerBound, int upperBound) {
		this.forwards = dir == (ASC);
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		rewind();
	}

	private int compareToLast(int idx) {
		return forwards ? idx - (upperBound) : (lowerBound) - idx;
	}

	private int compareToFirst(int idx) {
		return forwards ? idx - (lowerBound) : (upperBound) - idx;
	}

	public boolean hasNext() {
		return (state) != (BTreeSearchIterator.END);
	}

	public V next() {
		return current();
	}

	public V next(K target) {
		if (!(hasNext()))
			return null;

		int state = this.state;
		V next = null;
		if ((state == (BTreeSearchIterator.BEFORE_FIRST)) && ((compareToFirst(index)) < 0))
			return null;

		int compareToLast = compareToLast(index);
		if (compareToLast <= 0) {
			state = (compareToLast < 0) ? BTreeSearchIterator.MIDDLE : BTreeSearchIterator.LAST;
		}else
			state = BTreeSearchIterator.END;

		this.state = ((byte) (state));
		this.index = index;
		return next;
	}

	public void rewind() {
		if ((upperBound) < (lowerBound)) {
			state = ((byte) (BTreeSearchIterator.END));
		}else {
			state = ((byte) (BTreeSearchIterator.BEFORE_FIRST));
		}
	}

	private void checkOnItem() {
		if (((state) & (BTreeSearchIterator.ON_ITEM)) != (BTreeSearchIterator.ON_ITEM))
			throw new NoSuchElementException();

	}

	public V current() {
		checkOnItem();
		return null;
	}

	public int indexOfCurrent() {
		checkOnItem();
		return compareToFirst(index);
	}
}

