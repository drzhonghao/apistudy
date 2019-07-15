

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Ordering;
import io.netty.util.Recycler;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SortedSet;
import java.util.function.Consumer;
import org.apache.cassandra.utils.ObjectSizes;
import org.apache.cassandra.utils.btree.BTreeSearchIterator;
import org.apache.cassandra.utils.btree.BTreeSet;
import org.apache.cassandra.utils.btree.UpdateFunction;


public class BTree {
	static final int FAN_SHIFT;

	static {
		int fanfactor = 32;
		if ((System.getProperty("cassandra.btree.fanfactor")) != null)
			fanfactor = Integer.parseInt(System.getProperty("cassandra.btree.fanfactor"));

		int shift = 1;
		while ((1 << shift) < fanfactor)
			shift += 1;

		FAN_SHIFT = shift;
	}

	static final int FAN_FACTOR = 1 << (BTree.FAN_SHIFT);

	static final int MINIMAL_NODE_SIZE = (BTree.FAN_FACTOR) >> 1;

	static final Object[] EMPTY_LEAF = new Object[1];

	static final Object[] EMPTY_BRANCH = new Object[]{ null, new int[0] };

	public static enum Dir {

		ASC,
		DESC;
		public BTree.Dir invert() {
			return (this) == (BTree.Dir.ASC) ? BTree.Dir.DESC : BTree.Dir.ASC;
		}

		public static BTree.Dir asc(boolean asc) {
			return asc ? BTree.Dir.ASC : BTree.Dir.DESC;
		}

		public static BTree.Dir desc(boolean desc) {
			return desc ? BTree.Dir.DESC : BTree.Dir.ASC;
		}
	}

	public static Object[] empty() {
		return BTree.EMPTY_LEAF;
	}

	public static Object[] singleton(Object value) {
		return new Object[]{ value };
	}

	public static <C, K extends C, V extends C> Object[] build(Collection<K> source, UpdateFunction<K, V> updateF) {
		return BTree.buildInternal(source, source.size(), updateF);
	}

	public static <C, K extends C, V extends C> Object[] build(Iterable<K> source, UpdateFunction<K, V> updateF) {
		return BTree.buildInternal(source, (-1), updateF);
	}

	public static <C, K extends C, V extends C> Object[] build(Iterable<K> source, int size, UpdateFunction<K, V> updateF) {
		if (size < 0)
			throw new IllegalArgumentException(Integer.toString(size));

		return BTree.buildInternal(source, size, updateF);
	}

	private static <C, K extends C, V extends C> Object[] buildInternal(Iterable<K> source, int size, UpdateFunction<K, V> updateF) {
		if ((size >= 0) & (size < (BTree.FAN_FACTOR))) {
			if (size == 0)
				return BTree.EMPTY_LEAF;

			V[] values = ((V[]) (new Object[size | 1]));
			{
				int i = 0;
				for (K k : source)
					values[(i++)] = updateF.apply(k);

			}
			if (updateF != (UpdateFunction.noOp()))
				updateF.allocated(ObjectSizes.sizeOfArray(values));

			return values;
		}
		return null;
	}

	public static <C, K extends C, V extends C> Object[] update(Object[] btree, Comparator<C> comparator, Collection<K> updateWith, UpdateFunction<K, V> updateF) {
		return BTree.update(btree, comparator, updateWith, updateWith.size(), updateF);
	}

	public static <C, K extends C, V extends C> Object[] update(Object[] btree, Comparator<C> comparator, Iterable<K> updateWith, int updateWithLength, UpdateFunction<K, V> updateF) {
		if (BTree.isEmpty(btree))
			return BTree.build(updateWith, updateWithLength, updateF);

		return btree;
	}

	public static <K> Object[] merge(Object[] tree1, Object[] tree2, Comparator<? super K> comparator, UpdateFunction<K, K> updateF) {
		if ((BTree.size(tree1)) < (BTree.size(tree2))) {
			Object[] tmp = tree1;
			tree1 = tree2;
			tree2 = tmp;
		}
		return BTree.update(tree1, comparator, new BTreeSet<>(tree2, comparator), updateF);
	}

