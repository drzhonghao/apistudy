

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.Spliterators;
import org.apache.cassandra.utils.btree.BTree;
import org.apache.cassandra.utils.btree.BTreeSearchIterator;
import org.apache.cassandra.utils.btree.UpdateFunction;

import static org.apache.cassandra.utils.btree.BTree.Dir.ASC;
import static org.apache.cassandra.utils.btree.BTree.Dir.DESC;


public class BTreeSet<V> implements List<V> , NavigableSet<V> {
	protected final Comparator<? super V> comparator;

	protected final Object[] tree;

	public BTreeSet(Object[] tree, Comparator<? super V> comparator) {
		this.tree = tree;
		this.comparator = comparator;
	}

	public BTreeSet<V> update(Collection<V> updateWith) {
		return new BTreeSet<>(BTree.update(tree, comparator, updateWith, UpdateFunction.<V>noOp()), comparator);
	}

	@Override
	public Comparator<? super V> comparator() {
		return comparator;
	}

	protected BTreeSearchIterator<V, V> slice(BTree.Dir dir) {
		return BTree.slice(tree, comparator, dir);
	}

	public Object[] tree() {
		return tree;
	}

	public int indexOf(Object item) {
		return BTree.findIndex(tree, comparator, ((V) (item)));
	}

	public V get(int index) {
		return BTree.<V>findByIndex(tree, index);
	}

	public int lastIndexOf(Object o) {
		return indexOf(o);
	}

	public BTreeSet<V> subList(int fromIndex, int toIndex) {
		return new BTreeSet.BTreeRange<V>(tree, comparator, fromIndex, (toIndex - 1));
	}

	@Override
	public int size() {
		return BTree.size(tree);
	}

	@Override
	public boolean isEmpty() {
		return BTree.isEmpty(tree);
	}

	@Override
	public BTreeSearchIterator<V, V> iterator() {
		return slice(ASC);
	}

	@Override
	public BTreeSearchIterator<V, V> descendingIterator() {
		return slice(DESC);
	}

	@Override
	public Object[] toArray() {
		return toArray(new Object[0]);
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return toArray(a, 0);
	}

	public <T> T[] toArray(T[] a, int offset) {
		int size = size();
		if ((a.length) < (size + offset))
			a = Arrays.copyOf(a, size);

		BTree.toArray(tree, a, offset);
		return a;
	}

	public Spliterator<V> spliterator() {
		return Spliterators.spliterator(this, (((((Spliterator.ORDERED) | (Spliterator.DISTINCT)) | (Spliterator.IMMUTABLE)) | (Spliterator.NONNULL)) | (Spliterator.SIZED)));
	}

	@Override
	public BTreeSet<V> subSet(V fromElement, boolean fromInclusive, V toElement, boolean toInclusive) {
		return new BTreeSet.BTreeRange<>(tree, comparator, fromElement, fromInclusive, toElement, toInclusive);
	}

	@Override
	public BTreeSet<V> headSet(V toElement, boolean inclusive) {
		return new BTreeSet.BTreeRange<>(tree, comparator, null, true, toElement, inclusive);
	}

	@Override
	public BTreeSet<V> tailSet(V fromElement, boolean inclusive) {
		return new BTreeSet.BTreeRange<>(tree, comparator, fromElement, inclusive, null, true);
	}

	@Override
	public SortedSet<V> subSet(V fromElement, V toElement) {
		return subSet(fromElement, true, toElement, false);
	}

	@Override
	public SortedSet<V> headSet(V toElement) {
		return headSet(toElement, false);
	}

	@Override
	public SortedSet<V> tailSet(V fromElement) {
		return tailSet(fromElement, true);
	}

	@Override
	public BTreeSet<V> descendingSet() {
		return new BTreeSet.BTreeRange<V>(this.tree, this.comparator).descendingSet();
	}

	@Override
	public V first() {
		return get(0);
	}

	@Override
	public V last() {
		return get(((size()) - 1));
	}

	@Override
	public V lower(V v) {
		return BTree.lower(tree, comparator, v);
	}

	@Override
	public V floor(V v) {
		return BTree.floor(tree, comparator, v);
	}

	@Override
	public V ceiling(V v) {
		return BTree.ceil(tree, comparator, v);
	}

	@Override
	public V higher(V v) {
		return BTree.higher(tree, comparator, v);
	}

	@Override
	public boolean contains(Object o) {
		return (indexOf(((V) (o)))) >= 0;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object o : c)
			if (!(contains(o)))
				return false;


