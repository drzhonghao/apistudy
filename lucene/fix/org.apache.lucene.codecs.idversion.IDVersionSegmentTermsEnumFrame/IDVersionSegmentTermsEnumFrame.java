

import java.io.IOException;
import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.codecs.idversion.IDVersionSegmentTermsEnum;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.PairOutputs;

import static org.apache.lucene.index.TermsEnum.SeekStatus.END;
import static org.apache.lucene.index.TermsEnum.SeekStatus.FOUND;
import static org.apache.lucene.index.TermsEnum.SeekStatus.NOT_FOUND;


final class IDVersionSegmentTermsEnumFrame {
	final int ord;

	boolean hasTerms;

	boolean hasTermsOrig;

	boolean isFloor;

	long maxIDVersion;

	FST.Arc<PairOutputs.Pair<BytesRef, Long>> arc;

	long fp;

	long fpOrig;

	long fpEnd;

	byte[] suffixBytes = new byte[128];

	final ByteArrayDataInput suffixesReader = new ByteArrayDataInput();

	byte[] floorData = new byte[32];

	final ByteArrayDataInput floorDataReader = new ByteArrayDataInput();

	int prefix;

	int entCount;

	int nextEnt;

	boolean isLastInFloor;

	boolean isLeafBlock;

	long lastSubFP;

	int nextFloorLabel;

	int numFollowFloorBlocks;

	int metaDataUpto;

	BlockTermState state = null;

	public long[] longs;

	public byte[] bytes;

	ByteArrayDataInput bytesReader;

	private final IDVersionSegmentTermsEnum ste;

	public IDVersionSegmentTermsEnumFrame(IDVersionSegmentTermsEnum ste, int ord) throws IOException {
		this.ste = ste;
		this.ord = ord;
		this.state.totalTermFreq = -1;
		state = null;
	}

	public void setFloorData(ByteArrayDataInput in, BytesRef source) {
		final int numBytes = (source.length) - ((in.getPosition()) - (source.offset));
		if (numBytes > (floorData.length)) {
			floorData = new byte[ArrayUtil.oversize(numBytes, 1)];
		}
		System.arraycopy(source.bytes, ((source.offset) + (in.getPosition())), floorData, 0, numBytes);
		floorDataReader.reset(floorData, 0, numBytes);
		numFollowFloorBlocks = floorDataReader.readVInt();
		nextFloorLabel = (floorDataReader.readByte()) & 255;
	}

	public int getTermBlockOrd() {
		return isLeafBlock ? nextEnt : state.termBlockOrd;
	}

	void loadNextFloorBlock() throws IOException {
		assert ((arc) == null) || (isFloor) : (("arc=" + (arc)) + " isFloor=") + (isFloor);
		fp = fpEnd;
		nextEnt = -1;
		loadBlock();
	}

	void loadBlock() throws IOException {
		if ((nextEnt) != (-1)) {
			return;
		}
		assert (entCount) > 0;
		assert ((arc) == null) || ((isLastInFloor) || (isFloor));
		metaDataUpto = 0;
		state.termBlockOrd = 0;
		nextEnt = 0;
		lastSubFP = -1;
		if ((bytes) == null) {
			bytesReader = new ByteArrayDataInput();
		}else {
		}
	}

	void rewind() {
		fp = fpOrig;
		nextEnt = -1;
		hasTerms = hasTermsOrig;
		if (isFloor) {
			floorDataReader.rewind();
			numFollowFloorBlocks = floorDataReader.readVInt();
			nextFloorLabel = (floorDataReader.readByte()) & 255;
		}
	}

	public boolean next() {
		return isLeafBlock ? nextLeaf() : nextNonLeaf();
	}

	public boolean nextLeaf() {
		assert ((nextEnt) != (-1)) && ((nextEnt) < (entCount)) : (((("nextEnt=" + (nextEnt)) + " entCount=") + (entCount)) + " fp=") + (fp);
		(nextEnt)++;
		suffix = suffixesReader.readVInt();
		startBytePos = suffixesReader.getPosition();
		return false;
	}

	public boolean nextNonLeaf() {
		assert ((nextEnt) != (-1)) && ((nextEnt) < (entCount)) : (((("nextEnt=" + (nextEnt)) + " entCount=") + (entCount)) + " fp=") + (fp);
		(nextEnt)++;
		final int code = suffixesReader.readVInt();
		suffix = code >>> 1;
		startBytePos = suffixesReader.getPosition();
		if ((code & 1) == 0) {
			subCode = 0;
			(state.termBlockOrd)++;
			return false;
		}else {
			subCode = suffixesReader.readVLong();
			lastSubFP = (fp) - (subCode);
			return true;
		}
	}

	public void scanToFloorFrame(BytesRef target) {
		if ((!(isFloor)) || ((target.length) <= (prefix))) {
			return;
		}
		final int targetLabel = (target.bytes[((target.offset) + (prefix))]) & 255;
		if (targetLabel < (nextFloorLabel)) {
			return;
		}
		assert (numFollowFloorBlocks) != 0;
		long newFP = fpOrig;
		while (true) {
			final long code = floorDataReader.readVLong();
			newFP = (fpOrig) + (code >>> 1);
			hasTerms = (code & 1) != 0;
			isLastInFloor = (numFollowFloorBlocks) == 1;
			(numFollowFloorBlocks)--;
			if (isLastInFloor) {
				nextFloorLabel = 256;
				break;
			}else {
				nextFloorLabel = (floorDataReader.readByte()) & 255;
				if (targetLabel < (nextFloorLabel)) {
					break;
				}
			}
		} 
		if (newFP != (fp)) {
			nextEnt = -1;
			fp = newFP;
		}else {
		}
	}

