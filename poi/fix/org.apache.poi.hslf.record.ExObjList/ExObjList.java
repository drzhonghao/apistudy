

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import org.apache.poi.hslf.record.ExHyperlink;
import org.apache.poi.hslf.record.ExHyperlinkAtom;
import org.apache.poi.hslf.record.ExObjListAtom;
import org.apache.poi.hslf.record.Record;
import org.apache.poi.hslf.record.RecordContainer;
import org.apache.poi.util.LittleEndian;


public class ExObjList extends RecordContainer {
	private byte[] _header;

	private static long _type = 1033;

	private ExObjListAtom exObjListAtom;

	public ExObjListAtom getExObjListAtom() {
		return exObjListAtom;
	}

	public ExHyperlink[] getExHyperlinks() {
		ArrayList<ExHyperlink> links = new ArrayList<>();
		for (int i = 0; i < (_children.length); i++) {
			if ((_children[i]) instanceof ExHyperlink) {
				links.add(((ExHyperlink) (_children[i])));
			}
		}
		return links.toArray(new ExHyperlink[links.size()]);
	}

	protected ExObjList(byte[] source, int start, int len) {
		_header = new byte[8];
		System.arraycopy(source, start, _header, 0, 8);
		_children = Record.findChildRecords(source, (start + 8), (len - 8));
		findInterestingChildren();
	}

	private void findInterestingChildren() {
		if ((_children[0]) instanceof ExObjListAtom) {
			exObjListAtom = ((ExObjListAtom) (_children[0]));
		}else {
			throw new IllegalStateException(("First child record wasn't a ExObjListAtom, was of type " + (_children[0].getRecordType())));
		}
	}

	public ExObjList() {
		_header = new byte[8];
		_children = new Record[1];
		_header[0] = 15;
		LittleEndian.putShort(_header, 2, ((short) (ExObjList._type)));
		findInterestingChildren();
	}

	public long getRecordType() {
		return ExObjList._type;
	}

	public void writeOut(OutputStream out) throws IOException {
		writeOut(_header[0], _header[1], ExObjList._type, _children, out);
	}

	public ExHyperlink get(int id) {
		for (int i = 0; i < (_children.length); i++) {
			if ((_children[i]) instanceof ExHyperlink) {
				ExHyperlink rec = ((ExHyperlink) (_children[i]));
				if ((rec.getExHyperlinkAtom().getNumber()) == id) {
					return rec;
				}
			}
		}
		return null;
	}
}

