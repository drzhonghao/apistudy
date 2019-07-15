

import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import org.apache.cassandra.cache.IMeasurableMemory;
import org.apache.cassandra.db.AbstractBufferClusteringPrefix;
import org.apache.cassandra.db.AbstractClusteringPrefix;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringBound;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.RangeTombstone;
import org.apache.cassandra.db.Slice;
import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.EncodingStats;
import org.apache.cassandra.utils.AbstractIterator;
import org.apache.cassandra.utils.ObjectSizes;
import org.apache.cassandra.utils.memory.AbstractAllocator;


public class RangeTombstoneList implements Iterable<RangeTombstone> , IMeasurableMemory {
	private static long EMPTY_SIZE = ObjectSizes.measure(new RangeTombstoneList(null, 0));

	private final ClusteringComparator comparator;

	private ClusteringBound[] starts;

	private ClusteringBound[] ends;

	private long[] markedAts;

	private int[] delTimes;

	private long boundaryHeapSize;

	private int size;

	private RangeTombstoneList(ClusteringComparator comparator, ClusteringBound[] starts, ClusteringBound[] ends, long[] markedAts, int[] delTimes, long boundaryHeapSize, int size) {
		assert (((starts.length) == (ends.length)) && ((starts.length) == (markedAts.length))) && ((starts.length) == (delTimes.length));
		this.comparator = comparator;
		this.starts = starts;
		this.ends = ends;
		this.markedAts = markedAts;
		this.delTimes = delTimes;
		this.size = size;
		this.boundaryHeapSize = boundaryHeapSize;
	}

	public RangeTombstoneList(ClusteringComparator comparator, int capacity) {
		this(comparator, new ClusteringBound[capacity], new ClusteringBound[capacity], new long[capacity], new int[capacity], 0, 0);
	}

	public boolean isEmpty() {
		return (size) == 0;
	}

	public int size() {
		return size;
	}

	public ClusteringComparator comparator() {
		return comparator;
	}

	public RangeTombstoneList copy() {
		return new RangeTombstoneList(comparator, Arrays.copyOf(starts, size), Arrays.copyOf(ends, size), Arrays.copyOf(markedAts, size), Arrays.copyOf(delTimes, size), boundaryHeapSize, size);
	}

	public RangeTombstoneList copy(AbstractAllocator allocator) {
		RangeTombstoneList copy = new RangeTombstoneList(comparator, new ClusteringBound[size], new ClusteringBound[size], Arrays.copyOf(markedAts, size), Arrays.copyOf(delTimes, size), boundaryHeapSize, size);
		for (int i = 0; i < (size); i++) {
			copy.starts[i] = RangeTombstoneList.clone(starts[i], allocator);
			copy.ends[i] = RangeTombstoneList.clone(ends[i], allocator);
		}
		return copy;
	}

	private static ClusteringBound clone(ClusteringBound bound, AbstractAllocator allocator) {
		ByteBuffer[] values = new ByteBuffer[bound.size()];
		for (int i = 0; i < (values.length); i++)
			values[i] = allocator.clone(bound.get(i));

		return null;
	}

	public void add(RangeTombstone tombstone) {
		add(tombstone.deletedSlice().start(), tombstone.deletedSlice().end(), tombstone.deletionTime().markedForDeleteAt(), tombstone.deletionTime().localDeletionTime());
	}

	public void add(ClusteringBound start, ClusteringBound end, long markedAt, int delTime) {
		if (isEmpty()) {
			addInternal(0, start, end, markedAt, delTime);
			return;
		}
		int c = comparator.compare(ends[((size) - 1)], start);
		if (c <= 0) {
			addInternal(size, start, end, markedAt, delTime);
		}else {
			int pos = Arrays.binarySearch(ends, 0, size, start, comparator);
			insertFrom((pos >= 0 ? pos + 1 : (-pos) - 1), start, end, markedAt, delTime);
		}
		boundaryHeapSize += (start.unsharedHeapSize()) + (end.unsharedHeapSize());
	}

