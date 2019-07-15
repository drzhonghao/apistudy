

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.MutablePointValues;
import org.apache.lucene.index.MergeState;
import org.apache.lucene.index.MergeState.DocMap;
import org.apache.lucene.index.PointValues;
import org.apache.lucene.store.ChecksumIndexInput;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.GrowableByteArrayDataOutput;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.RAMOutputStream;
import org.apache.lucene.store.TrackingDirectoryWrapper;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefComparator;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.LongBitSet;
import org.apache.lucene.util.MSBRadixSorter;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.OfflineSorter;
import org.apache.lucene.util.PriorityQueue;
import org.apache.lucene.util.StringHelper;
import org.apache.lucene.util.bkd.BKDReader;
import org.apache.lucene.util.bkd.HeapPointWriter;
import org.apache.lucene.util.bkd.MutablePointsReaderUtils;
import org.apache.lucene.util.bkd.OfflinePointWriter;
import org.apache.lucene.util.bkd.PointReader;
import org.apache.lucene.util.bkd.PointWriter;

import static org.apache.lucene.index.PointValues.Relation.CELL_CROSSES_QUERY;
import static org.apache.lucene.util.OfflineSorter.BufferSize.megabytes;


public class BKDWriter implements Closeable {
	public static final String CODEC_NAME = "BKD";

	public static final int VERSION_START = 0;

	public static final int VERSION_COMPRESSED_DOC_IDS = 1;

	public static final int VERSION_COMPRESSED_VALUES = 2;

	public static final int VERSION_IMPLICIT_SPLIT_DIM_1D = 3;

	public static final int VERSION_PACKED_INDEX = 4;

	public static final int VERSION_CURRENT = BKDWriter.VERSION_PACKED_INDEX;

	private final int bytesPerDoc;

	public static final int DEFAULT_MAX_POINTS_IN_LEAF_NODE = 1024;

	public static final float DEFAULT_MAX_MB_SORT_IN_HEAP = 16.0F;

	public static final int MAX_DIMS = 8;

	protected final int numDims;

	protected final int bytesPerDim;

	protected final int packedBytesLength;

	final TrackingDirectoryWrapper tempDir;

	final String tempFileNamePrefix;

	final double maxMBSortInHeap;

	final byte[] scratchDiff;

	final byte[] scratch1;

	final byte[] scratch2;

	final BytesRef scratchBytesRef1 = new BytesRef();

	final BytesRef scratchBytesRef2 = new BytesRef();

	final int[] commonPrefixLengths;

	protected final FixedBitSet docsSeen;

	private OfflinePointWriter offlinePointWriter;

	private HeapPointWriter heapPointWriter;

	private IndexOutput tempInput;

	protected final int maxPointsInLeafNode;

	private final int maxPointsSortInHeap;

	protected final byte[] minPackedValue;

	protected final byte[] maxPackedValue;

	protected long pointCount;

	protected final boolean longOrds;

	private final long totalPointCount;

	protected final boolean singleValuePerDoc;

	protected final OfflineSorter.BufferSize offlineSorterBufferMB;

	protected final int offlineSorterMaxTempFiles;

	private final int maxDoc;

	public BKDWriter(int maxDoc, Directory tempDir, String tempFileNamePrefix, int numDims, int bytesPerDim, int maxPointsInLeafNode, double maxMBSortInHeap, long totalPointCount, boolean singleValuePerDoc) throws IOException {
		this(maxDoc, tempDir, tempFileNamePrefix, numDims, bytesPerDim, maxPointsInLeafNode, maxMBSortInHeap, totalPointCount, singleValuePerDoc, (totalPointCount > (Integer.MAX_VALUE)), Math.max(1, ((long) (maxMBSortInHeap))), OfflineSorter.MAX_TEMPFILES);
	}

	protected BKDWriter(int maxDoc, Directory tempDir, String tempFileNamePrefix, int numDims, int bytesPerDim, int maxPointsInLeafNode, double maxMBSortInHeap, long totalPointCount, boolean singleValuePerDoc, boolean longOrds, long offlineSorterBufferMB, int offlineSorterMaxTempFiles) throws IOException {
		BKDWriter.verifyParams(numDims, maxPointsInLeafNode, maxMBSortInHeap, totalPointCount);
		this.tempDir = new TrackingDirectoryWrapper(tempDir);
		this.tempFileNamePrefix = tempFileNamePrefix;
		this.maxPointsInLeafNode = maxPointsInLeafNode;
		this.numDims = numDims;
		this.bytesPerDim = bytesPerDim;
		this.totalPointCount = totalPointCount;
		this.maxDoc = maxDoc;
		this.offlineSorterBufferMB = megabytes(offlineSorterBufferMB);
		this.offlineSorterMaxTempFiles = offlineSorterMaxTempFiles;
		docsSeen = new FixedBitSet(maxDoc);
		packedBytesLength = numDims * bytesPerDim;
		scratchDiff = new byte[bytesPerDim];
		scratch1 = new byte[packedBytesLength];
		scratch2 = new byte[packedBytesLength];
		commonPrefixLengths = new int[numDims];
		minPackedValue = new byte[packedBytesLength];
		maxPackedValue = new byte[packedBytesLength];
		this.longOrds = longOrds;
		this.singleValuePerDoc = singleValuePerDoc;
		if (singleValuePerDoc) {
			assert longOrds == false;
			bytesPerDoc = (packedBytesLength) + (Integer.BYTES);
		}else
			if (longOrds) {
				bytesPerDoc = ((packedBytesLength) + (Long.BYTES)) + (Integer.BYTES);
			}else {
				bytesPerDoc = ((packedBytesLength) + (Integer.BYTES)) + (Integer.BYTES);
			}

		maxPointsSortInHeap = ((int) ((0.5 * ((maxMBSortInHeap * 1024) * 1024)) / ((bytesPerDoc) * numDims)));
		if ((maxPointsSortInHeap) < maxPointsInLeafNode) {
			throw new IllegalArgumentException((((((("maxMBSortInHeap=" + maxMBSortInHeap) + " only allows for maxPointsSortInHeap=") + (maxPointsSortInHeap)) + ", but this is less than maxPointsInLeafNode=") + maxPointsInLeafNode) + "; either increase maxMBSortInHeap or decrease maxPointsInLeafNode"));
		}
		heapPointWriter = new HeapPointWriter(16, maxPointsSortInHeap, packedBytesLength, longOrds, singleValuePerDoc);
		this.maxMBSortInHeap = maxMBSortInHeap;
	}

	public static void verifyParams(int numDims, int maxPointsInLeafNode, double maxMBSortInHeap, long totalPointCount) {
		if ((numDims < 1) || (numDims > (BKDWriter.MAX_DIMS))) {
			throw new IllegalArgumentException((((("numDims must be 1 .. " + (BKDWriter.MAX_DIMS)) + " (got: ") + numDims) + ")"));
		}
		if (maxPointsInLeafNode <= 0) {
			throw new IllegalArgumentException(("maxPointsInLeafNode must be > 0; got " + maxPointsInLeafNode));
		}
		if (maxPointsInLeafNode > (ArrayUtil.MAX_ARRAY_LENGTH)) {
			throw new IllegalArgumentException(((("maxPointsInLeafNode must be <= ArrayUtil.MAX_ARRAY_LENGTH (= " + (ArrayUtil.MAX_ARRAY_LENGTH)) + "); got ") + maxPointsInLeafNode));
		}
		if (maxMBSortInHeap < 0.0) {
			throw new IllegalArgumentException((("maxMBSortInHeap must be >= 0.0 (got: " + maxMBSortInHeap) + ")"));
		}
		if (totalPointCount < 0) {
			throw new IllegalArgumentException((("totalPointCount must be >=0 (got: " + totalPointCount) + ")"));
		}
	}

