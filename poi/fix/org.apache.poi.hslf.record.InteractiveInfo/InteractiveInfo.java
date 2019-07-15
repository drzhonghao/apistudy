

import java.io.IOException;
import java.io.OutputStream;
import org.apache.poi.hslf.record.InteractiveInfoAtom;
import org.apache.poi.hslf.record.Record;
import org.apache.poi.hslf.record.RecordContainer;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogger;


public class InteractiveInfo extends RecordContainer {
	private byte[] _header;

	private static long _type = 4082;

	private InteractiveInfoAtom infoAtom;

	public InteractiveInfoAtom getInteractiveInfoAtom() {
		return infoAtom;
	}

	protected InteractiveInfo(byte[] source, int start, int len) {
		_header = new byte[8];
		System.arraycopy(source, start, _header, 0, 8);
		_children = Record.findChildRecords(source, (start + 8), (len - 8));
		findInterestingChildren();
	}

	private void findInterestingChildren() {
		if ((((_children) == null) || ((_children.length) == 0)) || (!((_children[0]) instanceof InteractiveInfoAtom))) {
			Record.logger.log(POILogger.WARN, "First child record wasn't a InteractiveInfoAtom - leaving this atom in an invalid state...");
			return;
		}
		infoAtom = ((InteractiveInfoAtom) (_children[0]));
	}

	public InteractiveInfo() {
		_header = new byte[8];
		_children = new Record[1];
		_header[0] = 15;
		LittleEndian.putShort(_header, 2, ((short) (InteractiveInfo._type)));
		_children[0] = infoAtom;
	}

	public long getRecordType() {
		return InteractiveInfo._type;
	}

	public void writeOut(OutputStream out) throws IOException {
		writeOut(_header[0], _header[1], InteractiveInfo._type, _children, out);
	}
}

