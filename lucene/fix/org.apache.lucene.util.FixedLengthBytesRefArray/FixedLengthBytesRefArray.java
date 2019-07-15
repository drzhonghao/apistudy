

import java.util.Comparator;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefComparator;
import org.apache.lucene.util.BytesRefIterator;
import org.apache.lucene.util.IntroSorter;
import org.apache.lucene.util.RamUsageEstimator;


final class FixedLengthBytesRefArray {
	private final int valueLength;

	private final int valuesPerBlock;

	private int size;

	private int currentBlock = -1;

	private int nextEntry;

	private byte[][] blocks;

	public FixedLengthBytesRefArray(int valueLength) {
		this.valueLength = valueLength;
		valuesPerBlock = Math.max(1, (32768 / valueLength));
		nextEntry = valuesPerBlock;
		blocks = new byte[0][];
	}

	public void clear() {
		size = 0;
		blocks = new byte[0][];
		currentBlock = -1;
		nextEntry = valuesPerBlock;
	}

	public int append(BytesRef bytes) {
		if ((bytes.length) != (valueLength)) {
			throw new IllegalArgumentException(((("value length is " + (bytes.length)) + " but is supposed to always be ") + (valueLength)));
		}
		if ((nextEntry) == (valuesPerBlock)) {
			(currentBlock)++;
			if ((currentBlock) == (blocks.length)) {
				int size = ArrayUtil.oversize(((currentBlock) + 1), RamUsageEstimator.NUM_BYTES_OBJECT_REF);
				byte[][] next = new byte[size][];
				System.arraycopy(blocks, 0, next, 0, blocks.length);
				blocks = next;
			}
			blocks[currentBlock] = new byte[(valuesPerBlock) * (valueLength)];
			nextEntry = 0;
		}
		System.arraycopy(bytes.bytes, bytes.offset, blocks[currentBlock], ((nextEntry) * (valueLength)), valueLength);
		(nextEntry)++;
		return (size)++;
	}

	public int size() {
		return size;
	}

	private int[] sort(final Comparator<BytesRef> comp) {
		final int[] orderedEntries = new int[size()];
		for (int i = 0; i < (orderedEntries.length); i++) {
			orderedEntries[i] = i;
		}
		if (comp instanceof BytesRefComparator) {
			BytesRefComparator bComp = ((BytesRefComparator) (comp));
			return orderedEntries;
		}
		final BytesRef pivot = new BytesRef();
		final BytesRef scratch1 = new BytesRef();
		final BytesRef scratch2 = new BytesRef();
		pivot.length = valueLength;
		scratch1.length = valueLength;
		scratch2.length = valueLength;
		new IntroSorter() {
			@Override
			protected void swap(int i, int j) {
				int o = orderedEntries[i];
				orderedEntries[i] = orderedEntries[j];
				orderedEntries[j] = o;
			}

			@Override
			protected int compare(int i, int j) {
				int index1 = orderedEntries[i];
				scratch1.bytes = blocks[(index1 / (valuesPerBlock))];
				scratch1.offset = (index1 % (valuesPerBlock)) * (valueLength);
				int index2 = orderedEntries[j];
				scratch2.bytes = blocks[(index2 / (valuesPerBlock))];
				scratch2.offset = (index2 % (valuesPerBlock)) * (valueLength);
				return comp.compare(scratch1, scratch2);
			}

			@Override
			protected void setPivot(int i) {
				int index = orderedEntries[i];
				pivot.bytes = blocks[(index / (valuesPerBlock))];
				pivot.offset = (index % (valuesPerBlock)) * (valueLength);
			}

			@Override
			protected int comparePivot(int j) {
				final int index = orderedEntries[j];
				scratch2.bytes = blocks[(index / (valuesPerBlock))];
				scratch2.offset = (index % (valuesPerBlock)) * (valueLength);
				return comp.compare(pivot, scratch2);
			}
		}.sort(0, size());
		return orderedEntries;
	}

	public BytesRefIterator iterator(final Comparator<BytesRef> comp) {
		final BytesRef result = new BytesRef();
		result.length = valueLength;
		final int size = size();
		final int[] indices = sort(comp);
		return new BytesRefIterator() {
			int pos = 0;

			@Override
			public BytesRef next() {
				if ((pos) < size) {
					int index = indices[pos];
					(pos)++;
					result.bytes = blocks[(index / (valuesPerBlock))];
					result.offset = (index % (valuesPerBlock)) * (valueLength);
					return result;
				}
				return null;
			}
		};
	}
}