	private void spillToOffline() throws IOException {
		offlinePointWriter = new OfflinePointWriter(tempDir, tempFileNamePrefix, packedBytesLength, longOrds, "spill", 0, singleValuePerDoc);
		tempInput = offlinePointWriter.out;
		PointReader reader = heapPointWriter.getReader(0, pointCount);
		for (int i = 0; i < (pointCount); i++) {
			boolean hasNext = reader.next();
			assert hasNext;
			offlinePointWriter.append(reader.packedValue(), i, heapPointWriter.docIDs[i]);
		}
		heapPointWriter = null;
	}

	public void add(byte[] packedValue, int docID) throws IOException {
		if ((packedValue.length) != (packedBytesLength)) {
			throw new IllegalArgumentException((((("packedValue should be length=" + (packedBytesLength)) + " (got: ") + (packedValue.length)) + ")"));
		}
		if ((pointCount) >= (maxPointsSortInHeap)) {
			if ((offlinePointWriter) == null) {
				spillToOffline();
			}
			offlinePointWriter.append(packedValue, pointCount, docID);
		}else {
			heapPointWriter.append(packedValue, pointCount, docID);
		}
		if ((pointCount) == 0) {
			System.arraycopy(packedValue, 0, minPackedValue, 0, packedBytesLength);
			System.arraycopy(packedValue, 0, maxPackedValue, 0, packedBytesLength);
		}else {
			for (int dim = 0; dim < (numDims); dim++) {
				int offset = dim * (bytesPerDim);
				if ((StringHelper.compare(bytesPerDim, packedValue, offset, minPackedValue, offset)) < 0) {
					System.arraycopy(packedValue, offset, minPackedValue, offset, bytesPerDim);
				}
				if ((StringHelper.compare(bytesPerDim, packedValue, offset, maxPackedValue, offset)) > 0) {
					System.arraycopy(packedValue, offset, maxPackedValue, offset, bytesPerDim);
				}
			}
		}
		(pointCount)++;
		if ((pointCount) > (totalPointCount)) {
			throw new IllegalStateException((((("totalPointCount=" + (totalPointCount)) + " was passed when we were created, but we just hit ") + (pointCount)) + " values"));
		}
		docsSeen.set(docID);
	}

	public long getPointCount() {
		return pointCount;
	}

	private static class MergeReader {
		final BKDReader bkd;

		final BKDReader.IntersectState state;

		final MergeState.DocMap docMap;

		public int docID;

		private int docBlockUpto;

		private int docsInBlock;

		private int blockID;

		private final byte[] packedValues;

		public MergeReader(BKDReader bkd, MergeState.DocMap docMap) throws IOException {
			this.bkd = bkd;
			this.docMap = docMap;
			packedValues = null;
			state = null;
		}

		public boolean next() throws IOException {
			while (true) {
				if ((docBlockUpto) == (docsInBlock)) {
					assert (docsInBlock) > 0;
					docBlockUpto = 0;
					(blockID)++;
				}
				final int index = (docBlockUpto)++;
				int mappedDocID;
				if ((docMap) == null) {
				}else {
				}
				mappedDocID = 0;
				if (mappedDocID != (-1)) {
					docID = mappedDocID;
					return true;
				}
			} 
		}
	}

	private static class BKDMergeQueue extends PriorityQueue<BKDWriter.MergeReader> {
		private final int bytesPerDim;

		public BKDMergeQueue(int bytesPerDim, int maxSize) {
			super(maxSize);
			this.bytesPerDim = bytesPerDim;
		}

		@Override
		public boolean lessThan(BKDWriter.MergeReader a, BKDWriter.MergeReader b) {
			assert a != b;
			return (a.docID) < (b.docID);
		}
	}

	public long writeField(IndexOutput out, String fieldName, MutablePointValues reader) throws IOException {
		if ((numDims) == 1) {
			return writeField1Dim(out, fieldName, reader);
		}else {
			return writeFieldNDims(out, fieldName, reader);
		}
	}

	private long writeFieldNDims(IndexOutput out, String fieldName, MutablePointValues values) throws IOException {
		if ((pointCount) != 0) {
			throw new IllegalStateException("cannot mix add and writeField");
		}
		if (((heapPointWriter) == null) && ((tempInput) == null)) {
			throw new IllegalStateException("already finished");
		}
		heapPointWriter = null;
		long countPerLeaf = pointCount = values.size();
		long innerNodeCount = 1;
		while (countPerLeaf > (maxPointsInLeafNode)) {
			countPerLeaf = (countPerLeaf + 1) / 2;
			innerNodeCount *= 2;
		} 
		int numLeaves = Math.toIntExact(innerNodeCount);
		checkMaxLeafNodeCount(numLeaves);
		final byte[] splitPackedValues = new byte[numLeaves * ((bytesPerDim) + 1)];
		final long[] leafBlockFPs = new long[numLeaves];
		Arrays.fill(minPackedValue, ((byte) (255)));
		Arrays.fill(maxPackedValue, ((byte) (0)));
		for (int i = 0; i < (Math.toIntExact(pointCount)); ++i) {
			values.getValue(i, scratchBytesRef1);
			for (int dim = 0; dim < (numDims); dim++) {
				int offset = dim * (bytesPerDim);
				if ((StringHelper.compare(bytesPerDim, scratchBytesRef1.bytes, ((scratchBytesRef1.offset) + offset), minPackedValue, offset)) < 0) {
					System.arraycopy(scratchBytesRef1.bytes, ((scratchBytesRef1.offset) + offset), minPackedValue, offset, bytesPerDim);
				}
				if ((StringHelper.compare(bytesPerDim, scratchBytesRef1.bytes, ((scratchBytesRef1.offset) + offset), maxPackedValue, offset)) > 0) {
					System.arraycopy(scratchBytesRef1.bytes, ((scratchBytesRef1.offset) + offset), maxPackedValue, offset, bytesPerDim);
				}
			}
			docsSeen.set(values.getDocID(i));
		}
		final int[] parentSplits = new int[numDims];
		build(1, numLeaves, values, 0, Math.toIntExact(pointCount), out, minPackedValue, maxPackedValue, parentSplits, splitPackedValues, leafBlockFPs, new int[maxPointsInLeafNode]);
		assert Arrays.equals(parentSplits, new int[numDims]);
		long indexFP = out.getFilePointer();
		writeIndex(out, Math.toIntExact(countPerLeaf), leafBlockFPs, splitPackedValues);
		return indexFP;
	}

	private long writeField1Dim(IndexOutput out, String fieldName, MutablePointValues reader) throws IOException {
		MutablePointsReaderUtils.sort(maxDoc, packedBytesLength, reader, 0, Math.toIntExact(reader.size()));
		final BKDWriter.OneDimensionBKDWriter oneDimWriter = new BKDWriter.OneDimensionBKDWriter(out);
		reader.intersect(new PointValues.IntersectVisitor() {
			@Override
			public void visit(int docID, byte[] packedValue) throws IOException {
				oneDimWriter.add(packedValue, docID);
			}

			@Override
			public void visit(int docID) throws IOException {
				throw new IllegalStateException();
			}

			@Override
			public PointValues.Relation compare(byte[] minPackedValue, byte[] maxPackedValue) {
				return CELL_CROSSES_QUERY;
			}
		});
		return oneDimWriter.finish();
	}

