

import java.io.IOException;
import java.io.OutputStream;
import org.apache.poi.hslf.record.AnimationInfoAtom;
import org.apache.poi.hslf.record.Record;
import org.apache.poi.hslf.record.RecordContainer;
import org.apache.poi.hslf.record.RecordTypes;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogger;


public final class AnimationInfo extends RecordContainer {
	private byte[] _header;

	private AnimationInfoAtom animationAtom;

	protected AnimationInfo(byte[] source, int start, int len) {
		_header = new byte[8];
		System.arraycopy(source, start, _header, 0, 8);
		_children = Record.findChildRecords(source, (start + 8), (len - 8));
		findInterestingChildren();
	}

	private void findInterestingChildren() {
		if ((_children[0]) instanceof AnimationInfoAtom) {
			animationAtom = ((AnimationInfoAtom) (_children[0]));
		}else {
			Record.logger.log(POILogger.ERROR, ("First child record wasn't a AnimationInfoAtom, was of type " + (_children[0].getRecordType())));
		}
	}

	public AnimationInfo() {
		_header = new byte[8];
		_header[0] = 15;
		LittleEndian.putShort(_header, 2, ((short) (getRecordType())));
		_children = new Record[1];
	}

	public long getRecordType() {
		return RecordTypes.AnimationInfo.typeID;
	}

	public void writeOut(OutputStream out) throws IOException {
		writeOut(_header[0], _header[1], getRecordType(), _children, out);
	}

	public AnimationInfoAtom getAnimationInfoAtom() {
		return animationAtom;
	}
}