	public void addAll(RangeTombstoneList tombstones) {
		if (tombstones.isEmpty())
			return;

		if (isEmpty()) {
			RangeTombstoneList.copyArrays(tombstones, this);
			return;
		}
		if ((size) > (10 * (tombstones.size))) {
			for (int i = 0; i < (tombstones.size); i++)
				add(tombstones.starts[i], tombstones.ends[i], tombstones.markedAts[i], tombstones.delTimes[i]);

		}else {
			int i = 0;
			int j = 0;
			while ((i < (size)) && (j < (tombstones.size))) {
				if ((comparator.compare(tombstones.starts[j], ends[i])) < 0) {
					insertFrom(i, tombstones.starts[j], tombstones.ends[j], tombstones.markedAts[j], tombstones.delTimes[j]);
					j++;
				}else {
					i++;
				}
			} 
			for (; j < (tombstones.size); j++)
				addInternal(size, tombstones.starts[j], tombstones.ends[j], tombstones.markedAts[j], tombstones.delTimes[j]);

		}
	}

	public boolean isDeleted(Clustering clustering, Cell cell) {
		int idx = searchInternal(clustering, 0, size);
		return (idx >= 0) && ((cell.isCounterCell()) || ((markedAts[idx]) >= (cell.timestamp())));
	}

	public DeletionTime searchDeletionTime(Clustering name) {
		int idx = searchInternal(name, 0, size);
		return idx < 0 ? null : new DeletionTime(markedAts[idx], delTimes[idx]);
	}

	public RangeTombstone search(Clustering name) {
		int idx = searchInternal(name, 0, size);
		return idx < 0 ? null : rangeTombstone(idx);
	}

	private int searchInternal(ClusteringPrefix name, int startIdx, int endIdx) {
		if (isEmpty())
			return -1;

		int pos = Arrays.binarySearch(starts, startIdx, endIdx, name, comparator);
		if (pos >= 0) {
			return (-pos) - 1;
		}else {
			int idx = (-pos) - 2;
			if (idx < 0)
				return -1;

			return (comparator.compare(name, ends[idx])) < 0 ? idx : (-idx) - 2;
		}
	}

	public int dataSize() {
		int dataSize = TypeSizes.sizeof(size);
		for (int i = 0; i < (size); i++) {
			dataSize += (starts[i].dataSize()) + (ends[i].dataSize());
			dataSize += TypeSizes.sizeof(markedAts[i]);
			dataSize += TypeSizes.sizeof(delTimes[i]);
		}
		return dataSize;
	}

	public long maxMarkedAt() {
		long max = Long.MIN_VALUE;
		for (int i = 0; i < (size); i++)
			max = Math.max(max, markedAts[i]);

		return max;
	}

	public void collectStats(EncodingStats.Collector collector) {
		for (int i = 0; i < (size); i++) {
			collector.updateTimestamp(markedAts[i]);
			collector.updateLocalDeletionTime(delTimes[i]);
		}
	}

	public void updateAllTimestamp(long timestamp) {
		for (int i = 0; i < (size); i++)
			markedAts[i] = timestamp;

	}

	private RangeTombstone rangeTombstone(int idx) {
		return new RangeTombstone(Slice.make(starts[idx], ends[idx]), new DeletionTime(markedAts[idx], delTimes[idx]));
	}

	private RangeTombstone rangeTombstoneWithNewStart(int idx, ClusteringBound newStart) {
		return new RangeTombstone(Slice.make(newStart, ends[idx]), new DeletionTime(markedAts[idx], delTimes[idx]));
	}

	private RangeTombstone rangeTombstoneWithNewEnd(int idx, ClusteringBound newEnd) {
		return new RangeTombstone(Slice.make(starts[idx], newEnd), new DeletionTime(markedAts[idx], delTimes[idx]));
	}