	public long merge(IndexOutput out, List<MergeState.DocMap> docMaps, List<BKDReader> readers) throws IOException {
		assert (docMaps == null) || ((readers.size()) == (docMaps.size()));
		BKDWriter.BKDMergeQueue queue = new BKDWriter.BKDMergeQueue(bytesPerDim, readers.size());
		for (int i = 0; i < (readers.size()); i++) {
			BKDReader bkd = readers.get(i);
			MergeState.DocMap docMap;
			if (docMaps == null) {
				docMap = null;
			}else {
				docMap = docMaps.get(i);
			}
			BKDWriter.MergeReader reader = new BKDWriter.MergeReader(bkd, docMap);
			if (reader.next()) {
				queue.add(reader);
			}
		}
		BKDWriter.OneDimensionBKDWriter oneDimWriter = new BKDWriter.OneDimensionBKDWriter(out);
		while ((queue.size()) != 0) {
			BKDWriter.MergeReader reader = queue.top();
			if (reader.next()) {
				queue.updateTop();
			}else {
				queue.pop();
			}
		} 
		return oneDimWriter.finish();
	}

	private final GrowableByteArrayDataOutput scratchOut = new GrowableByteArrayDataOutput((32 * 1024));

	private class OneDimensionBKDWriter {
		final IndexOutput out;

		final List<Long> leafBlockFPs = new ArrayList<>();

		final List<byte[]> leafBlockStartValues = new ArrayList<>();

		final byte[] leafValues = new byte[(maxPointsInLeafNode) * (packedBytesLength)];

		final int[] leafDocs = new int[maxPointsInLeafNode];

		private long valueCount;

		private int leafCount;

		OneDimensionBKDWriter(IndexOutput out) {
			if ((numDims) != 1) {
				throw new UnsupportedOperationException(("numDims must be 1 but got " + (numDims)));
			}
			if ((pointCount) != 0) {
				throw new IllegalStateException("cannot mix add and merge");
			}
			if (((heapPointWriter) == null) && ((tempInput) == null)) {
				throw new IllegalStateException("already finished");
			}
			heapPointWriter = null;
			this.out = out;
			lastPackedValue = new byte[packedBytesLength];
		}

		final byte[] lastPackedValue;

		private int lastDocID;

		void add(byte[] packedValue, int docID) throws IOException {
			assert valueInOrder(((valueCount) + (leafCount)), 0, lastPackedValue, packedValue, 0, docID, lastDocID);
			System.arraycopy(packedValue, 0, leafValues, ((leafCount) * (packedBytesLength)), packedBytesLength);
			leafDocs[leafCount] = docID;
			docsSeen.set(docID);
			(leafCount)++;
			if ((valueCount) > (totalPointCount)) {
				throw new IllegalStateException((((("totalPointCount=" + (totalPointCount)) + " was passed when we were created, but we just hit ") + (pointCount)) + " values"));
			}
			if ((leafCount) == (maxPointsInLeafNode)) {
				writeLeafBlock();
				leafCount = 0;
			}
			assert (lastDocID = docID) >= 0;
		}

		public long finish() throws IOException {
			if ((leafCount) > 0) {
				writeLeafBlock();
				leafCount = 0;
			}
			if ((valueCount) == 0) {
				return -1;
			}
			pointCount = valueCount;
			long indexFP = out.getFilePointer();
			int numInnerNodes = leafBlockStartValues.size();
			byte[] index = new byte[(1 + numInnerNodes) * (1 + (bytesPerDim))];
			rotateToTree(1, 0, numInnerNodes, index, leafBlockStartValues);
			long[] arr = new long[leafBlockFPs.size()];
			for (int i = 0; i < (leafBlockFPs.size()); i++) {
				arr[i] = leafBlockFPs.get(i);
			}
			writeIndex(out, maxPointsInLeafNode, arr, index);
			return indexFP;
		}

		private void writeLeafBlock() throws IOException {
			assert (leafCount) != 0;
			if ((valueCount) == 0) {
				System.arraycopy(leafValues, 0, minPackedValue, 0, packedBytesLength);
			}
			System.arraycopy(leafValues, (((leafCount) - 1) * (packedBytesLength)), maxPackedValue, 0, packedBytesLength);
			valueCount += leafCount;
			if ((leafBlockFPs.size()) > 0) {
				leafBlockStartValues.add(Arrays.copyOf(leafValues, packedBytesLength));
			}
			leafBlockFPs.add(out.getFilePointer());
			checkMaxLeafNodeCount(leafBlockFPs.size());
			int prefix = bytesPerDim;
			int offset = ((leafCount) - 1) * (packedBytesLength);
			for (int j = 0; j < (bytesPerDim); j++) {
				if ((leafValues[j]) != (leafValues[(offset + j)])) {
					prefix = j;
					break;
				}
			}
			commonPrefixLengths[0] = prefix;
			assert (scratchOut.getPosition()) == 0;
			writeLeafBlockDocs(scratchOut, leafDocs, 0, leafCount);
			writeCommonPrefixes(scratchOut, commonPrefixLengths, leafValues);
			scratchBytesRef1.length = packedBytesLength;
			scratchBytesRef1.bytes = leafValues;
			final IntFunction<BytesRef> packedValues = new IntFunction<BytesRef>() {
				@Override
				public BytesRef apply(int i) {
					scratchBytesRef1.offset = (packedBytesLength) * i;
					return scratchBytesRef1;
				}
			};
			assert valuesInOrderAndBounds(leafCount, 0, Arrays.copyOf(leafValues, packedBytesLength), Arrays.copyOfRange(leafValues, (((leafCount) - 1) * (packedBytesLength)), ((leafCount) * (packedBytesLength))), packedValues, leafDocs, 0);
			writeLeafBlockPackedValues(scratchOut, commonPrefixLengths, leafCount, 0, packedValues);
			out.writeBytes(scratchOut.getBytes(), 0, scratchOut.getPosition());
			scratchOut.reset();
		}
	}

	private void rotateToTree(int nodeID, int offset, int count, byte[] index, List<byte[]> leafBlockStartValues) {
		if (count == 1) {
			System.arraycopy(leafBlockStartValues.get(offset), 0, index, ((nodeID * (1 + (bytesPerDim))) + 1), bytesPerDim);
		}else
			if (count > 1) {
				int countAtLevel = 1;
				int totalCount = 0;
				while (true) {
					int countLeft = count - totalCount;
					if (countLeft <= countAtLevel) {
						int lastLeftCount = Math.min((countAtLevel / 2), countLeft);
						assert lastLeftCount >= 0;
						int leftHalf = ((totalCount - 1) / 2) + lastLeftCount;
						int rootOffset = offset + leftHalf;
						System.arraycopy(leafBlockStartValues.get(rootOffset), 0, index, ((nodeID * (1 + (bytesPerDim))) + 1), bytesPerDim);
						rotateToTree((2 * nodeID), offset, leftHalf, index, leafBlockStartValues);
						rotateToTree(((2 * nodeID) + 1), (rootOffset + 1), ((count - leftHalf) - 1), index, leafBlockStartValues);
						return;
					}
					totalCount += countAtLevel;
					countAtLevel *= 2;
				} 
			}else {
				assert count == 0;
			}

	}

