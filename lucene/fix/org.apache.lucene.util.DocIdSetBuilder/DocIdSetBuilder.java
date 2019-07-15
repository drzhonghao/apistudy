

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.lucene.index.PointValues;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BitDocIdSet;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.LSBRadixSorter;
import org.apache.lucene.util.packed.PackedInts;


public final class DocIdSetBuilder {
	public static abstract class BulkAdder {
		public abstract void add(int doc);
	}

	private static class FixedBitSetAdder extends DocIdSetBuilder.BulkAdder {
		final FixedBitSet bitSet;

		FixedBitSetAdder(FixedBitSet bitSet) {
			this.bitSet = bitSet;
		}

		@Override
		public void add(int doc) {
			bitSet.set(doc);
		}
	}

	private static class Buffer {
		int[] array;

		int length;

		Buffer(int length) {
			this.array = new int[length];
			this.length = 0;
		}

		Buffer(int[] array, int length) {
			this.array = array;
			this.length = length;
		}
	}

	private static class BufferAdder extends DocIdSetBuilder.BulkAdder {
		final DocIdSetBuilder.Buffer buffer;

		BufferAdder(DocIdSetBuilder.Buffer buffer) {
			this.buffer = buffer;
		}

		@Override
		public void add(int doc) {
			buffer.array[((buffer.length)++)] = doc;
		}
	}

	private final int maxDoc;

	private final int threshold;

	final boolean multivalued;

	final double numValuesPerDoc;

	private List<DocIdSetBuilder.Buffer> buffers = new ArrayList<>();

	private int totalAllocated;

	private FixedBitSet bitSet;

	private long counter = -1;

	private DocIdSetBuilder.BulkAdder adder;

	public DocIdSetBuilder(int maxDoc) {
		this(maxDoc, (-1), (-1));
	}

	public DocIdSetBuilder(int maxDoc, Terms terms) throws IOException {
		this(maxDoc, terms.getDocCount(), terms.getSumDocFreq());
	}

	public DocIdSetBuilder(int maxDoc, PointValues values, String field) throws IOException {
		this(maxDoc, values.getDocCount(), values.size());
	}

	DocIdSetBuilder(int maxDoc, int docCount, long valueCount) {
		this.maxDoc = maxDoc;
		this.multivalued = (docCount < 0) || (docCount != valueCount);
		if ((docCount <= 0) || (valueCount < 0)) {
			this.numValuesPerDoc = 1;
		}else {
			this.numValuesPerDoc = ((double) (valueCount)) / docCount;
		}
		assert (numValuesPerDoc) >= 1 : (("valueCount=" + valueCount) + " docCount=") + docCount;
		this.threshold = maxDoc >>> 7;
		this.bitSet = null;
	}

	public void add(DocIdSetIterator iter) throws IOException {
		if ((bitSet) != null) {
			bitSet.or(iter);
			return;
		}
		int cost = ((int) (Math.min(Integer.MAX_VALUE, iter.cost())));
		DocIdSetBuilder.BulkAdder adder = grow(cost);
		for (int i = 0; i < cost; ++i) {
			int doc = iter.nextDoc();
			if (doc == (DocIdSetIterator.NO_MORE_DOCS)) {
				return;
			}
			adder.add(doc);
		}
		for (int doc = iter.nextDoc(); doc != (DocIdSetIterator.NO_MORE_DOCS); doc = iter.nextDoc()) {
			grow(1).add(doc);
		}
	}

	public DocIdSetBuilder.BulkAdder grow(int numDocs) {
		if ((bitSet) == null) {
			if ((((long) (totalAllocated)) + numDocs) <= (threshold)) {
				ensureBufferCapacity(numDocs);
			}else {
				upgradeToBitSet();
				counter += numDocs;
			}
		}else {
			counter += numDocs;
		}
		return adder;
	}

	private void ensureBufferCapacity(int numDocs) {
		if (buffers.isEmpty()) {
			addBuffer(additionalCapacity(numDocs));
			return;
		}
		DocIdSetBuilder.Buffer current = buffers.get(((buffers.size()) - 1));
		if (((current.array.length) - (current.length)) >= numDocs) {
			return;
		}
		if ((current.length) < ((current.array.length) - ((current.array.length) >>> 3))) {
			growBuffer(current, additionalCapacity(numDocs));
		}else {
			addBuffer(additionalCapacity(numDocs));
		}
	}

