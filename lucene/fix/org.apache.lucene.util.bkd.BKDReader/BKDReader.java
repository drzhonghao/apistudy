

import java.io.IOException;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.PointValues;
import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.MathUtil;
import org.apache.lucene.util.RamUsageEstimator;
import org.apache.lucene.util.StringHelper;
import org.apache.lucene.util.bkd.BKDWriter;

import static org.apache.lucene.index.PointValues.Relation.CELL_INSIDE_QUERY;
import static org.apache.lucene.index.PointValues.Relation.CELL_OUTSIDE_QUERY;


public final class BKDReader extends PointValues implements Accountable {
	final int leafNodeOffset;

	final int numDims;

	final int bytesPerDim;

	final int numLeaves;

	final IndexInput in;

	final int maxPointsInLeafNode;

	final byte[] minPackedValue;

	final byte[] maxPackedValue;

	final long pointCount;

	final int docCount;

	final int version;

	protected final int packedBytesLength;

	final byte[] packedIndex;

	private final byte[] splitPackedValues;

	final int bytesPerIndexEntry;

	final long[] leafBlockFPs;

	public BKDReader(IndexInput in) throws IOException {
		version = CodecUtil.checkHeader(in, BKDWriter.CODEC_NAME, BKDWriter.VERSION_START, BKDWriter.VERSION_CURRENT);
		numDims = in.readVInt();
		maxPointsInLeafNode = in.readVInt();
		bytesPerDim = in.readVInt();
		bytesPerIndexEntry = (((numDims) == 1) && ((version) >= (BKDWriter.VERSION_IMPLICIT_SPLIT_DIM_1D))) ? bytesPerDim : (bytesPerDim) + 1;
		packedBytesLength = (numDims) * (bytesPerDim);
		numLeaves = in.readVInt();
		assert (numLeaves) > 0;
		leafNodeOffset = numLeaves;
		minPackedValue = new byte[packedBytesLength];
		maxPackedValue = new byte[packedBytesLength];
		in.readBytes(minPackedValue, 0, packedBytesLength);
		in.readBytes(maxPackedValue, 0, packedBytesLength);
		for (int dim = 0; dim < (numDims); dim++) {
			if ((StringHelper.compare(bytesPerDim, minPackedValue, (dim * (bytesPerDim)), maxPackedValue, (dim * (bytesPerDim)))) > 0) {
				throw new CorruptIndexException(((((("minPackedValue " + (new BytesRef(minPackedValue))) + " is > maxPackedValue ") + (new BytesRef(maxPackedValue))) + " for dim=") + dim), in);
			}
		}
		pointCount = in.readVLong();
		docCount = in.readVInt();
		if ((version) >= (BKDWriter.VERSION_PACKED_INDEX)) {
			int numBytes = in.readVInt();
			packedIndex = new byte[numBytes];
			in.readBytes(packedIndex, 0, numBytes);
			leafBlockFPs = null;
			splitPackedValues = null;
		}else {
			splitPackedValues = new byte[(bytesPerIndexEntry) * (numLeaves)];
			in.readBytes(splitPackedValues, 0, splitPackedValues.length);
			long[] leafBlockFPs = new long[numLeaves];
			long lastFP = 0;
			for (int i = 0; i < (numLeaves); i++) {
				long delta = in.readVLong();
				leafBlockFPs[i] = lastFP + delta;
				lastFP += delta;
			}
			if (((numDims) == 1) && ((numLeaves) > 1)) {
				int levelCount = 2;
				while (true) {
					if (((numLeaves) >= levelCount) && ((numLeaves) <= (2 * levelCount))) {
						int lastLevel = 2 * ((numLeaves) - levelCount);
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
			this.leafBlockFPs = leafBlockFPs;
			packedIndex = null;
		}
		this.in = in;
	}

	long getMinLeafBlockFP() {
		if ((packedIndex) != null) {
			return new ByteArrayDataInput(packedIndex).readVLong();
		}else {
			long minFP = Long.MAX_VALUE;
			for (long fp : leafBlockFPs) {
				minFP = Math.min(minFP, fp);
			}
			return minFP;
		}
	}

	public abstract class IndexTree implements Cloneable {
		protected int nodeID;

		protected int level;

		protected int splitDim;

		protected final byte[][] splitPackedValueStack;

		protected IndexTree() {
			int treeDepth = getTreeDepth();
			splitPackedValueStack = new byte[treeDepth + 1][];
			nodeID = 1;
			level = 1;
			splitPackedValueStack[level] = new byte[packedBytesLength];
		}

		public void pushLeft() {
			nodeID *= 2;
			(level)++;
			if ((splitPackedValueStack[level]) == null) {
				splitPackedValueStack[level] = new byte[packedBytesLength];
			}
		}

		public abstract BKDReader.IndexTree clone();

		public void pushRight() {
			nodeID = ((nodeID) * 2) + 1;
			(level)++;
			if ((splitPackedValueStack[level]) == null) {
				splitPackedValueStack[level] = new byte[packedBytesLength];
			}
		}

		public void pop() {
			nodeID /= 2;
			(level)--;
			splitDim = -1;
		}

		public boolean isLeafNode() {
			return (nodeID) >= (leafNodeOffset);
		}

		public boolean nodeExists() {
			return ((nodeID) - (leafNodeOffset)) < (leafNodeOffset);
		}

		public int getNodeID() {
			return nodeID;
		}

		public byte[] getSplitPackedValue() {
			assert (isLeafNode()) == false;
			assert (splitPackedValueStack[level]) != null : "level=" + (level);
			return splitPackedValueStack[level];
		}

		public int getSplitDim() {
			assert (isLeafNode()) == false;
			return splitDim;
		}

		public abstract BytesRef getSplitDimValue();

		public abstract long getLeafBlockFP();

		public int getNumLeaves() {
			int leftMostLeafNode = nodeID;
			while (leftMostLeafNode < (leafNodeOffset)) {
				leftMostLeafNode = leftMostLeafNode * 2;
			} 
			int rightMostLeafNode = nodeID;
			while (rightMostLeafNode < (leafNodeOffset)) {
				rightMostLeafNode = (rightMostLeafNode * 2) + 1;
			} 
			final int numLeaves;
			if (rightMostLeafNode >= leftMostLeafNode) {
				numLeaves = (rightMostLeafNode - leftMostLeafNode) + 1;
			}else {
				numLeaves = ((rightMostLeafNode - leftMostLeafNode) + 1) + (leafNodeOffset);
			}
			assert numLeaves == (getNumLeavesSlow(nodeID)) : (numLeaves + " ") + (getNumLeavesSlow(nodeID));
			return numLeaves;
		}

		private int getNumLeavesSlow(int node) {
			if (node >= (2 * (leafNodeOffset))) {
				return 0;
			}else
				if (node >= (leafNodeOffset)) {
					return 1;
				}else {
					final int leftCount = getNumLeavesSlow((node * 2));
					final int rightCount = getNumLeavesSlow(((node * 2) + 1));
					return leftCount + rightCount;
				}

		}
	}

	private final class LegacyIndexTree extends BKDReader.IndexTree {
		private long leafBlockFP;

		private final byte[] splitDimValue = new byte[bytesPerDim];

		private final BytesRef scratch = new BytesRef();

		public LegacyIndexTree() {
			setNodeData();
			scratch.bytes = splitDimValue;
			scratch.length = bytesPerDim;
		}

		@Override
		public BKDReader.LegacyIndexTree clone() {
			BKDReader.LegacyIndexTree index = new BKDReader.LegacyIndexTree();
			index.nodeID = nodeID;
			index.level = level;
			index.splitDim = splitDim;
			index.leafBlockFP = leafBlockFP;
			index.splitPackedValueStack[index.level] = splitPackedValueStack[index.level].clone();
			return index;
		}

		@Override
		public void pushLeft() {
			super.pushLeft();
			setNodeData();
		}

		@Override
		public void pushRight() {
			super.pushRight();
			setNodeData();
		}

		private void setNodeData() {
			if (isLeafNode()) {
				leafBlockFP = leafBlockFPs[((nodeID) - (leafNodeOffset))];
				splitDim = -1;
			}else {
				leafBlockFP = -1;
				int address = (nodeID) * (bytesPerIndexEntry);
				if ((numDims) == 1) {
					splitDim = 0;
					if ((version) < (BKDWriter.VERSION_IMPLICIT_SPLIT_DIM_1D)) {
						assert (splitPackedValues[address]) == 0;
						address++;
					}
				}else {
					splitDim = (splitPackedValues[(address++)]) & 255;
				}
				System.arraycopy(splitPackedValues, address, splitDimValue, 0, bytesPerDim);
			}
		}

		@Override
		public long getLeafBlockFP() {
			assert isLeafNode();
			return leafBlockFP;
		}

		@Override
		public BytesRef getSplitDimValue() {
			assert (isLeafNode()) == false;
			return scratch;
		}

		@Override
		public void pop() {
			super.pop();
			leafBlockFP = -1;
		}
	}

	private final class PackedIndexTree extends BKDReader.IndexTree {
		private final ByteArrayDataInput in;

		private final long[] leafBlockFPStack;

		private final int[] leftNodePositions;

		private final int[] rightNodePositions;

		private final int[] splitDims;

		private final boolean[] negativeDeltas;

		private final byte[][] splitValuesStack;

		private final BytesRef scratch;

		public PackedIndexTree() {
			int treeDepth = getTreeDepth();
			leafBlockFPStack = new long[treeDepth + 1];
			leftNodePositions = new int[treeDepth + 1];
			rightNodePositions = new int[treeDepth + 1];
			splitValuesStack = new byte[treeDepth + 1][];
			splitDims = new int[treeDepth + 1];
			negativeDeltas = new boolean[(numDims) * (treeDepth + 1)];
			in = new ByteArrayDataInput(packedIndex);
			splitValuesStack[0] = new byte[packedBytesLength];
			readNodeData(false);
			scratch = new BytesRef();
			scratch.length = bytesPerDim;
		}

		@Override
		public BKDReader.PackedIndexTree clone() {
			BKDReader.PackedIndexTree index = new BKDReader.PackedIndexTree();
			index.nodeID = nodeID;
			index.level = level;
			index.splitDim = splitDim;
			index.leafBlockFPStack[level] = leafBlockFPStack[level];
			index.leftNodePositions[level] = leftNodePositions[level];
			index.rightNodePositions[level] = rightNodePositions[level];
			index.splitValuesStack[index.level] = splitValuesStack[index.level].clone();
			System.arraycopy(negativeDeltas, ((level) * (numDims)), index.negativeDeltas, ((level) * (numDims)), numDims);
			index.splitDims[level] = splitDims[level];
			return index;
		}

		@Override
		public void pushLeft() {
			int nodePosition = leftNodePositions[level];
			super.pushLeft();
			System.arraycopy(negativeDeltas, (((level) - 1) * (numDims)), negativeDeltas, ((level) * (numDims)), numDims);
			assert (splitDim) != (-1);
			negativeDeltas[(((level) * (numDims)) + (splitDim))] = true;
			in.setPosition(nodePosition);
			readNodeData(true);
		}

		@Override
		public void pushRight() {
			int nodePosition = rightNodePositions[level];
			super.pushRight();
			System.arraycopy(negativeDeltas, (((level) - 1) * (numDims)), negativeDeltas, ((level) * (numDims)), numDims);
			assert (splitDim) != (-1);
			negativeDeltas[(((level) * (numDims)) + (splitDim))] = false;
			in.setPosition(nodePosition);
			readNodeData(false);
		}

		@Override
		public void pop() {
			super.pop();
			splitDim = splitDims[level];
		}

		@Override
		public long getLeafBlockFP() {
			assert isLeafNode() : ("nodeID=" + (nodeID)) + " is not a leaf";
			return leafBlockFPStack[level];
		}

		@Override
		public BytesRef getSplitDimValue() {
			assert (isLeafNode()) == false;
			scratch.bytes = splitValuesStack[level];
			scratch.offset = (splitDim) * (bytesPerDim);
			return scratch;
		}

		private void readNodeData(boolean isLeft) {
			leafBlockFPStack[level] = leafBlockFPStack[((level) - 1)];
			if (isLeft == false) {
				leafBlockFPStack[level] += in.readVLong();
			}
			if (isLeafNode()) {
				splitDim = -1;
			}else {
				int code = in.readVInt();
				splitDim = code % (numDims);
				splitDims[level] = splitDim;
				code /= numDims;
				int prefix = code % (1 + (bytesPerDim));
				int suffix = (bytesPerDim) - prefix;
				if ((splitValuesStack[level]) == null) {
					splitValuesStack[level] = new byte[packedBytesLength];
				}
				System.arraycopy(splitValuesStack[((level) - 1)], 0, splitValuesStack[level], 0, packedBytesLength);
				if (suffix > 0) {
					int firstDiffByteDelta = code / (1 + (bytesPerDim));
					if (negativeDeltas[(((level) * (numDims)) + (splitDim))]) {
						firstDiffByteDelta = -firstDiffByteDelta;
					}
					int oldByte = (splitValuesStack[level][(((splitDim) * (bytesPerDim)) + prefix)]) & 255;
					splitValuesStack[level][(((splitDim) * (bytesPerDim)) + prefix)] = ((byte) (oldByte + firstDiffByteDelta));
					in.readBytes(splitValuesStack[level], ((((splitDim) * (bytesPerDim)) + prefix) + 1), (suffix - 1));
				}else {
				}
				int leftNumBytes;
				if (((nodeID) * 2) < (leafNodeOffset)) {
					leftNumBytes = in.readVInt();
				}else {
					leftNumBytes = 0;
				}
				leftNodePositions[level] = in.getPosition();
				rightNodePositions[level] = (leftNodePositions[level]) + leftNumBytes;
			}
		}
	}

	private int getTreeDepth() {
		return (MathUtil.log(numLeaves, 2)) + 2;
	}

	public static final class IntersectState {
		final IndexInput in;

		final int[] scratchDocIDs;

		final byte[] scratchPackedValue;

		final int[] commonPrefixLengths;

		final PointValues.IntersectVisitor visitor;

		public final BKDReader.IndexTree index;

		public IntersectState(IndexInput in, int numDims, int packedBytesLength, int maxPointsInLeafNode, PointValues.IntersectVisitor visitor, BKDReader.IndexTree indexVisitor) {
			this.in = in;
			this.visitor = visitor;
			this.commonPrefixLengths = new int[numDims];
			this.scratchDocIDs = new int[maxPointsInLeafNode];
			this.scratchPackedValue = new byte[packedBytesLength];
			this.index = indexVisitor;
		}
	}

	@Override
	public void intersect(PointValues.IntersectVisitor visitor) throws IOException {
		intersect(getIntersectState(visitor), minPackedValue, maxPackedValue);
	}

	@Override
	public long estimatePointCount(PointValues.IntersectVisitor visitor) {
		return estimatePointCount(getIntersectState(visitor), minPackedValue, maxPackedValue);
	}

	private void addAll(BKDReader.IntersectState state, boolean grown) throws IOException {
		if (grown == false) {
			final long maxPointCount = ((long) (maxPointsInLeafNode)) * (state.index.getNumLeaves());
			if (maxPointCount <= (Integer.MAX_VALUE)) {
				state.visitor.grow(((int) (maxPointCount)));
				grown = true;
			}
		}
		if (state.index.isLeafNode()) {
			assert grown;
			if (state.index.nodeExists()) {
				visitDocIDs(state.in, state.index.getLeafBlockFP(), state.visitor);
			}
		}else {
			state.index.pushLeft();
			addAll(state, grown);
			state.index.pop();
			state.index.pushRight();
			addAll(state, grown);
			state.index.pop();
		}
	}

	public BKDReader.IntersectState getIntersectState(PointValues.IntersectVisitor visitor) {
		BKDReader.IndexTree index;
		if ((packedIndex) != null) {
			index = new BKDReader.PackedIndexTree();
		}else {
			index = new BKDReader.LegacyIndexTree();
		}
		return new BKDReader.IntersectState(in.clone(), numDims, packedBytesLength, maxPointsInLeafNode, visitor, index);
	}

	public void visitLeafBlockValues(BKDReader.IndexTree index, BKDReader.IntersectState state) throws IOException {
		int count = readDocIDs(state.in, index.getLeafBlockFP(), state.scratchDocIDs);
		visitDocValues(state.commonPrefixLengths, state.scratchPackedValue, state.in, state.scratchDocIDs, count, state.visitor);
	}

	private void visitDocIDs(IndexInput in, long blockFP, PointValues.IntersectVisitor visitor) throws IOException {
		in.seek(blockFP);
		int count = in.readVInt();
		if ((version) < (BKDWriter.VERSION_COMPRESSED_DOC_IDS)) {
		}else {
		}
	}

	int readDocIDs(IndexInput in, long blockFP, int[] docIDs) throws IOException {
		in.seek(blockFP);
		int count = in.readVInt();
		if ((version) < (BKDWriter.VERSION_COMPRESSED_DOC_IDS)) {
		}else {
		}
		return count;
	}

	void visitDocValues(int[] commonPrefixLengths, byte[] scratchPackedValue, IndexInput in, int[] docIDs, int count, PointValues.IntersectVisitor visitor) throws IOException {
		visitor.grow(count);
		readCommonPrefixes(commonPrefixLengths, scratchPackedValue, in);
		int compressedDim = ((version) < (BKDWriter.VERSION_COMPRESSED_VALUES)) ? -1 : readCompressedDim(in);
		if (compressedDim == (-1)) {
			visitRawDocValues(commonPrefixLengths, scratchPackedValue, in, docIDs, count, visitor);
		}else {
			visitCompressedDocValues(commonPrefixLengths, scratchPackedValue, in, docIDs, count, visitor, compressedDim);
		}
	}

	private void visitRawDocValues(int[] commonPrefixLengths, byte[] scratchPackedValue, IndexInput in, int[] docIDs, int count, PointValues.IntersectVisitor visitor) throws IOException {
		for (int i = 0; i < count; ++i) {
			for (int dim = 0; dim < (numDims); dim++) {
				int prefix = commonPrefixLengths[dim];
				in.readBytes(scratchPackedValue, ((dim * (bytesPerDim)) + prefix), ((bytesPerDim) - prefix));
			}
			visitor.visit(docIDs[i], scratchPackedValue);
		}
	}

	private void visitCompressedDocValues(int[] commonPrefixLengths, byte[] scratchPackedValue, IndexInput in, int[] docIDs, int count, PointValues.IntersectVisitor visitor, int compressedDim) throws IOException {
		final int compressedByteOffset = (compressedDim * (bytesPerDim)) + (commonPrefixLengths[compressedDim]);
		(commonPrefixLengths[compressedDim])++;
		int i;
		for (i = 0; i < count;) {
			scratchPackedValue[compressedByteOffset] = in.readByte();
			final int runLen = Byte.toUnsignedInt(in.readByte());
			for (int j = 0; j < runLen; ++j) {
				for (int dim = 0; dim < (numDims); dim++) {
					int prefix = commonPrefixLengths[dim];
					in.readBytes(scratchPackedValue, ((dim * (bytesPerDim)) + prefix), ((bytesPerDim) - prefix));
				}
				visitor.visit(docIDs[(i + j)], scratchPackedValue);
			}
			i += runLen;
		}
		if (i != count) {
			throw new CorruptIndexException(((("Sub blocks do not add up to the expected count: " + count) + " != ") + i), in);
		}
	}

	private int readCompressedDim(IndexInput in) throws IOException {
		int compressedDim = in.readByte();
		if ((compressedDim < (-1)) || (compressedDim >= (numDims))) {
			throw new CorruptIndexException(("Got compressedDim=" + compressedDim), in);
		}
		return compressedDim;
	}

	private void readCommonPrefixes(int[] commonPrefixLengths, byte[] scratchPackedValue, IndexInput in) throws IOException {
		for (int dim = 0; dim < (numDims); dim++) {
			int prefix = in.readVInt();
			commonPrefixLengths[dim] = prefix;
			if (prefix > 0) {
				in.readBytes(scratchPackedValue, (dim * (bytesPerDim)), prefix);
			}
		}
	}

	private void intersect(BKDReader.IntersectState state, byte[] cellMinPacked, byte[] cellMaxPacked) throws IOException {
		PointValues.Relation r = state.visitor.compare(cellMinPacked, cellMaxPacked);
		if (r == (CELL_OUTSIDE_QUERY)) {
		}else
			if (r == (CELL_INSIDE_QUERY)) {
				addAll(state, false);
			}else
				if (state.index.isLeafNode()) {
					if (state.index.nodeExists()) {
						int count = readDocIDs(state.in, state.index.getLeafBlockFP(), state.scratchDocIDs);
						visitDocValues(state.commonPrefixLengths, state.scratchPackedValue, state.in, state.scratchDocIDs, count, state.visitor);
					}
				}else {
					int splitDim = state.index.getSplitDim();
					assert splitDim >= 0 : "splitDim=" + splitDim;
					assert splitDim < (numDims);
					byte[] splitPackedValue = state.index.getSplitPackedValue();
					BytesRef splitDimValue = state.index.getSplitDimValue();
					assert (splitDimValue.length) == (bytesPerDim);
					assert (StringHelper.compare(bytesPerDim, cellMinPacked, (splitDim * (bytesPerDim)), splitDimValue.bytes, splitDimValue.offset)) <= 0 : (((("bytesPerDim=" + (bytesPerDim)) + " splitDim=") + splitDim) + " numDims=") + (numDims);
					assert (StringHelper.compare(bytesPerDim, cellMaxPacked, (splitDim * (bytesPerDim)), splitDimValue.bytes, splitDimValue.offset)) >= 0 : (((("bytesPerDim=" + (bytesPerDim)) + " splitDim=") + splitDim) + " numDims=") + (numDims);
					System.arraycopy(cellMaxPacked, 0, splitPackedValue, 0, packedBytesLength);
					System.arraycopy(splitDimValue.bytes, splitDimValue.offset, splitPackedValue, (splitDim * (bytesPerDim)), bytesPerDim);
					state.index.pushLeft();
					intersect(state, cellMinPacked, splitPackedValue);
					state.index.pop();
					System.arraycopy(splitPackedValue, (splitDim * (bytesPerDim)), splitDimValue.bytes, splitDimValue.offset, bytesPerDim);
					System.arraycopy(cellMinPacked, 0, splitPackedValue, 0, packedBytesLength);
					System.arraycopy(splitDimValue.bytes, splitDimValue.offset, splitPackedValue, (splitDim * (bytesPerDim)), bytesPerDim);
					state.index.pushRight();
					intersect(state, splitPackedValue, cellMaxPacked);
					state.index.pop();
				}


	}

	private long estimatePointCount(BKDReader.IntersectState state, byte[] cellMinPacked, byte[] cellMaxPacked) {
		PointValues.Relation r = state.visitor.compare(cellMinPacked, cellMaxPacked);
		if (r == (CELL_OUTSIDE_QUERY)) {
			return 0L;
		}else
			if (r == (CELL_INSIDE_QUERY)) {
				return ((long) (maxPointsInLeafNode)) * (state.index.getNumLeaves());
			}else
				if (state.index.isLeafNode()) {
					return ((maxPointsInLeafNode) + 1) / 2;
				}else {
					int splitDim = state.index.getSplitDim();
					assert splitDim >= 0 : "splitDim=" + splitDim;
					assert splitDim < (numDims);
					byte[] splitPackedValue = state.index.getSplitPackedValue();
					BytesRef splitDimValue = state.index.getSplitDimValue();
					assert (splitDimValue.length) == (bytesPerDim);
					assert (StringHelper.compare(bytesPerDim, cellMinPacked, (splitDim * (bytesPerDim)), splitDimValue.bytes, splitDimValue.offset)) <= 0 : (((("bytesPerDim=" + (bytesPerDim)) + " splitDim=") + splitDim) + " numDims=") + (numDims);
					assert (StringHelper.compare(bytesPerDim, cellMaxPacked, (splitDim * (bytesPerDim)), splitDimValue.bytes, splitDimValue.offset)) >= 0 : (((("bytesPerDim=" + (bytesPerDim)) + " splitDim=") + splitDim) + " numDims=") + (numDims);
					System.arraycopy(cellMaxPacked, 0, splitPackedValue, 0, packedBytesLength);
					System.arraycopy(splitDimValue.bytes, splitDimValue.offset, splitPackedValue, (splitDim * (bytesPerDim)), bytesPerDim);
					state.index.pushLeft();
					final long leftCost = estimatePointCount(state, cellMinPacked, splitPackedValue);
					state.index.pop();
					System.arraycopy(splitPackedValue, (splitDim * (bytesPerDim)), splitDimValue.bytes, splitDimValue.offset, bytesPerDim);
					System.arraycopy(cellMinPacked, 0, splitPackedValue, 0, packedBytesLength);
					System.arraycopy(splitDimValue.bytes, splitDimValue.offset, splitPackedValue, (splitDim * (bytesPerDim)), bytesPerDim);
					state.index.pushRight();
					final long rightCost = estimatePointCount(state, splitPackedValue, cellMaxPacked);
					state.index.pop();
					return leftCost + rightCost;
				}


	}

	@Override
	public long ramBytesUsed() {
		if ((packedIndex) != null) {
			return packedIndex.length;
		}else {
			return (RamUsageEstimator.sizeOf(splitPackedValues)) + (RamUsageEstimator.sizeOf(leafBlockFPs));
		}
	}

	@Override
	public byte[] getMinPackedValue() {
		return minPackedValue.clone();
	}

	@Override
	public byte[] getMaxPackedValue() {
		return maxPackedValue.clone();
	}

	@Override
	public int getNumDimensions() {
		return numDims;
	}

	@Override
	public int getBytesPerDimension() {
		return bytesPerDim;
	}

	@Override
	public long size() {
		return pointCount;
	}

	@Override
	public int getDocCount() {
		return docCount;
	}

	public boolean isLeafNode(int nodeID) {
		return nodeID >= (leafNodeOffset);
	}
}