	private void sortHeapPointWriter(final HeapPointWriter writer, int dim) {
		final int pointCount = Math.toIntExact(this.pointCount);
		new MSBRadixSorter(((bytesPerDim) + (Integer.BYTES))) {
			@Override
			protected int byteAt(int i, int k) {
				assert k >= 0;
				if (k < (bytesPerDim)) {
					int block = i / (writer.valuesPerBlock);
					int index = i % (writer.valuesPerBlock);
					return (writer.blocks.get(block)[(((index * (packedBytesLength)) + (dim * (bytesPerDim))) + k)]) & 255;
				}else {
					int s = 3 - (k - (bytesPerDim));
					return ((writer.docIDs[i]) >>> (s * 8)) & 255;
				}
			}

			@Override
			protected void swap(int i, int j) {
				int docID = writer.docIDs[i];
				writer.docIDs[i] = writer.docIDs[j];
				writer.docIDs[j] = docID;
				if ((singleValuePerDoc) == false) {
					if (longOrds) {
						long ord = writer.ordsLong[i];
						writer.ordsLong[i] = writer.ordsLong[j];
						writer.ordsLong[j] = ord;
					}else {
						int ord = writer.ords[i];
						writer.ords[i] = writer.ords[j];
						writer.ords[j] = ord;
					}
				}
				byte[] blockI = writer.blocks.get((i / (writer.valuesPerBlock)));
				int indexI = (i % (writer.valuesPerBlock)) * (packedBytesLength);
				byte[] blockJ = writer.blocks.get((j / (writer.valuesPerBlock)));
				int indexJ = (j % (writer.valuesPerBlock)) * (packedBytesLength);
				System.arraycopy(blockI, indexI, scratch1, 0, packedBytesLength);
				System.arraycopy(blockJ, indexJ, blockI, indexI, packedBytesLength);
				System.arraycopy(scratch1, 0, blockJ, indexJ, packedBytesLength);
			}
		}.sort(0, pointCount);
	}

	private PointWriter sort(int dim) throws IOException {
		assert (dim >= 0) && (dim < (numDims));
		if ((heapPointWriter) != null) {
			assert (tempInput) == null;
			HeapPointWriter sorted;
			if (dim == 0) {
				sorted = heapPointWriter;
			}else {
				sorted = new HeapPointWriter(((int) (pointCount)), ((int) (pointCount)), packedBytesLength, longOrds, singleValuePerDoc);
				sorted.copyFrom(heapPointWriter);
			}
			sortHeapPointWriter(sorted, dim);
			sorted.close();
			return sorted;
		}else {
			assert (tempInput) != null;
			final int offset = (bytesPerDim) * dim;
			Comparator<BytesRef> cmp;
			if (dim == ((numDims) - 1)) {
				cmp = new BytesRefComparator(((bytesPerDim) + (Integer.BYTES))) {
					@Override
					protected int byteAt(BytesRef ref, int i) {
						return (ref.bytes[(((ref.offset) + offset) + i)]) & 255;
					}
				};
			}else {
				cmp = new BytesRefComparator(((bytesPerDim) + (Integer.BYTES))) {
					@Override
					protected int byteAt(BytesRef ref, int i) {
						if (i < (bytesPerDim)) {
							return (ref.bytes[(((ref.offset) + offset) + i)]) & 255;
						}else {
							return (ref.bytes[((((ref.offset) + (packedBytesLength)) + i) - (bytesPerDim))]) & 255;
						}
					}
				};
			}
			OfflineSorter sorter = new OfflineSorter(tempDir, (((tempFileNamePrefix) + "_bkd") + dim), cmp, offlineSorterBufferMB, offlineSorterMaxTempFiles, bytesPerDoc, null, 0) {
				@Override
				protected OfflineSorter.ByteSequencesWriter getWriter(IndexOutput out, long count) {
					return new OfflineSorter.ByteSequencesWriter(out) {
						@Override
						public void write(byte[] bytes, int off, int len) throws IOException {
							assert len == (bytesPerDoc) : (("len=" + len) + " bytesPerDoc=") + (bytesPerDoc);
							out.writeBytes(bytes, off, len);
						}
					};
				}

				@Override
				protected OfflineSorter.ByteSequencesReader getReader(ChecksumIndexInput in, String name) throws IOException {
					return new OfflineSorter.ByteSequencesReader(in, name) {
						final BytesRef scratch = new BytesRef(new byte[bytesPerDoc]);

						@Override
						public BytesRef next() throws IOException {
							if ((in.getFilePointer()) >= (end)) {
								return null;
							}
							in.readBytes(scratch.bytes, 0, bytesPerDoc);
							return scratch;
						}
					};
				}
			};
			String name = sorter.sort(tempInput.getName());
			return new OfflinePointWriter(tempDir, name, packedBytesLength, pointCount, longOrds, singleValuePerDoc);
		}
	}

	private void checkMaxLeafNodeCount(int numLeaves) {
		if (((1 + (bytesPerDim)) * ((long) (numLeaves))) > (ArrayUtil.MAX_ARRAY_LENGTH)) {
			throw new IllegalStateException((("too many nodes; increase maxPointsInLeafNode (currently " + (maxPointsInLeafNode)) + ") and reindex"));
		}
	}

	public long finish(IndexOutput out) throws IOException {
		if (((heapPointWriter) == null) && ((tempInput) == null)) {
			throw new IllegalStateException("already finished");
		}
		if ((offlinePointWriter) != null) {
			offlinePointWriter.close();
		}
		if ((pointCount) == 0) {
			throw new IllegalStateException("must index at least one point");
		}
		LongBitSet ordBitSet;
		if ((numDims) > 1) {
			if (singleValuePerDoc) {
				ordBitSet = new LongBitSet(maxDoc);
			}else {
				ordBitSet = new LongBitSet(pointCount);
			}
		}else {
			ordBitSet = null;
		}
		long countPerLeaf = pointCount;
		long innerNodeCount = 1;
		while (countPerLeaf > (maxPointsInLeafNode)) {
			countPerLeaf = (countPerLeaf + 1) / 2;
			innerNodeCount *= 2;
		} 
		int numLeaves = ((int) (innerNodeCount));
		checkMaxLeafNodeCount(numLeaves);
		byte[] splitPackedValues = new byte[Math.toIntExact((numLeaves * (1 + (bytesPerDim))))];
		long[] leafBlockFPs = new long[numLeaves];
		assert ((pointCount) / numLeaves) <= (maxPointsInLeafNode) : (((("pointCount=" + (pointCount)) + " numLeaves=") + numLeaves) + " maxPointsInLeafNode=") + (maxPointsInLeafNode);
		BKDWriter.PathSlice[] sortedPointWriters = new BKDWriter.PathSlice[numDims];
		List<Closeable> toCloseHeroically = new ArrayList<>();
		boolean success = false;
		try {
			for (int dim = 0; dim < (numDims); dim++) {
				sortedPointWriters[dim] = new BKDWriter.PathSlice(sort(dim), 0, pointCount);
			}
			if ((tempInput) != null) {
				tempDir.deleteFile(tempInput.getName());
				tempInput = null;
			}else {
				assert (heapPointWriter) != null;
				heapPointWriter = null;
			}
			final int[] parentSplits = new int[numDims];
			build(1, numLeaves, sortedPointWriters, ordBitSet, out, minPackedValue, maxPackedValue, parentSplits, splitPackedValues, leafBlockFPs, toCloseHeroically);
			assert Arrays.equals(parentSplits, new int[numDims]);
			for (BKDWriter.PathSlice slice : sortedPointWriters) {
				slice.writer.destroy();
			}
			assert tempDir.getCreatedFiles().isEmpty();
			success = true;
		} finally {
			if (success == false) {
				IOUtils.deleteFilesIgnoringExceptions(tempDir, tempDir.getCreatedFiles());
				IOUtils.closeWhileHandlingException(toCloseHeroically);
			}
		}
		long indexFP = out.getFilePointer();
		writeIndex(out, Math.toIntExact(countPerLeaf), leafBlockFPs, splitPackedValues);
		return indexFP;
	}