	private RangeTombstone rangeTombstoneWithNewBounds(int idx, ClusteringBound newStart, ClusteringBound newEnd) {
		return new RangeTombstone(Slice.make(newStart, newEnd), new DeletionTime(markedAts[idx], delTimes[idx]));
	}

	public Iterator<RangeTombstone> iterator() {
		return iterator(false);
	}

	public Iterator<RangeTombstone> iterator(boolean reversed) {
		return reversed ? new AbstractIterator<RangeTombstone>() {
			private int idx = (size) - 1;

			protected RangeTombstone computeNext() {
				if ((idx) < 0)
					return endOfData();

				return rangeTombstone(((idx)--));
			}
		} : new AbstractIterator<RangeTombstone>() {
			private int idx;

			protected RangeTombstone computeNext() {
				if ((idx) >= (size))
					return endOfData();

				return rangeTombstone(((idx)++));
			}
		};
	}

	public Iterator<RangeTombstone> iterator(final Slice slice, boolean reversed) {
		return reversed ? reverseIterator(slice) : forwardIterator(slice);
	}

	private Iterator<RangeTombstone> forwardIterator(final Slice slice) {
		int startIdx = ((slice.start()) == (ClusteringBound.BOTTOM)) ? 0 : searchInternal(slice.start(), 0, size);
		final int start = (startIdx < 0) ? (-startIdx) - 1 : startIdx;
		if (start >= (size))
			return Collections.emptyIterator();

		int finishIdx = ((slice.end()) == (ClusteringBound.TOP)) ? (size) - 1 : searchInternal(slice.end(), start, size);
		final int finish = (finishIdx < 0) ? (-finishIdx) - 2 : finishIdx;
		if (start > finish)
			return Collections.emptyIterator();

		if (start == finish) {
			ClusteringBound s = ((comparator.compare(starts[start], slice.start())) < 0) ? slice.start() : starts[start];
			ClusteringBound e = ((comparator.compare(slice.end(), ends[start])) < 0) ? slice.end() : ends[start];
			return Iterators.<RangeTombstone>singletonIterator(rangeTombstoneWithNewBounds(start, s, e));
		}
		return new AbstractIterator<RangeTombstone>() {
			private int idx = start;

			protected RangeTombstone computeNext() {
				if (((idx) >= (size)) || ((idx) > finish))
					return endOfData();

				if (((idx) == start) && ((comparator.compare(starts[idx], slice.start())) < 0))
					return rangeTombstoneWithNewStart(((idx)++), slice.start());

				if (((idx) == finish) && ((comparator.compare(slice.end(), ends[idx])) < 0))
					return rangeTombstoneWithNewEnd(((idx)++), slice.end());

				return rangeTombstone(((idx)++));
			}
		};
	}

