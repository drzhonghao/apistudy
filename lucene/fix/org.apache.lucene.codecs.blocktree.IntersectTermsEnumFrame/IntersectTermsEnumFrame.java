

import java.io.IOException;
import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.Transition;
import org.apache.lucene.util.fst.FST;


final class IntersectTermsEnumFrame {
	final int ord = 0;

	long fp;

	long fpOrig;

	long fpEnd;

	long lastSubFP;

	int state;

	int lastState;

	int metaDataUpto;

	byte[] suffixBytes = new byte[128];

	final ByteArrayDataInput suffixesReader = new ByteArrayDataInput();

	byte[] statBytes = new byte[64];

	final ByteArrayDataInput statsReader = new ByteArrayDataInput();

	byte[] floorData = new byte[32];

	final ByteArrayDataInput floorDataReader = new ByteArrayDataInput();

	int prefix;

	int entCount;

	int nextEnt;

	boolean isLastInFloor;

	boolean isLeafBlock;

	int numFollowFloorBlocks;

	int nextFloorLabel;

	final Transition transition = new Transition();

	int transitionIndex;

	int transitionCount;

	FST.Arc<BytesRef> arc;

	final BlockTermState termState = null;

	final long[] longs = null;

	byte[] bytes = new byte[32];

	final ByteArrayDataInput bytesReader = new ByteArrayDataInput();

	BytesRef outputPrefix;

	int startBytePos;

	int suffix;

	int floorSuffixLeadStart;

	int floorSuffixLeadEnd;

	boolean isAutoPrefixTerm;

	void loadNextFloorBlock() throws IOException {
		assert (numFollowFloorBlocks) > 0 : "nextFloorLabel=" + (nextFloorLabel);
		do {
			fp = (fpOrig) + ((floorDataReader.readVLong()) >>> 1);
			(numFollowFloorBlocks)--;
			if ((numFollowFloorBlocks) != 0) {
				nextFloorLabel = (floorDataReader.readByte()) & 255;
			}else {
				nextFloorLabel = 256;
			}
		} while (((numFollowFloorBlocks) != 0) && ((nextFloorLabel) <= (transition.min)) );
	}

	public void setState(int state) {
		this.state = state;
		transitionIndex = 0;
		if ((transitionCount) != 0) {
		}else {
			transition.min = -1;
			transition.max = -1;
		}
	}

	public boolean next() {
		if (isLeafBlock) {
			nextLeaf();
			return false;
		}else {
			return nextNonLeaf();
		}
	}

	public void nextLeaf() {
		assert ((nextEnt) != (-1)) && ((nextEnt) < (entCount)) : (((("nextEnt=" + (nextEnt)) + " entCount=") + (entCount)) + " fp=") + (fp);
		(nextEnt)++;
		suffix = suffixesReader.readVInt();
		startBytePos = suffixesReader.getPosition();
		suffixesReader.skipBytes(suffix);
	}

	public boolean nextNonLeaf() {
		assert ((nextEnt) != (-1)) && ((nextEnt) < (entCount)) : (((("nextEnt=" + (nextEnt)) + " entCount=") + (entCount)) + " fp=") + (fp);
		(nextEnt)++;
		final int code = suffixesReader.readVInt();
		suffix = code >>> 1;
		startBytePos = suffixesReader.getPosition();
		suffixesReader.skipBytes(suffix);
		if ((code & 1) == 0) {
			(termState.termBlockOrd)++;
			return false;
		}else {
			lastSubFP = (fp) - (suffixesReader.readVLong());
			return true;
		}
	}

	public int getTermBlockOrd() {
		return isLeafBlock ? nextEnt : termState.termBlockOrd;
	}

	public void decodeMetaData() throws IOException {
		final int limit = getTermBlockOrd();
		boolean absolute = (metaDataUpto) == 0;
		assert limit > 0;
		while ((metaDataUpto) < limit) {
			termState.docFreq = statsReader.readVInt();
			(metaDataUpto)++;
			absolute = false;
		} 
		termState.termBlockOrd = metaDataUpto;
	}
}

