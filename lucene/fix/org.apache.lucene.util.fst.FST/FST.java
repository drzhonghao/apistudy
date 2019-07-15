

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.store.ByteArrayDataOutput;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.InputStreamDataInput;
import org.apache.lucene.store.OutputStreamDataOutput;
import org.apache.lucene.store.RAMOutputStream;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.Constants;
import org.apache.lucene.util.RamUsageEstimator;
import org.apache.lucene.util.fst.Builder;
import org.apache.lucene.util.fst.Outputs;


public final class FST<T> implements Accountable {
	private static final long BASE_RAM_BYTES_USED = RamUsageEstimator.shallowSizeOfInstance(FST.class);

	private static final long ARC_SHALLOW_RAM_BYTES_USED = RamUsageEstimator.shallowSizeOfInstance(FST.Arc.class);

	public static enum INPUT_TYPE {

		BYTE1,
		BYTE2,
		BYTE4;}

	static final int BIT_FINAL_ARC = 1 << 0;

	static final int BIT_LAST_ARC = 1 << 1;

	static final int BIT_TARGET_NEXT = 1 << 2;

	static final int BIT_STOP_NODE = 1 << 3;

	public static final int BIT_ARC_HAS_OUTPUT = 1 << 4;

	static final int BIT_ARC_HAS_FINAL_OUTPUT = 1 << 5;

	private static final byte ARCS_AS_FIXED_ARRAY = FST.BIT_ARC_HAS_FINAL_OUTPUT;

	static final int FIXED_ARRAY_SHALLOW_DISTANCE = 3;

	static final int FIXED_ARRAY_NUM_ARCS_SHALLOW = 5;

	static final int FIXED_ARRAY_NUM_ARCS_DEEP = 10;

	private static final String FILE_FORMAT_NAME = "FST";

	private static final int VERSION_START = 0;

	private static final int VERSION_INT_NUM_BYTES_PER_ARC = 1;

	private static final int VERSION_SHORT_BYTE2_LABELS = 2;

	private static final int VERSION_PACKED = 3;

	private static final int VERSION_VINT_TARGET = 4;

	private static final int VERSION_NO_NODE_ARC_COUNTS = 5;

	private static final int VERSION_PACKED_REMOVED = 6;

	private static final int VERSION_CURRENT = FST.VERSION_PACKED_REMOVED;

	private static final long FINAL_END_NODE = -1;

	private static final long NON_FINAL_END_NODE = 0;

	public static final int END_LABEL = -1;

	public final FST.INPUT_TYPE inputType;

	T emptyOutput;

	final byte[] bytesArray;

	private long startNode = -1;

	public final Outputs<T> outputs;

	private FST.Arc<T>[] cachedRootArcs;

	public static final class Arc<T> {
		public int label;

		public T output;

		public long target;

		byte flags;

		public T nextFinalOutput;

		long nextArc;

		public long posArcsStart;

		public int bytesPerArc;

		public int arcIdx;

		public int numArcs;

		public FST.Arc<T> copyFrom(FST.Arc<T> other) {
			label = other.label;
			target = other.target;
			flags = other.flags;
			output = other.output;
			nextFinalOutput = other.nextFinalOutput;
			nextArc = other.nextArc;
			bytesPerArc = other.bytesPerArc;
			if ((bytesPerArc) != 0) {
				posArcsStart = other.posArcsStart;
				arcIdx = other.arcIdx;
				numArcs = other.numArcs;
			}
			return this;
		}

		boolean flag(int flag) {
			return FST.flag(flags, flag);
		}

		public boolean isLast() {
			return flag(FST.BIT_LAST_ARC);
		}