	private int additionalCapacity(int numDocs) {
		int c = totalAllocated;
		c = Math.max((numDocs + 1), c);
		c = Math.max(32, c);
		c = Math.min(((threshold) - (totalAllocated)), c);
		return c;
	}

	private DocIdSetBuilder.Buffer addBuffer(int len) {
		DocIdSetBuilder.Buffer buffer = new DocIdSetBuilder.Buffer(len);
		buffers.add(buffer);
		adder = new DocIdSetBuilder.BufferAdder(buffer);
		totalAllocated += buffer.array.length;
		return buffer;
	}

	private void growBuffer(DocIdSetBuilder.Buffer buffer, int additionalCapacity) {
		buffer.array = Arrays.copyOf(buffer.array, ((buffer.array.length) + additionalCapacity));
		totalAllocated += additionalCapacity;
	}

	private void upgradeToBitSet() {
		assert (bitSet) == null;
		FixedBitSet bitSet = new FixedBitSet(maxDoc);
		long counter = 0;
		for (DocIdSetBuilder.Buffer buffer : buffers) {
			int[] array = buffer.array;
			int length = buffer.length;
			counter += length;
			for (int i = 0; i < length; ++i) {
				bitSet.set(array[i]);
			}
		}
		this.bitSet = bitSet;
		this.counter = counter;
		this.buffers = null;
		this.adder = new DocIdSetBuilder.FixedBitSetAdder(bitSet);
	}

	public DocIdSet build() {
		try {
			if ((bitSet) != null) {
				assert (counter) >= 0;
				final long cost = Math.round(((counter) / (numValuesPerDoc)));
				return new BitDocIdSet(bitSet, cost);
			}else {
				DocIdSetBuilder.Buffer concatenated = DocIdSetBuilder.concat(buffers);
				LSBRadixSorter sorter = new LSBRadixSorter();
				sorter.sort(PackedInts.bitsRequired(((maxDoc) - 1)), concatenated.array, concatenated.length);
				final int l;
				if (multivalued) {
					l = DocIdSetBuilder.dedup(concatenated.array, concatenated.length);
				}else {
					assert DocIdSetBuilder.noDups(concatenated.array, concatenated.length);
					l = concatenated.length;
				}
				assert l <= (concatenated.length);
				concatenated.array[l] = DocIdSetIterator.NO_MORE_DOCS;
			}
		} finally {
			this.buffers = null;
			this.bitSet = null;
		}
		return null;
	}

	private static DocIdSetBuilder.Buffer concat(List<DocIdSetBuilder.Buffer> buffers) {
		int totalLength = 0;
		DocIdSetBuilder.Buffer largestBuffer = null;
		for (DocIdSetBuilder.Buffer buffer : buffers) {
			totalLength += buffer.length;
			if ((largestBuffer == null) || ((buffer.array.length) > (largestBuffer.array.length))) {
				largestBuffer = buffer;
			}
		}
		if (largestBuffer == null) {
			return new DocIdSetBuilder.Buffer(1);
		}
		int[] docs = largestBuffer.array;
		if ((docs.length) < (totalLength + 1)) {
			docs = Arrays.copyOf(docs, (totalLength + 1));
		}
		totalLength = largestBuffer.length;
		for (DocIdSetBuilder.Buffer buffer : buffers) {
			if (buffer != largestBuffer) {
				System.arraycopy(buffer.array, 0, docs, totalLength, buffer.length);
				totalLength += buffer.length;
			}
		}
		return new DocIdSetBuilder.Buffer(docs, totalLength);
	}

	private static int dedup(int[] arr, int length) {
		if (length == 0) {
			return 0;
		}
		int l = 1;
		int previous = arr[0];
		for (int i = 1; i < length; ++i) {
			final int value = arr[i];
			assert value >= previous;
			if (value != previous) {
				arr[(l++)] = value;
				previous = value;
			}
		}
		return l;
	}

	private static boolean noDups(int[] a, int len) {
		for (int i = 1; i < len; ++i) {
			assert (a[(i - 1)]) < (a[i]);
		}
		return true;
	}
}