	public static <V> Iterator<V> iterator(Object[] btree) {
		return BTree.iterator(btree, BTree.Dir.ASC);
	}

	public static <V> Iterator<V> iterator(Object[] btree, BTree.Dir dir) {
		return null;
	}

	public static <V> Iterator<V> iterator(Object[] btree, int lb, int ub, BTree.Dir dir) {
		return null;
	}

	public static <V> Iterable<V> iterable(Object[] btree) {
		return BTree.iterable(btree, BTree.Dir.ASC);
	}

	public static <V> Iterable<V> iterable(Object[] btree, BTree.Dir dir) {
		return () -> BTree.iterator(btree, dir);
	}

	public static <V> Iterable<V> iterable(Object[] btree, int lb, int ub, BTree.Dir dir) {
		return () -> BTree.iterator(btree, lb, ub, dir);
	}

	public static <K, V> BTreeSearchIterator<K, V> slice(Object[] btree, Comparator<? super K> comparator, BTree.Dir dir) {
		return null;
	}

	public static <K, V extends K> BTreeSearchIterator<K, V> slice(Object[] btree, Comparator<? super K> comparator, K start, K end, BTree.Dir dir) {
		return BTree.slice(btree, comparator, start, true, end, false, dir);
	}

	public static <K, V extends K> BTreeSearchIterator<K, V> slice(Object[] btree, Comparator<? super K> comparator, K start, boolean startInclusive, K end, boolean endInclusive, BTree.Dir dir) {
		int inclusiveLowerBound = Math.max(0, (start == null ? Integer.MIN_VALUE : startInclusive ? BTree.ceilIndex(btree, comparator, start) : BTree.higherIndex(btree, comparator, start)));
		int inclusiveUpperBound = Math.min(((BTree.size(btree)) - 1), (end == null ? Integer.MAX_VALUE : endInclusive ? BTree.floorIndex(btree, comparator, end) : BTree.lowerIndex(btree, comparator, end)));
		return null;
	}

	public static <V> V find(Object[] node, Comparator<? super V> comparator, V find) {
		while (true) {
			int keyEnd = BTree.getKeyEnd(node);
			int i = Arrays.binarySearch(((V[]) (node)), 0, keyEnd, find, comparator);
			if (i >= 0)
				return ((V) (node[i]));

			if (BTree.isLeaf(node))
				return null;

			i = (-1) - i;
			node = ((Object[]) (node[(keyEnd + i)]));
		} 
	}

	public static <V> void replaceInSitu(Object[] tree, int index, V replace) {
		if ((index < 0) | (index >= (BTree.size(tree))))
			throw new IndexOutOfBoundsException((((index + " not in range [0..") + (BTree.size(tree))) + ")"));

		while (!(BTree.isLeaf(tree))) {
			final int[] sizeMap = BTree.getSizeMap(tree);
			int boundary = Arrays.binarySearch(sizeMap, index);
			if (boundary >= 0) {
				assert boundary < ((sizeMap.length) - 1);
				tree[boundary] = replace;
				return;
			}
			boundary = (-1) - boundary;
			if (boundary > 0) {
				assert boundary < (sizeMap.length);
				index -= 1 + (sizeMap[(boundary - 1)]);
			}
			tree = ((Object[]) (tree[((BTree.getChildStart(tree)) + boundary)]));
		} 
		assert index < (BTree.getLeafKeyEnd(tree));
		tree[index] = replace;
	}

	public static <V> void replaceInSitu(Object[] node, Comparator<? super V> comparator, V find, V replace) {
		while (true) {
			int keyEnd = BTree.getKeyEnd(node);
			int i = Arrays.binarySearch(((V[]) (node)), 0, keyEnd, find, comparator);
			if (i >= 0) {
				assert find == (node[i]);
				node[i] = replace;
				return;
			}
			if (BTree.isLeaf(node))
				throw new NoSuchElementException();

			i = (-1) - i;
			node = ((Object[]) (node[(keyEnd + i)]));
		} 
	}