	private byte[] packIndex(long[] leafBlockFPs, byte[] splitPackedValues) throws IOException {
		int numLeaves = leafBlockFPs.length;
		if (((numDims) == 1) && (numLeaves > 1)) {
			int levelCount = 2;
			while (true) {
				if ((numLeaves >= levelCount) && (numLeaves <= (2 * levelCount))) {
					int lastLevel = 2 * (numLeaves - levelCount);
					assert lastLevel >= 0;
					if (lastLevel != 0) {
						long[] newLeafBlockFPs = new long[numLeaves];
						System.arraycopy(leafBlockFPs, lastLevel, newLeafBlockFPs, 0, ((leafBlockFPs.length) - lastLevel));
						System.arraycopy(leafBlockFPs, 0, newLeafBlockFPs, ((leafBlockFPs.length) - lastLevel), lastLevel);
						leafBlockFPs = newLeafBlockFPs;
					}
					break;
				}
				levelCount *= 2;
			} 
		}
		RAMOutputStream writeBuffer = new RAMOutputStream();
		List<byte[]> blocks = new ArrayList<>();
		byte[] lastSplitValues = new byte[(bytesPerDim) * (numDims)];
		int totalSize = recursePackIndex(writeBuffer, leafBlockFPs, splitPackedValues, 0L, blocks, 1, lastSplitValues, new boolean[numDims], false);
		byte[] index = new byte[totalSize];
		int upto = 0;
		for (byte[] block : blocks) {
			System.arraycopy(block, 0, index, upto, block.length);
			upto += block.length;
		}
		assert upto == totalSize;
		return index;
	}

	private int appendBlock(RAMOutputStream writeBuffer, List<byte[]> blocks) throws IOException {
		int pos = Math.toIntExact(writeBuffer.getFilePointer());
		byte[] bytes = new byte[pos];
		writeBuffer.writeTo(bytes, 0);
		writeBuffer.reset();
		blocks.add(bytes);
		return pos;
	}

	private int recursePackIndex(RAMOutputStream writeBuffer, long[] leafBlockFPs, byte[] splitPackedValues, long minBlockFP, List<byte[]> blocks, int nodeID, byte[] lastSplitValues, boolean[] negativeDeltas, boolean isLeft) throws IOException {
		if (nodeID >= (leafBlockFPs.length)) {
			int leafID = nodeID - (leafBlockFPs.length);
			if (leafID < (leafBlockFPs.length)) {
				long delta = (leafBlockFPs[leafID]) - minBlockFP;
				if (isLeft) {
					assert delta == 0;
					return 0;
				}else {
					assert (nodeID == 1) || (delta > 0) : "nodeID=" + nodeID;
					writeBuffer.writeVLong(delta);
					return appendBlock(writeBuffer, blocks);
				}
			}else {
				return 0;
			}
		}else {
			long leftBlockFP;
			if (isLeft == false) {
				leftBlockFP = getLeftMostLeafBlockFP(leafBlockFPs, nodeID);
				long delta = leftBlockFP - minBlockFP;
				assert (nodeID == 1) || (delta > 0);
				writeBuffer.writeVLong(delta);
			}else {
				leftBlockFP = minBlockFP;
			}
			int address = nodeID * (1 + (bytesPerDim));
			int splitDim = (splitPackedValues[(address++)]) & 255;
			int prefix = 0;
			for (; prefix < (bytesPerDim); prefix++) {
				if ((splitPackedValues[(address + prefix)]) != (lastSplitValues[((splitDim * (bytesPerDim)) + prefix)])) {
					break;
				}
			}
			int firstDiffByteDelta;
			if (prefix < (bytesPerDim)) {
				firstDiffByteDelta = ((splitPackedValues[(address + prefix)]) & 255) - ((lastSplitValues[((splitDim * (bytesPerDim)) + prefix)]) & 255);
				if (negativeDeltas[splitDim]) {
					firstDiffByteDelta = -firstDiffByteDelta;
				}
				assert firstDiffByteDelta > 0;
			}else {
				firstDiffByteDelta = 0;
			}
			int code = (((firstDiffByteDelta * (1 + (bytesPerDim))) + prefix) * (numDims)) + splitDim;
			writeBuffer.writeVInt(code);
			int suffix = (bytesPerDim) - prefix;
			byte[] savSplitValue = new byte[suffix];
			if (suffix > 1) {
				writeBuffer.writeBytes(splitPackedValues, ((address + prefix) + 1), (suffix - 1));
			}
			byte[] cmp = lastSplitValues.clone();
			System.arraycopy(lastSplitValues, ((splitDim * (bytesPerDim)) + prefix), savSplitValue, 0, suffix);
			System.arraycopy(splitPackedValues, (address + prefix), lastSplitValues, ((splitDim * (bytesPerDim)) + prefix), suffix);
			int numBytes = appendBlock(writeBuffer, blocks);
			int idxSav = blocks.size();
			blocks.add(null);
			boolean savNegativeDelta = negativeDeltas[splitDim];
			negativeDeltas[splitDim] = true;
			int leftNumBytes = recursePackIndex(writeBuffer, leafBlockFPs, splitPackedValues, leftBlockFP, blocks, (2 * nodeID), lastSplitValues, negativeDeltas, true);
			if ((nodeID * 2) < (leafBlockFPs.length)) {
				writeBuffer.writeVInt(leftNumBytes);
			}else {
				assert leftNumBytes == 0 : "leftNumBytes=" + leftNumBytes;
			}
			int numBytes2 = Math.toIntExact(writeBuffer.getFilePointer());
			byte[] bytes2 = new byte[numBytes2];
			writeBuffer.writeTo(bytes2, 0);
			writeBuffer.reset();
			blocks.set(idxSav, bytes2);
			negativeDeltas[splitDim] = false;
			int rightNumBytes = recursePackIndex(writeBuffer, leafBlockFPs, splitPackedValues, leftBlockFP, blocks, ((2 * nodeID) + 1), lastSplitValues, negativeDeltas, false);
			negativeDeltas[splitDim] = savNegativeDelta;
			System.arraycopy(savSplitValue, 0, lastSplitValues, ((splitDim * (bytesPerDim)) + prefix), suffix);
			assert Arrays.equals(lastSplitValues, cmp);
			return ((numBytes + numBytes2) + leftNumBytes) + rightNumBytes;
		}
	}

	private long getLeftMostLeafBlockFP(long[] leafBlockFPs, int nodeID) {
		int nodeIDIn = nodeID;
		while (nodeID < (leafBlockFPs.length)) {
			nodeID *= 2;
		} 
		int leafID = nodeID - (leafBlockFPs.length);
		long result = leafBlockFPs[leafID];
		if (result < 0) {
			throw new AssertionError(((result + " for leaf ") + leafID));
		}
		return result;
	}

	private void writeIndex(IndexOutput out, int countPerLeaf, long[] leafBlockFPs, byte[] splitPackedValues) throws IOException {
		byte[] packedIndex = packIndex(leafBlockFPs, splitPackedValues);
		writeIndex(out, countPerLeaf, leafBlockFPs.length, packedIndex);
	}

	private void writeIndex(IndexOutput out, int countPerLeaf, int numLeaves, byte[] packedIndex) throws IOException {
		CodecUtil.writeHeader(out, BKDWriter.CODEC_NAME, BKDWriter.VERSION_CURRENT);
		out.writeVInt(numDims);
		out.writeVInt(countPerLeaf);
		out.writeVInt(bytesPerDim);
		assert numLeaves > 0;
		out.writeVInt(numLeaves);
		out.writeBytes(minPackedValue, 0, packedBytesLength);
		out.writeBytes(maxPackedValue, 0, packedBytesLength);
		out.writeVLong(pointCount);
		out.writeVInt(docsSeen.cardinality());
		out.writeVInt(packedIndex.length);
		out.writeBytes(packedIndex, 0, packedIndex.length);
	}