	public void decodeMetaData() throws IOException {
		assert (nextEnt) >= 0;
		final int limit = getTermBlockOrd();
		boolean absolute = (metaDataUpto) == 0;
		while ((metaDataUpto) < limit) {
			state.docFreq = 1;
			state.totalTermFreq = 1;
			(metaDataUpto)++;
			absolute = false;
		} 
		state.termBlockOrd = metaDataUpto;
	}

	private boolean prefixMatches(BytesRef target) {
		for (int bytePos = 0; bytePos < (prefix); bytePos++) {
		}
		return true;
	}

	public void scanToSubBlock(long subFP) {
		assert !(isLeafBlock);
		if ((lastSubFP) == subFP) {
			return;
		}
		assert subFP < (fp) : (("fp=" + (fp)) + " subFP=") + subFP;
		final long targetSubCode = (fp) - subFP;
		while (true) {
			assert (nextEnt) < (entCount);
			(nextEnt)++;
			final int code = suffixesReader.readVInt();
			suffixesReader.skipBytes((isLeafBlock ? code : code >>> 1));
			if ((code & 1) != 0) {
				final long subCode = suffixesReader.readVLong();
				if (targetSubCode == subCode) {
					lastSubFP = subFP;
					return;
				}
			}else {
				(state.termBlockOrd)++;
			}
		} 
	}

	public TermsEnum.SeekStatus scanToTerm(BytesRef target, boolean exactOnly) throws IOException {
		return isLeafBlock ? scanToTermLeaf(target, exactOnly) : scanToTermNonLeaf(target, exactOnly);
	}

	private int startBytePos;

	private int suffix;

	private long subCode;

	public TermsEnum.SeekStatus scanToTermLeaf(BytesRef target, boolean exactOnly) throws IOException {
		assert (nextEnt) != (-1);
		subCode = 0;
		if ((nextEnt) == (entCount)) {
			if (exactOnly) {
				fillTerm();
			}
			return END;
		}
		assert prefixMatches(target);
		nextTerm : while (true) {
			(nextEnt)++;
			suffix = suffixesReader.readVInt();
			final int termLen = (prefix) + (suffix);
			startBytePos = suffixesReader.getPosition();
			suffixesReader.skipBytes(suffix);
			final int targetLimit = (target.offset) + ((target.length) < termLen ? target.length : termLen);
			int targetPos = (target.offset) + (prefix);
			int bytePos = startBytePos;
			while (true) {
				final int cmp;
				final boolean stop;
				if (targetPos < targetLimit) {
					cmp = ((suffixBytes[(bytePos++)]) & 255) - ((target.bytes[(targetPos++)]) & 255);
					stop = false;
				}else {
					assert targetPos == targetLimit;
					cmp = termLen - (target.length);
					stop = true;
				}
				if (cmp < 0) {
					if ((nextEnt) == (entCount)) {
						if (exactOnly) {
							fillTerm();
						}
						break nextTerm;
					}else {
						continue nextTerm;
					}
				}else
					if (cmp > 0) {
						fillTerm();
						return NOT_FOUND;
					}else
						if (stop) {
							fillTerm();
							return FOUND;
						}


			} 
		} 
		if (exactOnly) {
			fillTerm();
		}
		return END;
	}

	public TermsEnum.SeekStatus scanToTermNonLeaf(BytesRef target, boolean exactOnly) throws IOException {
		assert (nextEnt) != (-1);
		if ((nextEnt) == (entCount)) {
			if (exactOnly) {
				fillTerm();
			}
			return END;
		}
		assert prefixMatches(target);
		nextTerm : while (true) {
			(nextEnt)++;
			final int code = suffixesReader.readVInt();
			suffix = code >>> 1;
			final int termLen = (prefix) + (suffix);
			startBytePos = suffixesReader.getPosition();
			suffixesReader.skipBytes(suffix);
			final int targetLimit = (target.offset) + ((target.length) < termLen ? target.length : termLen);
			int targetPos = (target.offset) + (prefix);
			int bytePos = startBytePos;
			while (true) {
				final int cmp;
				final boolean stop;
				if (targetPos < targetLimit) {
					cmp = ((suffixBytes[(bytePos++)]) & 255) - ((target.bytes[(targetPos++)]) & 255);
					stop = false;
				}else {
					assert targetPos == targetLimit;
					cmp = termLen - (target.length);
					stop = true;
				}
				if (cmp < 0) {
					if ((nextEnt) == (entCount)) {
						if (exactOnly) {
							fillTerm();
						}
						break nextTerm;
					}else {
						continue nextTerm;
					}
				}else
					if (cmp > 0) {
						fillTerm();
						return NOT_FOUND;
					}else
						if (stop) {
							fillTerm();
							return FOUND;
						}


			} 
		} 
		if (exactOnly) {
			fillTerm();
		}
		return END;
	}

	private void fillTerm() {
		final int termLength = (prefix) + (suffix);
	}
}