	public static <V> int findIndex(Object[] node, Comparator<? super V> comparator, V find) {
		int lb = 0;
		while (true) {
			int keyEnd = BTree.getKeyEnd(node);
			int i = Arrays.binarySearch(((V[]) (node)), 0, keyEnd, find, comparator);
			boolean exact = i >= 0;
			if (BTree.isLeaf(node))
				return exact ? lb + i : i - lb;

			if (!exact)
				i = (-1) - i;

			int[] sizeMap = BTree.getSizeMap(node);
			if (exact)
				return lb + (sizeMap[i]);
			else
				if (i > 0)
					lb += (sizeMap[(i - 1)]) + 1;


			node = ((Object[]) (node[(keyEnd + i)]));
		} 
	}

	public static <V> V findByIndex(Object[] tree, int index) {
		if ((index < 0) | (index >= (BTree.size(tree))))
			throw new IndexOutOfBoundsException((((index + " not in range [0..") + (BTree.size(tree))) + ")"));

		Object[] node = tree;
		while (true) {
			if (BTree.isLeaf(node)) {
				int keyEnd = BTree.getLeafKeyEnd(node);
				assert index < keyEnd;
				return ((V) (node[index]));
			}
			int[] sizeMap = BTree.getSizeMap(node);
			int boundary = Arrays.binarySearch(sizeMap, index);
			if (boundary >= 0) {
				assert boundary < ((sizeMap.length) - 1);
				return ((V) (node[boundary]));
			}
			boundary = (-1) - boundary;
			if (boundary > 0) {
				assert boundary < (sizeMap.length);
				index -= 1 + (sizeMap[(boundary - 1)]);
			}
			node = ((Object[]) (node[((BTree.getChildStart(node)) + boundary)]));
		} 
	}

	public static <V> int lowerIndex(Object[] btree, Comparator<? super V> comparator, V find) {
		int i = BTree.findIndex(btree, comparator, find);
		if (i < 0)
			i = (-1) - i;

		return i - 1;
	}

	public static <V> V lower(Object[] btree, Comparator<? super V> comparator, V find) {
		int i = BTree.lowerIndex(btree, comparator, find);
		return i >= 0 ? BTree.findByIndex(btree, i) : null;
	}

	public static <V> int floorIndex(Object[] btree, Comparator<? super V> comparator, V find) {
		int i = BTree.findIndex(btree, comparator, find);
		if (i < 0)
			i = (-2) - i;

		return i;
	}

	public static <V> V floor(Object[] btree, Comparator<? super V> comparator, V find) {
		int i = BTree.floorIndex(btree, comparator, find);
		return i >= 0 ? BTree.findByIndex(btree, i) : null;
	}

	public static <V> int higherIndex(Object[] btree, Comparator<? super V> comparator, V find) {
		int i = BTree.findIndex(btree, comparator, find);
		if (i < 0)
			i = (-1) - i;
		else
			i++;

		return i;
	}

	public static <V> V higher(Object[] btree, Comparator<? super V> comparator, V find) {
		int i = BTree.higherIndex(btree, comparator, find);
		return i < (BTree.size(btree)) ? BTree.findByIndex(btree, i) : null;
	}

	public static <V> int ceilIndex(Object[] btree, Comparator<? super V> comparator, V find) {
		int i = BTree.findIndex(btree, comparator, find);
		if (i < 0)
			i = (-1) - i;

		return i;
	}

	public static <V> V ceil(Object[] btree, Comparator<? super V> comparator, V find) {
		int i = BTree.ceilIndex(btree, comparator, find);
		return i < (BTree.size(btree)) ? BTree.findByIndex(btree, i) : null;
	}

	static int getKeyEnd(Object[] node) {
		if (BTree.isLeaf(node))
			return BTree.getLeafKeyEnd(node);
		else
			return BTree.getBranchKeyEnd(node);

	}