	private void writeLeafBlockDocs(DataOutput out, int[] docIDs, int start, int count) throws IOException {
		assert count > 0 : "maxPointsInLeafNode=" + (maxPointsInLeafNode);
		out.writeVInt(count);
	}

	private void writeLeafBlockPackedValues(DataOutput out, int[] commonPrefixLengths, int count, int sortedDim, IntFunction<BytesRef> packedValues) throws IOException {
		int prefixLenSum = Arrays.stream(commonPrefixLengths).sum();
		if (prefixLenSum == (packedBytesLength)) {
			out.writeByte(((byte) (-1)));
		}else {
			assert (commonPrefixLengths[sortedDim]) < (bytesPerDim);
			out.writeByte(((byte) (sortedDim)));
			int compressedByteOffset = (sortedDim * (bytesPerDim)) + (commonPrefixLengths[sortedDim]);
			(commonPrefixLengths[sortedDim])++;
			for (int i = 0; i < count;) {
				int runLen = BKDWriter.runLen(packedValues, i, Math.min((i + 255), count), compressedByteOffset);
				assert runLen <= 255;
				BytesRef first = packedValues.apply(i);
				byte prefixByte = first.bytes[((first.offset) + compressedByteOffset)];
				out.writeByte(prefixByte);
				out.writeByte(((byte) (runLen)));
				writeLeafBlockPackedValuesRange(out, commonPrefixLengths, i, (i + runLen), packedValues);
				i += runLen;
				assert i <= count;
			}
		}
	}

	private void writeLeafBlockPackedValuesRange(DataOutput out, int[] commonPrefixLengths, int start, int end, IntFunction<BytesRef> packedValues) throws IOException {
		for (int i = start; i < end; ++i) {
			BytesRef ref = packedValues.apply(i);
			assert (ref.length) == (packedBytesLength);
			for (int dim = 0; dim < (numDims); dim++) {
				int prefix = commonPrefixLengths[dim];
				out.writeBytes(ref.bytes, (((ref.offset) + (dim * (bytesPerDim))) + prefix), ((bytesPerDim) - prefix));
			}
		}
	}

	private static int runLen(IntFunction<BytesRef> packedValues, int start, int end, int byteOffset) {
		BytesRef first = packedValues.apply(start);
		byte b = first.bytes[((first.offset) + byteOffset)];
		for (int i = start + 1; i < end; ++i) {
			BytesRef ref = packedValues.apply(i);
			byte b2 = ref.bytes[((ref.offset) + byteOffset)];
			assert (Byte.toUnsignedInt(b2)) >= (Byte.toUnsignedInt(b));
			if (b != b2) {
				return i - start;
			}
		}
		return end - start;
	}

	private void writeCommonPrefixes(DataOutput out, int[] commonPrefixes, byte[] packedValue) throws IOException {
		for (int dim = 0; dim < (numDims); dim++) {
			out.writeVInt(commonPrefixes[dim]);
			out.writeBytes(packedValue, (dim * (bytesPerDim)), commonPrefixes[dim]);
		}
	}

	@Override
	public void close() throws IOException {
		if ((tempInput) != null) {
			try {
				tempInput.close();
			} finally {
				tempDir.deleteFile(tempInput.getName());
				tempInput = null;
			}
		}
	}

	private static final class PathSlice {
		final PointWriter writer;

		final long start;

		final long count;

		public PathSlice(PointWriter writer, long start, long count) {
			this.writer = writer;
			this.start = start;
			this.count = count;
		}

		@Override
		public String toString() {
			return ((((("PathSlice(start=" + (start)) + " count=") + (count)) + " writer=") + (writer)) + ")";
		}
	}

	private Error verifyChecksum(Throwable priorException, PointWriter writer) throws IOException {
		assert priorException != null;
		if (writer instanceof OfflinePointWriter) {
			String tempFileName = ((OfflinePointWriter) (writer)).name;
			try (ChecksumIndexInput in = tempDir.openChecksumInput(tempFileName, IOContext.READONCE)) {
				CodecUtil.checkFooter(in, priorException);
			}
		}
		throw IOUtils.rethrowAlways(priorException);
	}

	private byte[] markRightTree(long rightCount, int splitDim, BKDWriter.PathSlice source, LongBitSet ordBitSet) throws IOException {
		try (PointReader reader = source.writer.getReader((((source.start) + (source.count)) - rightCount), rightCount)) {
			boolean result = reader.next();
			assert result : (((("rightCount=" + rightCount) + " source.count=") + (source.count)) + " source.writer=") + (source.writer);
			System.arraycopy(reader.packedValue(), (splitDim * (bytesPerDim)), scratch1, 0, bytesPerDim);
			if ((numDims) > 1) {
				assert (ordBitSet.get(reader.ord())) == false;
				ordBitSet.set(reader.ord());
				reader.markOrds((rightCount - 1), ordBitSet);
			}
		} catch (Throwable t) {
			throw verifyChecksum(t, source.writer);
		}
		return scratch1;
	}

	private boolean valueInBounds(BytesRef packedValue, byte[] minPackedValue, byte[] maxPackedValue) {
		for (int dim = 0; dim < (numDims); dim++) {
			int offset = (bytesPerDim) * dim;
			if ((StringHelper.compare(bytesPerDim, packedValue.bytes, ((packedValue.offset) + offset), minPackedValue, offset)) < 0) {
				return false;
			}
			if ((StringHelper.compare(bytesPerDim, packedValue.bytes, ((packedValue.offset) + offset), maxPackedValue, offset)) > 0) {
				return false;
			}
		}
		return true;
	}

	protected int split(byte[] minPackedValue, byte[] maxPackedValue, int[] parentSplits) {
		int maxNumSplits = 0;
		for (int numSplits : parentSplits) {
			maxNumSplits = Math.max(maxNumSplits, numSplits);
		}
		for (int dim = 0; dim < (numDims); ++dim) {
			final int offset = dim * (bytesPerDim);
			if (((parentSplits[dim]) < (maxNumSplits / 2)) && ((StringHelper.compare(bytesPerDim, minPackedValue, offset, maxPackedValue, offset)) != 0)) {
				return dim;
			}
		}
		int splitDim = -1;
		for (int dim = 0; dim < (numDims); dim++) {
			NumericUtils.subtract(bytesPerDim, dim, maxPackedValue, minPackedValue, scratchDiff);
			if ((splitDim == (-1)) || ((StringHelper.compare(bytesPerDim, scratchDiff, 0, scratch1, 0)) > 0)) {
				System.arraycopy(scratchDiff, 0, scratch1, 0, bytesPerDim);
				splitDim = dim;
			}
		}
		return splitDim;
	}

	private BKDWriter.PathSlice switchToHeap(BKDWriter.PathSlice source, List<Closeable> toCloseHeroically) throws IOException {
		int count = Math.toIntExact(source.count);
		PointReader reader = source.writer.getSharedReader(source.start, source.count, toCloseHeroically);
		try (PointWriter writer = new HeapPointWriter(count, count, packedBytesLength, longOrds, singleValuePerDoc)) {
			for (int i = 0; i < count; i++) {
				boolean hasNext = reader.next();
				assert hasNext;
				writer.append(reader.packedValue(), reader.ord(), reader.docID());
			}
			return new BKDWriter.PathSlice(writer, 0, count);
		} catch (Throwable t) {
			throw verifyChecksum(t, source.writer);
		}
	}