		return true;
	}

	public int hashCode() {
		int result = 1;
		for (V v : this)
			result = (31 * result) + (Objects.hashCode(v));

		return result;
	}

	@Override
	public boolean addAll(Collection<? extends V> c) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(int index, Collection<? extends V> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public V pollFirst() {
		throw new UnsupportedOperationException();
	}

	@Override
	public V pollLast() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean add(V v) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	public V set(int index, V element) {
		throw new UnsupportedOperationException();
	}

	public void add(int index, V element) {
		throw new UnsupportedOperationException();
	}

	public V remove(int index) {
		throw new UnsupportedOperationException();
	}

	public ListIterator<V> listIterator() {
		throw new UnsupportedOperationException();
	}

	public ListIterator<V> listIterator(int index) {
		throw new UnsupportedOperationException();
	}

	public static class BTreeRange<V> extends BTreeSet<V> {
		protected final int lowerBound;

		protected final int upperBound;

		BTreeRange(Object[] tree, Comparator<? super V> comparator) {
			this(tree, comparator, null, true, null, true);
		}

		BTreeRange(BTreeSet.BTreeRange<V> from) {
			super(from.tree, from.comparator);
			this.lowerBound = from.lowerBound;
			this.upperBound = from.upperBound;
		}

		BTreeRange(Object[] tree, Comparator<? super V> comparator, int lowerBound, int upperBound) {
			super(tree, comparator);
			if (upperBound < (lowerBound - 1))
				upperBound = lowerBound - 1;

			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
		}

		BTreeRange(Object[] tree, Comparator<? super V> comparator, V lowerBound, boolean inclusiveLowerBound, V upperBound, boolean inclusiveUpperBound) {
			this(tree, comparator, (lowerBound == null ? 0 : inclusiveLowerBound ? BTree.ceilIndex(tree, comparator, lowerBound) : BTree.higherIndex(tree, comparator, lowerBound)), (upperBound == null ? (BTree.size(tree)) - 1 : inclusiveUpperBound ? BTree.floorIndex(tree, comparator, upperBound) : BTree.lowerIndex(tree, comparator, upperBound)));
		}

		BTreeRange(BTreeSet.BTreeRange<V> a, BTreeSet.BTreeRange<V> b) {
			this(a.tree, a.comparator, Math.max(a.lowerBound, b.lowerBound), Math.min(a.upperBound, b.upperBound));
			assert (a.tree) == (b.tree);
		}

		@Override
		protected BTreeSearchIterator<V, V> slice(BTree.Dir dir) {
			return null;
		}

		@Override
		public boolean isEmpty() {
			return (upperBound) < (lowerBound);
		}

		public int size() {
			return ((upperBound) - (lowerBound)) + 1;
		}

		boolean outOfBounds(int i) {
			return (i < (lowerBound)) | (i > (upperBound));
		}

		public V get(int index) {
			index += lowerBound;
			if (outOfBounds(index))
				throw new NoSuchElementException();

			return super.get(index);
		}

		public int indexOf(Object item) {
			int i = super.indexOf(item);
			boolean negate = i < 0;
			if (negate)
				i = (-1) - i;

			if (outOfBounds(i))
				return i < (lowerBound) ? -1 : (-1) - (size());

			i = i - (lowerBound);
			if (negate)
				i = (-1) - i;

			return i;
		}

		public V lower(V v) {
			return maybe(Math.min(upperBound, BTree.lowerIndex(tree, comparator, v)));
		}

		public V floor(V v) {
			return maybe(Math.min(upperBound, BTree.floorIndex(tree, comparator, v)));
		}

		public V ceiling(V v) {
			return maybe(Math.max(lowerBound, BTree.ceilIndex(tree, comparator, v)));
		}

		public V higher(V v) {
			return maybe(Math.max(lowerBound, BTree.higherIndex(tree, comparator, v)));
		}

		private V maybe(int i) {
			if (outOfBounds(i))
				return null;

			return super.get(i);
		}

		@Override
		public BTreeSet<V> subSet(V fromElement, boolean fromInclusive, V toElement, boolean toInclusive) {
			return new BTreeSet.BTreeRange<>(this, new BTreeSet.BTreeRange<>(tree, comparator, fromElement, fromInclusive, toElement, toInclusive));
		}

		@Override
		public BTreeSet<V> headSet(V toElement, boolean inclusive) {
			return new BTreeSet.BTreeRange<>(this, new BTreeSet.BTreeRange<>(tree, comparator, null, true, toElement, inclusive));
		}

		@Override
		public BTreeSet<V> tailSet(V fromElement, boolean inclusive) {
			return new BTreeSet.BTreeRange<>(this, new BTreeSet.BTreeRange<>(tree, comparator, fromElement, inclusive, null, true));
		}

		@Override
		public BTreeSet<V> descendingSet() {
			return new BTreeSet.BTreeDescRange<>(this);
		}

		public BTreeSet<V> subList(int fromIndex, int toIndex) {
			if ((fromIndex < 0) || (toIndex > (size())))
				throw new IndexOutOfBoundsException();

			return new BTreeSet.BTreeRange<V>(tree, comparator, ((lowerBound) + fromIndex), (((lowerBound) + toIndex) - 1));
		}

		@Override
		public <T> T[] toArray(T[] a) {
			return toArray(a, 0);
		}

		public <T> T[] toArray(T[] a, int offset) {
			if (((size()) + offset) < (a.length))
				a = Arrays.copyOf(a, ((size()) + offset));

			BTree.toArray(tree, lowerBound, ((upperBound) + 1), a, offset);
			return a;
		}
	}

	public static class BTreeDescRange<V> extends BTreeSet.BTreeRange<V> {
		BTreeDescRange(BTreeSet.BTreeRange<V> from) {
			super(from.tree, from.comparator, from.lowerBound, from.upperBound);
		}

		@Override
		protected BTreeSearchIterator<V, V> slice(BTree.Dir dir) {
			return super.slice(dir.invert());
		}

		public V higher(V v) {
			return super.lower(v);
		}

		public V ceiling(V v) {
			return super.floor(v);
		}

		public V floor(V v) {
			return super.ceiling(v);
		}

		public V lower(V v) {
			return super.higher(v);
		}

		public V get(int index) {
			index = (upperBound) - index;
			if (outOfBounds(index))
				throw new NoSuchElementException();

			return BTree.findByIndex(tree, index);
		}

		public int indexOf(Object item) {
			int i = super.indexOf(item);
			return i < 0 ? ((-2) - (size())) - i : (size()) - (i + 1);
		}

		public BTreeSet<V> subList(int fromIndex, int toIndex) {
			if ((fromIndex < 0) || (toIndex > (size())))
				throw new IndexOutOfBoundsException();

			return new BTreeSet.BTreeDescRange<V>(new BTreeSet.BTreeRange<V>(tree, comparator, ((upperBound) - (toIndex - 1)), ((upperBound) - fromIndex)));
		}

		@Override
		public BTreeSet<V> subSet(V fromElement, boolean fromInclusive, V toElement, boolean toInclusive) {
			return super.subSet(toElement, toInclusive, fromElement, fromInclusive).descendingSet();
		}

		@Override
		public BTreeSet<V> headSet(V toElement, boolean inclusive) {
			return super.tailSet(toElement, inclusive).descendingSet();
		}

		@Override
		public BTreeSet<V> tailSet(V fromElement, boolean inclusive) {
			return super.headSet(fromElement, inclusive).descendingSet();
		}

		@Override
		public BTreeSet<V> descendingSet() {
			return new BTreeSet.BTreeRange<>(this);
		}

		public Comparator<V> comparator() {
			return ( a, b) -> comparator.compare(b, a);
		}

		public <T> T[] toArray(T[] a, int offset) {
			a = super.toArray(a, offset);
			int count = size();
			int flip = count / 2;
			for (int i = 0; i < flip; i++) {
				int j = count - (i + 1);
				T t = a[(i + offset)];
				a[(i + offset)] = a[(j + offset)];
				a[(j + offset)] = t;
			}
			return a;
		}
	}

	public static class Builder<V> {
		final BTree.Builder<V> builder;

		protected Builder(Comparator<? super V> comparator) {
			builder = BTree.builder(comparator);
		}

		public BTreeSet.Builder<V> add(V v) {
			builder.add(v);
			return this;
		}

		public BTreeSet.Builder<V> addAll(Collection<V> iter) {
			builder.addAll(iter);
			return this;
		}

		public boolean isEmpty() {
			return builder.isEmpty();
		}

		public BTreeSet<V> build() {
			return null;
		}
	}

	public static <V> BTreeSet.Builder<V> builder(Comparator<? super V> comparator) {
		return new BTreeSet.Builder<>(comparator);
	}

	public static <V> BTreeSet<V> wrap(Object[] btree, Comparator<V> comparator) {
		return new BTreeSet<>(btree, comparator);
	}

	public static <V extends Comparable<V>> BTreeSet<V> of(Collection<V> sortedValues) {
		return new BTreeSet<>(BTree.build(sortedValues, UpdateFunction.<V>noOp()), Ordering.<V>natural());
	}

	public static <V extends Comparable<V>> BTreeSet<V> of(V value) {
		return new BTreeSet<>(BTree.build(ImmutableList.of(value), UpdateFunction.<V>noOp()), Ordering.<V>natural());
	}

	public static <V> BTreeSet<V> empty(Comparator<? super V> comparator) {
		return new BTreeSet<>(BTree.empty(), comparator);
	}

	public static <V> BTreeSet<V> of(Comparator<? super V> comparator, V value) {
		return new BTreeSet<>(BTree.singleton(value), comparator);
	}
}