	static int getLeafKeyEnd(Object[] node) {
		int len = node.length;
		return (node[(len - 1)]) == null ? len - 1 : len;
	}

	static int getBranchKeyEnd(Object[] branchNode) {
		return ((branchNode.length) / 2) - 1;
	}

	static int getChildStart(Object[] branchNode) {
		return BTree.getBranchKeyEnd(branchNode);
	}

	static int getChildEnd(Object[] branchNode) {
		return (branchNode.length) - 1;
	}

	static int getChildCount(Object[] branchNode) {
		return (branchNode.length) / 2;
	}

	static int[] getSizeMap(Object[] branchNode) {
		return ((int[]) (branchNode[BTree.getChildEnd(branchNode)]));
	}

	static int lookupSizeMap(Object[] branchNode, int index) {
		return BTree.getSizeMap(branchNode)[index];
	}

	public static int size(Object[] tree) {
		if (BTree.isLeaf(tree))
			return BTree.getLeafKeyEnd(tree);

		int length = tree.length;
		return ((int[]) (tree[(length - 1)]))[((length / 2) - 1)];
	}

	public static long sizeOfStructureOnHeap(Object[] tree) {
		long size = ObjectSizes.sizeOfArray(tree);
		if (BTree.isLeaf(tree))
			return size;

		for (int i = BTree.getChildStart(tree); i < (BTree.getChildEnd(tree)); i++)
			size += BTree.sizeOfStructureOnHeap(((Object[]) (tree[i])));

		return size;
	}

	static boolean isLeaf(Object[] node) {
		return ((node.length) & 1) == 1;
	}

	public static boolean isEmpty(Object[] tree) {
		return tree == (BTree.EMPTY_LEAF);
	}

	public static int depth(Object[] tree) {
		int depth = 1;
		while (!(BTree.isLeaf(tree))) {
			depth++;
			tree = ((Object[]) (tree[BTree.getKeyEnd(tree)]));
		} 
		return depth;
	}

	public static int toArray(Object[] tree, Object[] target, int targetOffset) {
		return BTree.toArray(tree, 0, BTree.size(tree), target, targetOffset);
	}

	public static int toArray(Object[] tree, int treeStart, int treeEnd, Object[] target, int targetOffset) {
		if (BTree.isLeaf(tree)) {
			int count = treeEnd - treeStart;
			System.arraycopy(tree, treeStart, target, targetOffset, count);
			return count;
		}
		int newTargetOffset = targetOffset;
		int childCount = BTree.getChildCount(tree);
		int childOffset = BTree.getChildStart(tree);
		for (int i = 0; i < childCount; i++) {
			int childStart = BTree.treeIndexOffsetOfChild(tree, i);
			int childEnd = BTree.treeIndexOfBranchKey(tree, i);
			if ((childStart <= treeEnd) && (childEnd >= treeStart)) {
				newTargetOffset += BTree.toArray(((Object[]) (tree[(childOffset + i)])), Math.max(0, (treeStart - childStart)), ((Math.min(childEnd, treeEnd)) - childStart), target, newTargetOffset);
				if ((treeStart <= childEnd) && (treeEnd > childEnd))
					target[(newTargetOffset++)] = tree[i];

			}
		}
		return newTargetOffset - targetOffset;
	}

	private static class FiltrationTracker<V> implements Function<V, V> {
		final Function<? super V, ? extends V> wrapped;

		int index;

		boolean failed;

		private FiltrationTracker(Function<? super V, ? extends V> wrapped) {
			this.wrapped = wrapped;
		}

		public V apply(V i) {
			V o = wrapped.apply(i);
			if (o != null)
				(index)++;
			else
				failed = true;

			return o;
		}
	}