	private void build(int nodeID, int leafNodeOffset, MutablePointValues reader, int from, int to, IndexOutput out, byte[] minPackedValue, byte[] maxPackedValue, int[] parentSplits, byte[] splitPackedValues, long[] leafBlockFPs, int[] spareDocIds) throws IOException {
		if (nodeID >= leafNodeOffset) {
			final int count = to - from;
			assert count <= (maxPointsInLeafNode);
			Arrays.fill(commonPrefixLengths, bytesPerDim);
			reader.getValue(from, scratchBytesRef1);
			for (int i = from + 1; i < to; ++i) {
				reader.getValue(i, scratchBytesRef2);
				for (int dim = 0; dim < (numDims); dim++) {
					final int offset = dim * (bytesPerDim);
					for (int j = 0; j < (commonPrefixLengths[dim]); j++) {
						if ((scratchBytesRef1.bytes[(((scratchBytesRef1.offset) + offset) + j)]) != (scratchBytesRef2.bytes[(((scratchBytesRef2.offset) + offset) + j)])) {
							commonPrefixLengths[dim] = j;
							break;
						}
					}
				}
			}
			FixedBitSet[] usedBytes = new FixedBitSet[numDims];
			for (int dim = 0; dim < (numDims); ++dim) {
				if ((commonPrefixLengths[dim]) < (bytesPerDim)) {
					usedBytes[dim] = new FixedBitSet(256);
				}
			}
			for (int i = from + 1; i < to; ++i) {
				for (int dim = 0; dim < (numDims); dim++) {
					if ((usedBytes[dim]) != null) {
						byte b = reader.getByteAt(i, ((dim * (bytesPerDim)) + (commonPrefixLengths[dim])));
						usedBytes[dim].set(Byte.toUnsignedInt(b));
					}
				}
			}
			int sortedDim = 0;
			int sortedDimCardinality = Integer.MAX_VALUE;
			for (int dim = 0; dim < (numDims); ++dim) {
				if ((usedBytes[dim]) != null) {
					final int cardinality = usedBytes[dim].cardinality();
					if (cardinality < sortedDimCardinality) {
						sortedDim = dim;
						sortedDimCardinality = cardinality;
					}
				}
			}
			MutablePointsReaderUtils.sortByDim(sortedDim, bytesPerDim, commonPrefixLengths, reader, from, to, scratchBytesRef1, scratchBytesRef2);
			leafBlockFPs[(nodeID - leafNodeOffset)] = out.getFilePointer();
			assert (scratchOut.getPosition()) == 0;
			int[] docIDs = spareDocIds;
			for (int i = from; i < to; ++i) {
				docIDs[(i - from)] = reader.getDocID(i);
			}
			writeLeafBlockDocs(scratchOut, docIDs, 0, count);
			reader.getValue(from, scratchBytesRef1);
			System.arraycopy(scratchBytesRef1.bytes, scratchBytesRef1.offset, scratch1, 0, packedBytesLength);
			writeCommonPrefixes(scratchOut, commonPrefixLengths, scratch1);
			IntFunction<BytesRef> packedValues = new IntFunction<BytesRef>() {
				@Override
				public BytesRef apply(int i) {
					reader.getValue((from + i), scratchBytesRef1);
					return scratchBytesRef1;
				}
			};
			assert valuesInOrderAndBounds(count, sortedDim, minPackedValue, maxPackedValue, packedValues, docIDs, 0);
			writeLeafBlockPackedValues(scratchOut, commonPrefixLengths, count, sortedDim, packedValues);
			out.writeBytes(scratchOut.getBytes(), 0, scratchOut.getPosition());
			scratchOut.reset();
		}else {
			final int splitDim = split(minPackedValue, maxPackedValue, parentSplits);
			final int mid = ((from + to) + 1) >>> 1;
			int commonPrefixLen = bytesPerDim;
			for (int i = 0; i < (bytesPerDim); ++i) {
				if ((minPackedValue[((splitDim * (bytesPerDim)) + i)]) != (maxPackedValue[((splitDim * (bytesPerDim)) + i)])) {
					commonPrefixLen = i;
					break;
				}
			}
			MutablePointsReaderUtils.partition(maxDoc, splitDim, bytesPerDim, commonPrefixLen, reader, from, to, mid, scratchBytesRef1, scratchBytesRef2);
			final int address = nodeID * (1 + (bytesPerDim));
			splitPackedValues[address] = ((byte) (splitDim));
			reader.getValue(mid, scratchBytesRef1);
			System.arraycopy(scratchBytesRef1.bytes, ((scratchBytesRef1.offset) + (splitDim * (bytesPerDim))), splitPackedValues, (address + 1), bytesPerDim);
			byte[] minSplitPackedValue = Arrays.copyOf(minPackedValue, packedBytesLength);
			byte[] maxSplitPackedValue = Arrays.copyOf(maxPackedValue, packedBytesLength);
			System.arraycopy(scratchBytesRef1.bytes, ((scratchBytesRef1.offset) + (splitDim * (bytesPerDim))), minSplitPackedValue, (splitDim * (bytesPerDim)), bytesPerDim);
			System.arraycopy(scratchBytesRef1.bytes, ((scratchBytesRef1.offset) + (splitDim * (bytesPerDim))), maxSplitPackedValue, (splitDim * (bytesPerDim)), bytesPerDim);
			(parentSplits[splitDim])++;
			build((nodeID * 2), leafNodeOffset, reader, from, mid, out, minPackedValue, maxSplitPackedValue, parentSplits, splitPackedValues, leafBlockFPs, spareDocIds);
			build(((nodeID * 2) + 1), leafNodeOffset, reader, mid, to, out, minSplitPackedValue, maxPackedValue, parentSplits, splitPackedValues, leafBlockFPs, spareDocIds);
			(parentSplits[splitDim])--;
		}
	}