		public boolean isFinal() {
			return flag(FST.BIT_FINAL_ARC);
		}

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			b.append((" target=" + (target)));
			b.append((" label=0x" + (Integer.toHexString(label))));
			if (flag(FST.BIT_FINAL_ARC)) {
				b.append(" final");
			}
			if (flag(FST.BIT_LAST_ARC)) {
				b.append(" last");
			}
			if (flag(FST.BIT_TARGET_NEXT)) {
				b.append(" targetNext");
			}
			if (flag(FST.BIT_STOP_NODE)) {
				b.append(" stop");
			}
			if (flag(FST.BIT_ARC_HAS_OUTPUT)) {
				b.append((" output=" + (output)));
			}
			if (flag(FST.BIT_ARC_HAS_FINAL_OUTPUT)) {
				b.append((" nextFinalOutput=" + (nextFinalOutput)));
			}
			if ((bytesPerArc) != 0) {
				b.append(((((" arcArray(idx=" + (arcIdx)) + " of ") + (numArcs)) + ")"));
			}
			return b.toString();
		}
	}

	private static boolean flag(int flags, int bit) {
		return (flags & bit) != 0;
	}

	private final int version;

	FST(FST.INPUT_TYPE inputType, Outputs<T> outputs, int bytesPageBits) {
		this.inputType = inputType;
		this.outputs = outputs;
		version = FST.VERSION_CURRENT;
		bytesArray = null;
		emptyOutput = null;
	}

	public static final int DEFAULT_MAX_BLOCK_BITS = (Constants.JRE_IS_64BIT) ? 30 : 28;

	public FST(DataInput in, Outputs<T> outputs) throws IOException {
		this(in, outputs, FST.DEFAULT_MAX_BLOCK_BITS);
	}

	public FST(DataInput in, Outputs<T> outputs, int maxBlockBits) throws IOException {
		this.outputs = outputs;
		if ((maxBlockBits < 1) || (maxBlockBits > 30)) {
			throw new IllegalArgumentException(("maxBlockBits should be 1 .. 30; got " + maxBlockBits));
		}
		version = CodecUtil.checkHeader(in, FST.FILE_FORMAT_NAME, FST.VERSION_PACKED, FST.VERSION_CURRENT);
		if ((version) < (FST.VERSION_PACKED_REMOVED)) {
			if ((in.readByte()) == 1) {
				throw new CorruptIndexException("Cannot read packed FSTs anymore", in);
			}
		}
		if ((in.readByte()) == 1) {
			int numBytes = in.readVInt();
			if (numBytes > 0) {
			}
		}else {
			emptyOutput = null;
		}
		final byte t = in.readByte();
		switch (t) {
			case 0 :
				inputType = FST.INPUT_TYPE.BYTE1;
				break;
			case 1 :
				inputType = FST.INPUT_TYPE.BYTE2;
				break;
			case 2 :
				inputType = FST.INPUT_TYPE.BYTE4;
				break;
			default :
				throw new IllegalStateException(("invalid input type " + t));
		}
		startNode = in.readVLong();
		if ((version) < (FST.VERSION_NO_NODE_ARC_COUNTS)) {
			in.readVLong();
			in.readVLong();
			in.readVLong();
		}
		long numBytes = in.readVLong();
		if (numBytes > (1 << maxBlockBits)) {
			bytesArray = null;
		}else {
			bytesArray = new byte[((int) (numBytes))];
			in.readBytes(bytesArray, 0, bytesArray.length);
		}
		cacheRootArcs();
	}

	public FST.INPUT_TYPE getInputType() {
		return inputType;
	}

	private long ramBytesUsed(FST.Arc<T>[] arcs) {
		long size = 0;
		if (arcs != null) {
			size += RamUsageEstimator.shallowSizeOf(arcs);
			for (FST.Arc<T> arc : arcs) {
				if (arc != null) {
					size += FST.ARC_SHALLOW_RAM_BYTES_USED;
					if (((arc.output) != null) && ((arc.output) != (outputs.getNoOutput()))) {
						size += outputs.ramBytesUsed(arc.output);
					}
					if (((arc.nextFinalOutput) != null) && ((arc.nextFinalOutput) != (outputs.getNoOutput()))) {
						size += outputs.ramBytesUsed(arc.nextFinalOutput);
					}
				}
			}
		}
		return size;
	}

	private int cachedArcsBytesUsed;

	@Override
	public long ramBytesUsed() {
		long size = FST.BASE_RAM_BYTES_USED;
		if ((bytesArray) != null) {
			size += bytesArray.length;
		}else {
		}
		size += cachedArcsBytesUsed;
		return size;
	}

	@Override
	public String toString() {
		return ((((getClass().getSimpleName()) + "(input=") + (inputType)) + ",output=") + (outputs);
	}

	void finish(long newStartNode) throws IOException {
		if ((startNode) != (-1)) {
			throw new IllegalStateException("already finished");
		}
		if ((newStartNode == (FST.FINAL_END_NODE)) && ((emptyOutput) != null)) {
			newStartNode = 0;
		}
		startNode = newStartNode;
		cacheRootArcs();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void cacheRootArcs() throws IOException {
		assert (cachedArcsBytesUsed) == 0;
		final FST.Arc<T> arc = new FST.Arc<>();
		getFirstArc(arc);
		if (FST.targetHasArcs(arc)) {
			final FST.BytesReader in = getBytesReader();
			FST.Arc<T>[] arcs = ((FST.Arc<T>[]) (new FST.Arc[128]));
			readFirstRealTargetArc(arc.target, arc, in);
			int count = 0;
			while (true) {
				assert (arc.label) != (FST.END_LABEL);
				if ((arc.label) < (arcs.length)) {
					arcs[arc.label] = new FST.Arc<T>().copyFrom(arc);
				}else {
					break;
				}
				if (arc.isLast()) {
					break;
				}
				readNextRealArc(arc, in);
				count++;
			} 
			int cacheRAM = ((int) (ramBytesUsed(arcs)));
			if ((count >= (FST.FIXED_ARRAY_NUM_ARCS_SHALLOW)) && (cacheRAM < ((ramBytesUsed()) / 5))) {
				cachedRootArcs = arcs;
				cachedArcsBytesUsed = cacheRAM;
			}
		}
	}

	public T getEmptyOutput() {
		return emptyOutput;
	}

	void setEmptyOutput(T v) throws IOException {
		if ((emptyOutput) != null) {
			emptyOutput = outputs.merge(emptyOutput, v);
		}else {
			emptyOutput = v;
		}
	}

	public void save(DataOutput out) throws IOException {
		if ((startNode) == (-1)) {
			throw new IllegalStateException("call finish first");
		}
		CodecUtil.writeHeader(out, FST.FILE_FORMAT_NAME, FST.VERSION_CURRENT);
		if ((emptyOutput) != null) {
			out.writeByte(((byte) (1)));
			RAMOutputStream ros = new RAMOutputStream();
			outputs.writeFinalOutput(emptyOutput, ros);
			byte[] emptyOutputBytes = new byte[((int) (ros.getFilePointer()))];
			ros.writeTo(emptyOutputBytes, 0);
			final int stopAt = (emptyOutputBytes.length) / 2;
			int upto = 0;
			while (upto < stopAt) {
				final byte b = emptyOutputBytes[upto];
				emptyOutputBytes[upto] = emptyOutputBytes[(((emptyOutputBytes.length) - upto) - 1)];
				emptyOutputBytes[(((emptyOutputBytes.length) - upto) - 1)] = b;
				upto++;
			} 
			out.writeVInt(emptyOutputBytes.length);
			out.writeBytes(emptyOutputBytes, 0, emptyOutputBytes.length);
		}else {
			out.writeByte(((byte) (0)));
		}
		final byte t;
		if ((inputType) == (FST.INPUT_TYPE.BYTE1)) {
			t = 0;
		}else
			if ((inputType) == (FST.INPUT_TYPE.BYTE2)) {
				t = 1;
			}else {
				t = 2;
			}

		out.writeByte(t);
		out.writeVLong(startNode);
	}

	public void save(final Path path) throws IOException {
		try (final OutputStream os = new BufferedOutputStream(Files.newOutputStream(path))) {
			save(new OutputStreamDataOutput(os));
		}
	}

	public static <T> FST<T> read(Path path, Outputs<T> outputs) throws IOException {
		try (final InputStream is = Files.newInputStream(path)) {
			return new FST<>(new InputStreamDataInput(new BufferedInputStream(is)), outputs);
		}
	}

	private void writeLabel(DataOutput out, int v) throws IOException {
		assert v >= 0 : "v=" + v;
		if ((inputType) == (FST.INPUT_TYPE.BYTE1)) {
			assert v <= 255 : "v=" + v;
			out.writeByte(((byte) (v)));
		}else
			if ((inputType) == (FST.INPUT_TYPE.BYTE2)) {
				assert v <= 65535 : "v=" + v;
				out.writeShort(((short) (v)));
			}else {
				out.writeVInt(v);
			}

	}

	public int readLabel(DataInput in) throws IOException {
		final int v;
		if ((inputType) == (FST.INPUT_TYPE.BYTE1)) {
			v = (in.readByte()) & 255;
		}else
			if ((inputType) == (FST.INPUT_TYPE.BYTE2)) {
				v = (in.readShort()) & 65535;
			}else {
				v = in.readVInt();
			}

		return v;
	}

	public static <T> boolean targetHasArcs(FST.Arc<T> arc) {
		return (arc.target) > 0;
	}

	long addNode(Builder<T> builder, Builder.UnCompiledNode<T> nodeIn) throws IOException {
		T NO_OUTPUT = outputs.getNoOutput();
		if ((nodeIn.numArcs) == 0) {
			if (nodeIn.isFinal) {
				return FST.FINAL_END_NODE;
			}else {
				return FST.NON_FINAL_END_NODE;
			}
		}
		final boolean doFixedArray = shouldExpand(builder, nodeIn);
		if (doFixedArray) {
		}
		final int lastArc = (nodeIn.numArcs) - 1;
		int maxBytesPerArc = 0;
		for (int arcIdx = 0; arcIdx < (nodeIn.numArcs); arcIdx++) {
			final Builder.Arc<T> arc = nodeIn.arcs[arcIdx];
			int flags = 0;
			if (arcIdx == lastArc) {
				flags += FST.BIT_LAST_ARC;
			}
			if (arc.isFinal) {
				flags += FST.BIT_FINAL_ARC;
				if ((arc.nextFinalOutput) != NO_OUTPUT) {
					flags += FST.BIT_ARC_HAS_FINAL_OUTPUT;
				}
			}else {
				assert (arc.nextFinalOutput) == NO_OUTPUT;
			}
			if ((arc.output) != NO_OUTPUT) {
				flags += FST.BIT_ARC_HAS_OUTPUT;
			}
			if ((arc.output) != NO_OUTPUT) {
			}
			if ((arc.nextFinalOutput) != NO_OUTPUT) {
			}
			if (doFixedArray) {
			}
		}
		if (doFixedArray) {
			final int MAX_HEADER_SIZE = 11;
			assert maxBytesPerArc > 0;
			byte[] header = new byte[MAX_HEADER_SIZE];
			ByteArrayDataOutput bad = new ByteArrayDataOutput(header);
			bad.writeByte(FST.ARCS_AS_FIXED_ARRAY);
			bad.writeVInt(nodeIn.numArcs);
			bad.writeVInt(maxBytesPerArc);
			int headerLen = bad.getPosition();
		}
		return 0l;
	}

	public FST.Arc<T> getFirstArc(FST.Arc<T> arc) {
		T NO_OUTPUT = outputs.getNoOutput();
		if ((emptyOutput) != null) {
			arc.flags = (FST.BIT_FINAL_ARC) | (FST.BIT_LAST_ARC);
			arc.nextFinalOutput = emptyOutput;
			if ((emptyOutput) != NO_OUTPUT) {
				arc.flags |= FST.BIT_ARC_HAS_FINAL_OUTPUT;
			}
		}else {
			arc.flags = FST.BIT_LAST_ARC;
			arc.nextFinalOutput = NO_OUTPUT;
		}
		arc.output = NO_OUTPUT;
		arc.target = startNode;
		return arc;
	}

	public FST.Arc<T> readLastTargetArc(FST.Arc<T> follow, FST.Arc<T> arc, FST.BytesReader in) throws IOException {
		if (!(FST.targetHasArcs(follow))) {
			assert follow.isFinal();
			arc.label = FST.END_LABEL;
			arc.target = FST.FINAL_END_NODE;
			arc.output = follow.nextFinalOutput;
			arc.flags = FST.BIT_LAST_ARC;
			return arc;
		}else {
			in.setPosition(follow.target);
			final byte b = in.readByte();
			if (b == (FST.ARCS_AS_FIXED_ARRAY)) {
				arc.numArcs = in.readVInt();
				if ((version) >= (FST.VERSION_VINT_TARGET)) {
					arc.bytesPerArc = in.readVInt();
				}else {
					arc.bytesPerArc = in.readInt();
				}
				arc.posArcsStart = in.getPosition();
				arc.arcIdx = (arc.numArcs) - 2;
			}else {
				arc.flags = b;
				arc.bytesPerArc = 0;
				while (!(arc.isLast())) {
					readLabel(in);
					if (arc.flag(FST.BIT_ARC_HAS_OUTPUT)) {
						outputs.skipOutput(in);
					}
					if (arc.flag(FST.BIT_ARC_HAS_FINAL_OUTPUT)) {
						outputs.skipFinalOutput(in);
					}
					if (arc.flag(FST.BIT_STOP_NODE)) {
					}else
						if (arc.flag(FST.BIT_TARGET_NEXT)) {
						}else {
							readUnpackedNodeTarget(in);
						}

					arc.flags = in.readByte();
				} 
				in.skipBytes((-1));
				arc.nextArc = in.getPosition();
			}
			readNextRealArc(arc, in);
			assert arc.isLast();
			return arc;
		}
	}

	private long readUnpackedNodeTarget(FST.BytesReader in) throws IOException {
		long target;
		if ((version) < (FST.VERSION_VINT_TARGET)) {
			target = in.readInt();
		}else {
			target = in.readVLong();
		}
		return target;
	}

	public FST.Arc<T> readFirstTargetArc(FST.Arc<T> follow, FST.Arc<T> arc, FST.BytesReader in) throws IOException {
		if (follow.isFinal()) {
			arc.label = FST.END_LABEL;
			arc.output = follow.nextFinalOutput;
			arc.flags = FST.BIT_FINAL_ARC;
			if ((follow.target) <= 0) {
				arc.flags |= FST.BIT_LAST_ARC;
			}else {
				arc.nextArc = follow.target;
			}
			arc.target = FST.FINAL_END_NODE;
			return arc;
		}else {
			return readFirstRealTargetArc(follow.target, arc, in);
		}
	}

	public FST.Arc<T> readFirstRealTargetArc(long node, FST.Arc<T> arc, final FST.BytesReader in) throws IOException {
		final long address = node;
		in.setPosition(address);
		if ((in.readByte()) == (FST.ARCS_AS_FIXED_ARRAY)) {
			arc.numArcs = in.readVInt();
			if ((version) >= (FST.VERSION_VINT_TARGET)) {
				arc.bytesPerArc = in.readVInt();
			}else {
				arc.bytesPerArc = in.readInt();
			}
			arc.arcIdx = -1;
			arc.nextArc = arc.posArcsStart = in.getPosition();
		}else {
			arc.nextArc = address;
			arc.bytesPerArc = 0;
		}
		return readNextRealArc(arc, in);
	}

	boolean isExpandedTarget(FST.Arc<T> follow, FST.BytesReader in) throws IOException {
		if (!(FST.targetHasArcs(follow))) {
			return false;
		}else {
			in.setPosition(follow.target);
			return (in.readByte()) == (FST.ARCS_AS_FIXED_ARRAY);
		}
	}

	public FST.Arc<T> readNextArc(FST.Arc<T> arc, FST.BytesReader in) throws IOException {
		if ((arc.label) == (FST.END_LABEL)) {
			if ((arc.nextArc) <= 0) {
				throw new IllegalArgumentException("cannot readNextArc when arc.isLast()=true");
			}
			return readFirstRealTargetArc(arc.nextArc, arc, in);
		}else {
			return readNextRealArc(arc, in);
		}
	}

	public int readNextArcLabel(FST.Arc<T> arc, FST.BytesReader in) throws IOException {
		assert !(arc.isLast());
		if ((arc.label) == (FST.END_LABEL)) {
			long pos = arc.nextArc;
			in.setPosition(pos);
			final byte b = in.readByte();
			if (b == (FST.ARCS_AS_FIXED_ARRAY)) {
				in.readVInt();
				if ((version) >= (FST.VERSION_VINT_TARGET)) {
					in.readVInt();
				}else {
					in.readInt();
				}
			}else {
				in.setPosition(pos);
			}
		}else {
			if ((arc.bytesPerArc) != 0) {
				in.setPosition(arc.posArcsStart);
				in.skipBytes(((1 + (arc.arcIdx)) * (arc.bytesPerArc)));
			}else {
				in.setPosition(arc.nextArc);
			}
		}
		in.readByte();
		return readLabel(in);
	}

	public FST.Arc<T> readNextRealArc(FST.Arc<T> arc, final FST.BytesReader in) throws IOException {
		if ((arc.bytesPerArc) != 0) {
			(arc.arcIdx)++;
			assert (arc.arcIdx) < (arc.numArcs);
			in.setPosition(arc.posArcsStart);
			in.skipBytes(((arc.arcIdx) * (arc.bytesPerArc)));
		}else {
			in.setPosition(arc.nextArc);
		}
		arc.flags = in.readByte();
		arc.label = readLabel(in);
		if (arc.flag(FST.BIT_ARC_HAS_OUTPUT)) {
			arc.output = outputs.read(in);
		}else {
			arc.output = outputs.getNoOutput();
		}
		if (arc.flag(FST.BIT_ARC_HAS_FINAL_OUTPUT)) {
			arc.nextFinalOutput = outputs.readFinalOutput(in);
		}else {
			arc.nextFinalOutput = outputs.getNoOutput();
		}
		if (arc.flag(FST.BIT_STOP_NODE)) {
			if (arc.flag(FST.BIT_FINAL_ARC)) {
				arc.target = FST.FINAL_END_NODE;
			}else {
				arc.target = FST.NON_FINAL_END_NODE;
			}
			arc.nextArc = in.getPosition();
		}else
			if (arc.flag(FST.BIT_TARGET_NEXT)) {
				arc.nextArc = in.getPosition();
				if (!(arc.flag(FST.BIT_LAST_ARC))) {
					if ((arc.bytesPerArc) == 0) {
						seekToNextNode(in);
					}else {
						in.setPosition(arc.posArcsStart);
						in.skipBytes(((arc.bytesPerArc) * (arc.numArcs)));
					}
				}
				arc.target = in.getPosition();
			}else {
				arc.target = readUnpackedNodeTarget(in);
				arc.nextArc = in.getPosition();
			}

		return arc;
	}

	private boolean assertRootCachedArc(int label, FST.Arc<T> cachedArc) throws IOException {
		FST.Arc<T> arc = new FST.Arc<>();
		getFirstArc(arc);
		FST.BytesReader in = getBytesReader();
		FST.Arc<T> result = findTargetArc(label, arc, arc, in, false);
		if (result == null) {
			assert cachedArc == null;
		}else {
			assert cachedArc != null;
			assert (cachedArc.arcIdx) == (result.arcIdx);
			assert (cachedArc.bytesPerArc) == (result.bytesPerArc);
			assert (cachedArc.flags) == (result.flags);
			assert (cachedArc.label) == (result.label);
			assert (cachedArc.nextArc) == (result.nextArc);
			assert cachedArc.nextFinalOutput.equals(result.nextFinalOutput);
			assert (cachedArc.numArcs) == (result.numArcs);
			assert cachedArc.output.equals(result.output);
			assert (cachedArc.posArcsStart) == (result.posArcsStart);
			assert (cachedArc.target) == (result.target);
		}
		return true;
	}

	public FST.Arc<T> findTargetArc(int labelToMatch, FST.Arc<T> follow, FST.Arc<T> arc, FST.BytesReader in) throws IOException {
		return findTargetArc(labelToMatch, follow, arc, in, true);
	}

	private FST.Arc<T> findTargetArc(int labelToMatch, FST.Arc<T> follow, FST.Arc<T> arc, FST.BytesReader in, boolean useRootArcCache) throws IOException {
		if (labelToMatch == (FST.END_LABEL)) {
			if (follow.isFinal()) {
				if ((follow.target) <= 0) {
					arc.flags = FST.BIT_LAST_ARC;
				}else {
					arc.flags = 0;
					arc.nextArc = follow.target;
				}
				arc.output = follow.nextFinalOutput;
				arc.label = FST.END_LABEL;
				return arc;
			}else {
				return null;
			}
		}
		if (((useRootArcCache && ((cachedRootArcs) != null)) && ((follow.target) == (startNode))) && (labelToMatch < (cachedRootArcs.length))) {
			final FST.Arc<T> result = cachedRootArcs[labelToMatch];
			assert assertRootCachedArc(labelToMatch, result);
			if (result == null) {
				return null;
			}else {
				arc.copyFrom(result);
				return arc;
			}
		}
		if (!(FST.targetHasArcs(follow))) {
			return null;
		}
		in.setPosition(follow.target);
		if ((in.readByte()) == (FST.ARCS_AS_FIXED_ARRAY)) {
			arc.numArcs = in.readVInt();
			if ((version) >= (FST.VERSION_VINT_TARGET)) {
				arc.bytesPerArc = in.readVInt();
			}else {
				arc.bytesPerArc = in.readInt();
			}
			arc.posArcsStart = in.getPosition();
			int low = 0;
			int high = (arc.numArcs) - 1;
			while (low <= high) {
				int mid = (low + high) >>> 1;
				in.setPosition(arc.posArcsStart);
				in.skipBytes((((arc.bytesPerArc) * mid) + 1));
				int midLabel = readLabel(in);
				final int cmp = midLabel - labelToMatch;
				if (cmp < 0) {
					low = mid + 1;
				}else
					if (cmp > 0) {
						high = mid - 1;
					}else {
						arc.arcIdx = mid - 1;
						return readNextRealArc(arc, in);
					}

			} 
			return null;
		}
		readFirstRealTargetArc(follow.target, arc, in);
		while (true) {
			if ((arc.label) == labelToMatch) {
				return arc;
			}else
				if ((arc.label) > labelToMatch) {
					return null;
				}else
					if (arc.isLast()) {
						return null;
					}else {
						readNextRealArc(arc, in);
					}


		} 
	}

	private void seekToNextNode(FST.BytesReader in) throws IOException {
		while (true) {
			final int flags = in.readByte();
			readLabel(in);
			if (FST.flag(flags, FST.BIT_ARC_HAS_OUTPUT)) {
				outputs.skipOutput(in);
			}
			if (FST.flag(flags, FST.BIT_ARC_HAS_FINAL_OUTPUT)) {
				outputs.skipFinalOutput(in);
			}
			if ((!(FST.flag(flags, FST.BIT_STOP_NODE))) && (!(FST.flag(flags, FST.BIT_TARGET_NEXT)))) {
				readUnpackedNodeTarget(in);
			}
			if (FST.flag(flags, FST.BIT_LAST_ARC)) {
				return;
			}
		} 
	}

	private boolean shouldExpand(Builder<T> builder, Builder.UnCompiledNode<T> node) {
		return false;
	}

	public FST.BytesReader getBytesReader() {
		if ((bytesArray) != null) {
		}else {
		}
		return null;
	}

	public static abstract class BytesReader extends DataInput {
		public abstract long getPosition();

		public abstract void setPosition(long pos);

		public abstract boolean reversed();
	}
}