	public static <V> Object[] transformAndFilter(Object[] btree, Function<? super V, ? extends V> function) {
		if (BTree.isEmpty(btree))
			return btree;

		BTree.FiltrationTracker<V> wrapped = new BTree.FiltrationTracker<>(function);
		Object[] result = BTree.transformAndFilter(btree, wrapped);
		if (!(wrapped.failed))
			return result;

		Iterable<V> head = BTree.iterable(result, 0, ((wrapped.index) - 1), BTree.Dir.ASC);
		Iterable<V> remainder = BTree.iterable(btree, ((wrapped.index) + 1), ((BTree.size(btree)) - 1), BTree.Dir.ASC);
		remainder = Iterables.filter(Iterables.transform(remainder, function), ( x) -> x != null);
		Iterable<V> build = Iterables.concat(head, remainder);
		return BTree.buildInternal(build, (-1), UpdateFunction.<V>noOp());
	}

	private static <V> Object[] transformAndFilter(Object[] btree, BTree.FiltrationTracker<V> function) {
		Object[] result = btree;
		boolean isLeaf = BTree.isLeaf(btree);
		int childOffset = (isLeaf) ? Integer.MAX_VALUE : BTree.getChildStart(btree);
		int limit = (isLeaf) ? BTree.getLeafKeyEnd(btree) : (btree.length) - 1;
		for (int i = 0; i < limit; i++) {
			int idx = (isLeaf) ? i : (i / 2) + ((i % 2) == 0 ? childOffset : 0);
			Object current = btree[idx];
			Object updated = (idx < childOffset) ? function.apply(((V) (current))) : BTree.transformAndFilter(((Object[]) (current)), function);
			if (updated != current) {
				if (result == btree)
					result = btree.clone();

				result[idx] = updated;
			}
			if (function.failed)
				return result;

		}
		return result;
	}

	public static boolean equals(Object[] a, Object[] b) {
		return ((BTree.size(a)) == (BTree.size(b))) && (Iterators.elementsEqual(BTree.iterator(a), BTree.iterator(b)));
	}

	public static int hashCode(Object[] btree) {
		int result = 1;
		for (Object v : BTree.iterable(btree))
			result = (31 * result) + (Objects.hashCode(v));

		return result;
	}

	public static int treeIndexOfKey(Object[] root, int keyIndex) {
		if (BTree.isLeaf(root))
			return keyIndex;

		int[] sizeMap = BTree.getSizeMap(root);
		if ((keyIndex >= 0) & (keyIndex < (sizeMap.length)))
			return sizeMap[keyIndex];

		if (keyIndex < 0)
			return -1;

		return (sizeMap[(keyIndex - 1)]) + 1;
	}

	public static int treeIndexOfLeafKey(int keyIndex) {
		return keyIndex;
	}

	public static int treeIndexOfBranchKey(Object[] root, int keyIndex) {
		return BTree.lookupSizeMap(root, keyIndex);
	}

	public static int treeIndexOffsetOfChild(Object[] root, int childIndex) {
		if (childIndex == 0)
			return 0;

		return 1 + (BTree.lookupSizeMap(root, (childIndex - 1)));
	}

	static final Recycler<BTree.Builder> builderRecycler = new Recycler<BTree.Builder>() {
		protected BTree.Builder newObject(Recycler.Handle handle) {
			return new BTree.Builder(handle);
		}
	};

	public static <V> BTree.Builder<V> builder(Comparator<? super V> comparator) {
		BTree.Builder<V> builder = BTree.builderRecycler.get();
		builder.reuse(comparator);
		return builder;
	}

	public static <V> BTree.Builder<V> builder(Comparator<? super V> comparator, int initialCapacity) {
		return BTree.builder(comparator);
	}

	public static class Builder<V> {
		public static interface Resolver {
			Object resolve(Object[] array, int lb, int ub);
		}

		public static interface QuickResolver<V> {
			V resolve(V a, V b);
		}

		Comparator<? super V> comparator;

		Object[] values;

		int count;

		boolean detected = true;

		boolean auto = true;

		BTree.Builder.QuickResolver<V> quickResolver;

		final Recycler.Handle recycleHandle;

		private Builder(Recycler.Handle handle) {
			this.recycleHandle = handle;
			this.values = new Object[16];
		}