	private Iterator<RangeTombstone> reverseIterator(final Slice slice) {
		int startIdx = ((slice.end()) == (ClusteringBound.TOP)) ? (size) - 1 : searchInternal(slice.end(), 0, size);
		final int start = (startIdx < 0) ? (-startIdx) - 2 : startIdx;
		if (start < 0)
			return Collections.emptyIterator();

		int finishIdx = ((slice.start()) == (ClusteringBound.BOTTOM)) ? 0 : searchInternal(slice.start(), 0, (start + 1));
		final int finish = (finishIdx < 0) ? (-finishIdx) - 1 : finishIdx;
		if (start < finish)
			return Collections.emptyIterator();

		if (start == finish) {
			ClusteringBound s = ((comparator.compare(starts[start], slice.start())) < 0) ? slice.start() : starts[start];
			ClusteringBound e = ((comparator.compare(slice.end(), ends[start])) < 0) ? slice.end() : ends[start];
			return Iterators.<RangeTombstone>singletonIterator(rangeTombstoneWithNewBounds(start, s, e));
		}
		return new AbstractIterator<RangeTombstone>() {
			private int idx = start;

			protected RangeTombstone computeNext() {
				if (((idx) < 0) || ((idx) < finish))
					return endOfData();

				if (((idx) == start) && ((comparator.compare(slice.end(), ends[idx])) < 0))
					return rangeTombstoneWithNewEnd(((idx)--), slice.end());

				if (((idx) == finish) && ((comparator.compare(starts[idx], slice.start())) < 0))
					return rangeTombstoneWithNewStart(((idx)--), slice.start());

				return rangeTombstone(((idx)--));
			}
		};
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof RangeTombstoneList))
			return false;

		RangeTombstoneList that = ((RangeTombstoneList) (o));
		if ((size) != (that.size))
			return false;

		for (int i = 0; i < (size); i++) {
			if (!(starts[i].equals(that.starts[i])))
				return false;

			if (!(ends[i].equals(that.ends[i])))
				return false;

			if ((markedAts[i]) != (that.markedAts[i]))
				return false;

			if ((delTimes[i]) != (that.delTimes[i]))
				return false;

		}
		return true;
	}

	@Override
	public final int hashCode() {
		int result = size;
		for (int i = 0; i < (size); i++) {
			result += (starts[i].hashCode()) + (ends[i].hashCode());
			result += ((int) ((markedAts[i]) ^ ((markedAts[i]) >>> 32)));
			result += delTimes[i];
		}
		return result;
	}

	private static void copyArrays(RangeTombstoneList src, RangeTombstoneList dst) {
		dst.grow(src.size);
		System.arraycopy(src.starts, 0, dst.starts, 0, src.size);
		System.arraycopy(src.ends, 0, dst.ends, 0, src.size);
		System.arraycopy(src.markedAts, 0, dst.markedAts, 0, src.size);
		System.arraycopy(src.delTimes, 0, dst.delTimes, 0, src.size);
		dst.size = src.size;
		dst.boundaryHeapSize = src.boundaryHeapSize;
	}

	private void insertFrom(int i, ClusteringBound start, ClusteringBound end, long markedAt, int delTime) {
		while (i < (size)) {
			assert (start.isStart()) && (end.isEnd());
			assert (i == 0) || ((comparator.compare(ends[(i - 1)], start)) <= 0);
			assert (comparator.compare(start, ends[i])) < 0;
			if (Slice.isEmpty(comparator, start, end))
				return;

			if (markedAt > (markedAts[i])) {
				if ((comparator.compare(starts[i], start)) < 0) {
					ClusteringBound newEnd = start.invert();
					if (!(Slice.isEmpty(comparator, starts[i], newEnd))) {
						addInternal(i, starts[i], newEnd, markedAts[i], delTimes[i]);
						i++;
						setInternal(i, start, ends[i], markedAts[i], delTimes[i]);
					}
				}
				int endCmp = comparator.compare(end, starts[i]);
				if (endCmp < 0) {
					addInternal(i, start, end, markedAt, delTime);
					return;
				}
				int cmp = comparator.compare(ends[i], end);
				if (cmp <= 0) {
					if ((i == ((size) - 1)) || ((comparator.compare(end, starts[(i + 1)])) <= 0)) {
						setInternal(i, start, end, markedAt, delTime);
						return;
					}
					setInternal(i, start, starts[(i + 1)].invert(), markedAt, delTime);
					start = starts[(i + 1)];
					i++;
				}else {
					addInternal(i, start, end, markedAt, delTime);
					i++;
					ClusteringBound newStart = end.invert();
					if (!(Slice.isEmpty(comparator, newStart, ends[i]))) {
						setInternal(i, newStart, ends[i], markedAts[i], delTimes[i]);
					}
					return;
				}
			}else {
				if ((comparator.compare(start, starts[i])) < 0) {
					if ((comparator.compare(end, starts[i])) <= 0) {
						addInternal(i, start, end, markedAt, delTime);
						return;
					}
					ClusteringBound newEnd = starts[i].invert();
					if (!(Slice.isEmpty(comparator, start, newEnd))) {
						addInternal(i, start, newEnd, markedAt, delTime);
						i++;
					}
				}
				if ((comparator.compare(end, ends[i])) <= 0)
					return;

				start = ends[i].invert();
				i++;
			}
		} 
		addInternal(i, start, end, markedAt, delTime);
	}

	private int capacity() {
		return starts.length;
	}

	private void addInternal(int i, ClusteringBound start, ClusteringBound end, long markedAt, int delTime) {
		assert i >= 0;
		if ((size) == (capacity()))
			growToFree(i);
		else
			if (i < (size))
				moveElements(i);


		setInternal(i, start, end, markedAt, delTime);
		(size)++;
	}

	private void growToFree(int i) {
		int newLength = (((capacity()) * 3) / 2) + 1;
		grow(i, newLength);
	}

	private void grow(int newLength) {
		if ((capacity()) < newLength)
			grow((-1), newLength);

	}

	private void grow(int i, int newLength) {
		starts = RangeTombstoneList.grow(starts, size, newLength, i);
		ends = RangeTombstoneList.grow(ends, size, newLength, i);
		markedAts = RangeTombstoneList.grow(markedAts, size, newLength, i);
		delTimes = RangeTombstoneList.grow(delTimes, size, newLength, i);
	}

	private static ClusteringBound[] grow(ClusteringBound[] a, int size, int newLength, int i) {
		if ((i < 0) || (i >= size))
			return Arrays.copyOf(a, newLength);

		ClusteringBound[] newA = new ClusteringBound[newLength];
		System.arraycopy(a, 0, newA, 0, i);
		System.arraycopy(a, i, newA, (i + 1), (size - i));
		return newA;
	}

	private static long[] grow(long[] a, int size, int newLength, int i) {
		if ((i < 0) || (i >= size))
			return Arrays.copyOf(a, newLength);

		long[] newA = new long[newLength];
		System.arraycopy(a, 0, newA, 0, i);
		System.arraycopy(a, i, newA, (i + 1), (size - i));
		return newA;
	}

	private static int[] grow(int[] a, int size, int newLength, int i) {
		if ((i < 0) || (i >= size))
			return Arrays.copyOf(a, newLength);

		int[] newA = new int[newLength];
		System.arraycopy(a, 0, newA, 0, i);
		System.arraycopy(a, i, newA, (i + 1), (size - i));
		return newA;
	}

	private void moveElements(int i) {
		if (i >= (size))
			return;

		System.arraycopy(starts, i, starts, (i + 1), ((size) - i));
		System.arraycopy(ends, i, ends, (i + 1), ((size) - i));
		System.arraycopy(markedAts, i, markedAts, (i + 1), ((size) - i));
		System.arraycopy(delTimes, i, delTimes, (i + 1), ((size) - i));
		starts[i] = null;
	}

	private void setInternal(int i, ClusteringBound start, ClusteringBound end, long markedAt, int delTime) {
		if ((starts[i]) != null)
			boundaryHeapSize -= (starts[i].unsharedHeapSize()) + (ends[i].unsharedHeapSize());

		starts[i] = start;
		ends[i] = end;
		markedAts[i] = markedAt;
		delTimes[i] = delTime;
		boundaryHeapSize += (start.unsharedHeapSize()) + (end.unsharedHeapSize());
	}

	@Override
	public long unsharedHeapSize() {
		return (((((RangeTombstoneList.EMPTY_SIZE) + (boundaryHeapSize)) + (ObjectSizes.sizeOfArray(starts))) + (ObjectSizes.sizeOfArray(ends))) + (ObjectSizes.sizeOfArray(markedAts))) + (ObjectSizes.sizeOfArray(delTimes));
	}
}

