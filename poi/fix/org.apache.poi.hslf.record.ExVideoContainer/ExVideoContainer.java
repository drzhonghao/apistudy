

import java.io.IOException;
import java.io.OutputStream;
import org.apache.poi.hslf.record.CString;
import org.apache.poi.hslf.record.ExMediaAtom;
import org.apache.poi.hslf.record.Record;
import org.apache.poi.hslf.record.RecordContainer;
import org.apache.poi.hslf.record.RecordTypes;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogger;


public final class ExVideoContainer extends RecordContainer {
	private byte[] _header;

	private ExMediaAtom mediaAtom;

	private CString pathAtom;

	protected ExVideoContainer(byte[] source, int start, int len) {
		_header = new byte[8];
		System.arraycopy(source, start, _header, 0, 8);
		_children = Record.findChildRecords(source, (start + 8), (len - 8));
		findInterestingChildren();
	}

	private void findInterestingChildren() {
		if ((_children[0]) instanceof ExMediaAtom) {
			mediaAtom = ((ExMediaAtom) (_children[0]));
		}else {
			Record.logger.log(POILogger.ERROR, ("First child record wasn't a ExMediaAtom, was of type " + (_children[0].getRecordType())));
		}
		if ((_children[1]) instanceof CString) {
			pathAtom = ((CString) (_children[1]));
		}else {
			Record.logger.log(POILogger.ERROR, ("Second child record wasn't a CString, was of type " + (_children[1].getRecordType())));
		}
	}

	public ExVideoContainer() {
		_header = new byte[8];
		_header[0] = 15;
		LittleEndian.putShort(_header, 2, ((short) (getRecordType())));
		_children = new Record[2];
		_children[1] = pathAtom = new CString();
	}

	public long getRecordType() {
		return RecordTypes.ExVideoContainer.typeID;
	}

	public void writeOut(OutputStream out) throws IOException {
		writeOut(_header[0], _header[1], getRecordType(), _children, out);
	}

	public ExMediaAtom getExMediaAtom() {
		return mediaAtom;
	}

	public CString getPathAtom() {
		return pathAtom;
	}
}