		private Builder(BTree.Builder<V> builder) {
			this.comparator = builder.comparator;
			this.values = Arrays.copyOf(builder.values, builder.values.length);
			this.count = builder.count;
			this.detected = builder.detected;
			this.auto = builder.auto;
			this.quickResolver = builder.quickResolver;
			this.recycleHandle = null;
		}

		public BTree.Builder<V> copy() {
			return new BTree.Builder<>(this);
		}

		public BTree.Builder<V> setQuickResolver(BTree.Builder.QuickResolver<V> quickResolver) {
			this.quickResolver = quickResolver;
			return this;
		}

		public void recycle() {
			if ((recycleHandle) != null) {
				this.cleanup();
				BTree.builderRecycler.recycle(this, recycleHandle);
			}
		}

		private void cleanup() {
			quickResolver = null;
			Arrays.fill(values, null);
			count = 0;
			detected = true;
			auto = true;
		}

		private void reuse(Comparator<? super V> comparator) {
			this.comparator = comparator;
		}

		public BTree.Builder<V> auto(boolean auto) {
			this.auto = auto;
			return this;
		}

		public BTree.Builder<V> add(V v) {
			if ((count) == (values.length))
				values = Arrays.copyOf(values, ((count) * 2));

			Object[] values = this.values;
			int prevCount = (this.count)++;
			values[prevCount] = v;
			if (((auto) && (detected)) && (prevCount > 0)) {
				V prev = ((V) (values[(prevCount - 1)]));
				int c = comparator.compare(prev, v);
				if ((c == 0) && (auto)) {
					count = prevCount;
					if ((quickResolver) != null)
						values[(prevCount - 1)] = quickResolver.resolve(prev, v);

				}else
					if (c > 0) {
						detected = false;
					}

			}
			return this;
		}

		public BTree.Builder<V> addAll(Collection<V> add) {
			if (((auto) && (add instanceof SortedSet)) && (BTree.Builder.equalComparators(comparator, ((SortedSet) (add)).comparator()))) {
				return mergeAll(add, add.size());
			}
			detected = false;
			if ((values.length) < ((count) + (add.size())))
				values = Arrays.copyOf(values, Math.max(((count) + (add.size())), ((count) * 2)));

			for (V v : add)
				values[((count)++)] = v;

			return this;
		}

		private static boolean equalComparators(Comparator<?> a, Comparator<?> b) {
			return (a == b) || ((BTree.Builder.isNaturalComparator(a)) && (BTree.Builder.isNaturalComparator(b)));
		}

		private static boolean isNaturalComparator(Comparator<?> a) {
			return ((a == null) || (a == (Comparator.naturalOrder()))) || (a == (Ordering.natural()));
		}

		private BTree.Builder<V> mergeAll(Iterable<V> add, int addCount) {
			assert auto;
			autoEnforce();
			int curCount = count;
			if ((values.length) < ((curCount * 2) + addCount))
				values = Arrays.copyOf(values, Math.max(((curCount * 2) + addCount), (curCount * 3)));

			if (add instanceof BTreeSet) {
				((BTreeSet) (add)).toArray(values, curCount);
			}else {
				int i = curCount;
				for (V v : add)
					values[(i++)] = v;

			}
			return mergeAll(addCount);
		}