	private void build(int nodeID, int leafNodeOffset, BKDWriter.PathSlice[] slices, LongBitSet ordBitSet, IndexOutput out, byte[] minPackedValue, byte[] maxPackedValue, int[] parentSplits, byte[] splitPackedValues, long[] leafBlockFPs, List<Closeable> toCloseHeroically) throws IOException {
		for (BKDWriter.PathSlice slice : slices) {
			assert (slice.count) == (slices[0].count);
		}
		if ((((numDims) == 1) && ((slices[0].writer) instanceof OfflinePointWriter)) && ((slices[0].count) <= (maxPointsSortInHeap))) {
			slices[0] = switchToHeap(slices[0], toCloseHeroically);
		}
		if (nodeID >= leafNodeOffset) {
			int sortedDim = 0;
			int sortedDimCardinality = Integer.MAX_VALUE;
			for (int dim = 0; dim < (numDims); dim++) {
				if (((slices[dim].writer) instanceof HeapPointWriter) == false) {
					slices[dim] = switchToHeap(slices[dim], toCloseHeroically);
				}
				BKDWriter.PathSlice source = slices[dim];
				HeapPointWriter heapSource = ((HeapPointWriter) (source.writer));
				heapSource.readPackedValue(Math.toIntExact(source.start), scratch1);
				heapSource.readPackedValue(Math.toIntExact((((source.start) + (source.count)) - 1)), scratch2);
				int offset = dim * (bytesPerDim);
				commonPrefixLengths[dim] = bytesPerDim;
				for (int j = 0; j < (bytesPerDim); j++) {
					if ((scratch1[(offset + j)]) != (scratch2[(offset + j)])) {
						commonPrefixLengths[dim] = j;
						break;
					}
				}
				int prefix = commonPrefixLengths[dim];
				if (prefix < (bytesPerDim)) {
					int cardinality = 1;
					byte previous = scratch1[(offset + prefix)];
					for (long i = 1; i < (source.count); ++i) {
						heapSource.readPackedValue(Math.toIntExact(((source.start) + i)), scratch2);
						byte b = scratch2[(offset + prefix)];
						assert (Byte.toUnsignedInt(previous)) <= (Byte.toUnsignedInt(b));
						if (b != previous) {
							cardinality++;
							previous = b;
						}
					}
					assert cardinality <= 256;
					if (cardinality < sortedDimCardinality) {
						sortedDim = dim;
						sortedDimCardinality = cardinality;
					}
				}
			}
			BKDWriter.PathSlice source = slices[sortedDim];
			HeapPointWriter heapSource = ((HeapPointWriter) (source.writer));
			leafBlockFPs[(nodeID - leafNodeOffset)] = out.getFilePointer();
			int count = Math.toIntExact(source.count);
			assert count > 0 : (("nodeID=" + nodeID) + " leafNodeOffset=") + leafNodeOffset;
			writeLeafBlockDocs(out, heapSource.docIDs, Math.toIntExact(source.start), count);
			writeCommonPrefixes(out, commonPrefixLengths, scratch1);
			IntFunction<BytesRef> packedValues = new IntFunction<BytesRef>() {
				final BytesRef scratch = new BytesRef();

				{
					scratch.length = packedBytesLength;
				}

				@Override
				public BytesRef apply(int i) {
					heapSource.getPackedValueSlice(Math.toIntExact(((source.start) + i)), scratch);
					return scratch;
				}
			};
			assert valuesInOrderAndBounds(count, sortedDim, minPackedValue, maxPackedValue, packedValues, heapSource.docIDs, Math.toIntExact(source.start));
			writeLeafBlockPackedValues(out, commonPrefixLengths, count, sortedDim, packedValues);
		}else {
			int splitDim;
			if ((numDims) > 1) {
				splitDim = split(minPackedValue, maxPackedValue, parentSplits);
			}else {
				splitDim = 0;
			}
			BKDWriter.PathSlice source = slices[splitDim];
			assert nodeID < (splitPackedValues.length) : (("nodeID=" + nodeID) + " splitValues.length=") + (splitPackedValues.length);
			long rightCount = (source.count) / 2;
			long leftCount = (source.count) - rightCount;
			byte[] splitValue = markRightTree(rightCount, splitDim, source, ordBitSet);
			int address = nodeID * (1 + (bytesPerDim));
			splitPackedValues[address] = ((byte) (splitDim));
			System.arraycopy(splitValue, 0, splitPackedValues, (address + 1), bytesPerDim);
			BKDWriter.PathSlice[] leftSlices = new BKDWriter.PathSlice[numDims];
			BKDWriter.PathSlice[] rightSlices = new BKDWriter.PathSlice[numDims];
			byte[] minSplitPackedValue = new byte[packedBytesLength];
			System.arraycopy(minPackedValue, 0, minSplitPackedValue, 0, packedBytesLength);
			byte[] maxSplitPackedValue = new byte[packedBytesLength];
			System.arraycopy(maxPackedValue, 0, maxSplitPackedValue, 0, packedBytesLength);
			int dimToClear;
			if (((numDims) - 1) == splitDim) {
				dimToClear = (numDims) - 2;
			}else {
				dimToClear = (numDims) - 1;
			}
			for (int dim = 0; dim < (numDims); dim++) {
				if (dim == splitDim) {
					leftSlices[dim] = new BKDWriter.PathSlice(source.writer, source.start, leftCount);
					rightSlices[dim] = new BKDWriter.PathSlice(source.writer, ((source.start) + leftCount), rightCount);
					System.arraycopy(splitValue, 0, minSplitPackedValue, (dim * (bytesPerDim)), bytesPerDim);
					System.arraycopy(splitValue, 0, maxSplitPackedValue, (dim * (bytesPerDim)), bytesPerDim);
					continue;
				}
				PointReader reader = slices[dim].writer.getSharedReader(slices[dim].start, slices[dim].count, toCloseHeroically);
				try (PointWriter leftPointWriter = getPointWriter(leftCount, ("left" + dim));PointWriter rightPointWriter = getPointWriter(((source.count) - leftCount), ("right" + dim))) {
					long nextRightCount = reader.split(source.count, ordBitSet, leftPointWriter, rightPointWriter, (dim == dimToClear));
					if (rightCount != nextRightCount) {
						throw new IllegalStateException(((("wrong number of points in split: expected=" + rightCount) + " but actual=") + nextRightCount));
					}
					leftSlices[dim] = new BKDWriter.PathSlice(leftPointWriter, 0, leftCount);
					rightSlices[dim] = new BKDWriter.PathSlice(rightPointWriter, 0, rightCount);
				} catch (Throwable t) {
					throw verifyChecksum(t, slices[dim].writer);
				}
			}
			(parentSplits[splitDim])++;
			build((2 * nodeID), leafNodeOffset, leftSlices, ordBitSet, out, minPackedValue, maxSplitPackedValue, parentSplits, splitPackedValues, leafBlockFPs, toCloseHeroically);
			for (int dim = 0; dim < (numDims); dim++) {
				if (dim != splitDim) {
					leftSlices[dim].writer.destroy();
				}
			}
			build(((2 * nodeID) + 1), leafNodeOffset, rightSlices, ordBitSet, out, minSplitPackedValue, maxPackedValue, parentSplits, splitPackedValues, leafBlockFPs, toCloseHeroically);
			for (int dim = 0; dim < (numDims); dim++) {
				if (dim != splitDim) {
					rightSlices[dim].writer.destroy();
				}
			}
			(parentSplits[splitDim])--;
		}
	}

	private boolean valuesInOrderAndBounds(int count, int sortedDim, byte[] minPackedValue, byte[] maxPackedValue, IntFunction<BytesRef> values, int[] docs, int docsOffset) throws IOException {
		byte[] lastPackedValue = new byte[packedBytesLength];
		int lastDoc = -1;
		for (int i = 0; i < count; i++) {
			BytesRef packedValue = values.apply(i);
			assert (packedValue.length) == (packedBytesLength);
			assert valueInOrder(i, sortedDim, lastPackedValue, packedValue.bytes, packedValue.offset, docs[(docsOffset + i)], lastDoc);
			lastDoc = docs[(docsOffset + i)];
			assert valueInBounds(packedValue, minPackedValue, maxPackedValue);
		}
		return true;
	}

	private boolean valueInOrder(long ord, int sortedDim, byte[] lastPackedValue, byte[] packedValue, int packedValueOffset, int doc, int lastDoc) {
		int dimOffset = sortedDim * (bytesPerDim);
		if (ord > 0) {
			int cmp = StringHelper.compare(bytesPerDim, lastPackedValue, dimOffset, packedValue, (packedValueOffset + dimOffset));
			if (cmp > 0) {
				throw new AssertionError(((((("values out of order: last value=" + (new BytesRef(lastPackedValue))) + " current value=") + (new BytesRef(packedValue, packedValueOffset, packedBytesLength))) + " ord=") + ord));
			}
			if ((cmp == 0) && (doc < lastDoc)) {
				throw new AssertionError(((((("docs out of order: last doc=" + lastDoc) + " current doc=") + doc) + " ord=") + ord));
			}
		}
		System.arraycopy(packedValue, packedValueOffset, lastPackedValue, 0, packedBytesLength);
		return true;
	}

	PointWriter getPointWriter(long count, String desc) throws IOException {
		if (count <= (maxPointsSortInHeap)) {
			int size = Math.toIntExact(count);
			return new HeapPointWriter(size, size, packedBytesLength, longOrds, singleValuePerDoc);
		}else {
			return new OfflinePointWriter(tempDir, tempFileNamePrefix, packedBytesLength, longOrds, desc, count, singleValuePerDoc);
		}
	}
}

