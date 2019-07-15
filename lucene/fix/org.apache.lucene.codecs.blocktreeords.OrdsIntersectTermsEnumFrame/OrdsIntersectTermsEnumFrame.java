

import java.io.IOException;
import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.util.automaton.Transition;


final class OrdsIntersectTermsEnumFrame {
	final int ord = 0;

	long fp;

	long fpOrig;

	long fpEnd;

	long lastSubFP;

	int state;

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

	long termOrdOrig;

	long termOrd;

	boolean isLastInFloor;

	boolean isLeafBlock;

	int numFollowFloorBlocks;

	int nextFloorLabel;

	Transition transition = new Transition();

	int curTransitionMax;

	int transitionIndex;

	int transitionCount;

	final BlockTermState termState = null;

	public long[] longs;

	public byte[] bytes;

	ByteArrayDataInput bytesReader;

	int startBytePos;

	int suffix;

	void loadNextFloorBlock() throws IOException {
		assert (numFollowFloorBlocks) > 0;
		do {
			fp = (fpOrig) + ((floorDataReader.readVLong()) >>> 1);
			(numFollowFloorBlocks)--;
			if ((numFollowFloorBlocks) != 0) {
				nextFloorLabel = (floorDataReader.readByte()) & 255;
				termOrd += floorDataReader.readVLong();
			}else {
				nextFloorLabel = 256;
			}
		} while (((numFollowFloorBlocks) != 0) && ((nextFloorLabel) <= (transition.min)) );
	}

	public boolean next() {
		return isLeafBlock ? nextLeaf() : nextNonLeaf();
	}

	public boolean nextLeaf() {
		assert ((nextEnt) != (-1)) && ((nextEnt) < (entCount)) : (((("nextEnt=" + (nextEnt)) + " entCount=") + (entCount)) + " fp=") + (fp);
		(nextEnt)++;
		suffix = suffixesReader.readVInt();
		startBytePos = suffixesReader.getPosition();
		suffixesReader.skipBytes(suffix);
		return false;
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
			suffixesReader.readVLong();
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