		private BTree.Builder<V> mergeAll(int addCount) {
			Object[] a = values;
			int addOffset = count;
			int i = 0;
			int j = addOffset;
			int curEnd = addOffset;
			int addEnd = addOffset + addCount;
			while ((i < curEnd) && (j < addEnd)) {
				V ai = ((V) (a[i]));
				V aj = ((V) (a[j]));
				int c = (ai == aj) ? 0 : comparator.compare(ai, aj);
				if (c > 0)
					break;
				else
					if (c == 0) {
						if ((quickResolver) != null)
							a[i] = quickResolver.resolve(ai, aj);

						j++;
					}

				i++;
			} 
			if (j == addEnd)
				return this;

			int newCount = i;
			System.arraycopy(a, i, a, addEnd, ((count) - i));
			curEnd = addEnd + ((count) - i);
			i = addEnd;
			while ((i < curEnd) && (j < addEnd)) {
				V ai = ((V) (a[i]));
				V aj = ((V) (a[j]));
				int c = comparator.compare(ai, aj);
				if (c == 0) {
					Object newValue = ((quickResolver) == null) ? ai : quickResolver.resolve(ai, aj);
					a[(newCount++)] = newValue;
					i++;
					j++;
				}else {
					a[(newCount++)] = (c < 0) ? a[(i++)] : a[(j++)];
				}
			} 
			if (i < curEnd) {
				System.arraycopy(a, i, a, newCount, (curEnd - i));
				newCount += curEnd - i;
			}else
				if (j < addEnd) {
					if (j != newCount)
						System.arraycopy(a, j, a, newCount, (addEnd - j));

					newCount += addEnd - j;
				}

			count = newCount;
			return this;
		}

		public boolean isEmpty() {
			return (count) == 0;
		}

		public BTree.Builder<V> reverse() {
			assert !(auto);
			int mid = (count) / 2;
			for (int i = 0; i < mid; i++) {
				Object t = values[i];
				values[i] = values[((count) - (1 + i))];
				values[((count) - (1 + i))] = t;
			}
			return this;
		}

		public BTree.Builder<V> sort() {
			Arrays.sort(((V[]) (values)), 0, count, comparator);
			return this;
		}

		private void autoEnforce() {
			if ((!(detected)) && ((count) > 1)) {
				sort();
				int prevIdx = 0;
				V prev = ((V) (values[0]));
				for (int i = 1; i < (count); i++) {
					V next = ((V) (values[i]));
					if ((comparator.compare(prev, next)) != 0)
						values[(++prevIdx)] = prev = next;
					else
						if ((quickResolver) != null)
							values[prevIdx] = prev = quickResolver.resolve(prev, next);


				}
				count = prevIdx + 1;
			}
			detected = true;
		}

		public BTree.Builder<V> resolve(BTree.Builder.Resolver resolver) {
			if ((count) > 0) {
				int c = 0;
				int prev = 0;
				for (int i = 1; i < (count); i++) {
					if ((comparator.compare(((V) (values[i])), ((V) (values[prev])))) != 0) {
						values[(c++)] = resolver.resolve(((V[]) (values)), prev, i);
						prev = i;
					}
				}
				values[(c++)] = resolver.resolve(((V[]) (values)), prev, count);
				count = c;
			}
			return this;
		}

		public Object[] build() {
			try {
				if (auto)
					autoEnforce();

				return BTree.build(Arrays.asList(values).subList(0, count), UpdateFunction.noOp());
			} finally {
				this.recycle();
			}
		}
	}

	static <V> int compare(Comparator<V> cmp, Object a, Object b) {
		if (a == b)
			return 0;

		if ((a == (BTree.NEGATIVE_INFINITY)) | (b == (BTree.POSITIVE_INFINITY)))
			return -1;

		if ((b == (BTree.NEGATIVE_INFINITY)) | (a == (BTree.POSITIVE_INFINITY)))
			return 1;

		return cmp.compare(((V) (a)), ((V) (b)));
	}

	static Object POSITIVE_INFINITY = new Object();

	static Object NEGATIVE_INFINITY = new Object();

	public static boolean isWellFormed(Object[] btree, Comparator<? extends Object> cmp) {
		return BTree.isWellFormed(cmp, btree, true, BTree.NEGATIVE_INFINITY, BTree.POSITIVE_INFINITY);
	}

	private static boolean isWellFormed(Comparator<?> cmp, Object[] node, boolean isRoot, Object min, Object max) {
		if ((cmp != null) && (!(BTree.isNodeWellFormed(cmp, node, min, max))))
			return false;

		if (BTree.isLeaf(node)) {
			if (isRoot)
				return (node.length) <= ((BTree.FAN_FACTOR) + 1);

			return ((node.length) >= ((BTree.FAN_FACTOR) / 2)) && ((node.length) <= ((BTree.FAN_FACTOR) + 1));
		}
		final int keyCount = BTree.getBranchKeyEnd(node);
		if (((!isRoot) && (keyCount < ((BTree.FAN_FACTOR) / 2))) || (keyCount > ((BTree.FAN_FACTOR) + 1)))
			return false;

		int type = 0;
		int size = -1;
		int[] sizeMap = BTree.getSizeMap(node);
		for (int i = BTree.getChildStart(node); i < (BTree.getChildEnd(node)); i++) {
			Object[] child = ((Object[]) (node[i]));
			size += (BTree.size(child)) + 1;
			if ((sizeMap[(i - (BTree.getChildStart(node)))]) != size)
				return false;

			Object localmax = (i < ((node.length) - 2)) ? node[(i - (BTree.getChildStart(node)))] : max;
			if (!(BTree.isWellFormed(cmp, child, false, min, localmax)))
				return false;

			type |= (BTree.isLeaf(child)) ? 1 : 2;
			min = localmax;
		}
		return type < 3;
	}

	private static boolean isNodeWellFormed(Comparator<?> cmp, Object[] node, Object min, Object max) {
		Object previous = min;
		int end = BTree.getKeyEnd(node);
		for (int i = 0; i < end; i++) {
			Object current = node[i];
			if ((BTree.compare(cmp, previous, current)) >= 0)
				return false;

			previous = current;
		}
		return (BTree.compare(cmp, previous, max)) < 0;
	}

	public static <V> void apply(Object[] btree, Consumer<V> function, boolean reversed) {
		if (reversed)
			BTree.applyReverse(btree, function, null);
		else
			BTree.applyForwards(btree, function, null);

	}

	public static <V> void apply(Object[] btree, Consumer<V> function, Predicate<V> stopCondition, boolean reversed) {
		if (reversed)
			BTree.applyReverse(btree, function, stopCondition);
		else
			BTree.applyForwards(btree, function, stopCondition);

	}

	private static <V> boolean applyForwards(Object[] btree, Consumer<V> function, Predicate<V> stopCondition) {
		boolean isLeaf = BTree.isLeaf(btree);
		int childOffset = (isLeaf) ? Integer.MAX_VALUE : BTree.getChildStart(btree);
		int limit = (isLeaf) ? BTree.getLeafKeyEnd(btree) : (btree.length) - 1;
		for (int i = 0; i < limit; i++) {
			int idx = (isLeaf) ? i : (i / 2) + ((i % 2) == 0 ? childOffset : 0);
			Object current = btree[idx];
			if (idx < childOffset) {
				V castedCurrent = ((V) (current));
				if ((stopCondition != null) && (stopCondition.apply(castedCurrent)))
					return true;

				function.accept(castedCurrent);
			}else {
				if (BTree.applyForwards(((Object[]) (current)), function, stopCondition))
					return true;

			}
		}
		return false;
	}

	private static <V> boolean applyReverse(Object[] btree, Consumer<V> function, Predicate<V> stopCondition) {
		boolean isLeaf = BTree.isLeaf(btree);
		int childOffset = (isLeaf) ? 0 : BTree.getChildStart(btree);
		int limit = (isLeaf) ? BTree.getLeafKeyEnd(btree) : (btree.length) - 1;
		for (int i = limit - 1, visited = 0; i >= 0; i-- , visited++) {
			int idx = i;
			if (!isLeaf) {
				int typeOffset = visited / 2;
				if ((i % 2) == 0) {
					idx += typeOffset;
				}else {
					idx = (i - childOffset) + typeOffset;
				}
			}
			Object current = btree[idx];
			if (isLeaf || (idx < childOffset)) {
				V castedCurrent = ((V) (current));
				if ((stopCondition != null) && (stopCondition.apply(castedCurrent)))
					return true;

				function.accept(castedCurrent);
			}else {
				if (BTree.applyReverse(((Object[]) (current)), function, stopCondition))
					return true;

			}
		}
		return false;
	}
}

