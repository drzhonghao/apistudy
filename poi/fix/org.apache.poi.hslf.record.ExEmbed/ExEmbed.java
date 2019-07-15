

import java.io.IOException;
import java.io.OutputStream;
import org.apache.poi.hslf.record.CString;
import org.apache.poi.hslf.record.ExEmbedAtom;
import org.apache.poi.hslf.record.ExOleObjAtom;
import org.apache.poi.hslf.record.Record;
import org.apache.poi.hslf.record.RecordAtom;
import org.apache.poi.hslf.record.RecordContainer;
import org.apache.poi.hslf.record.RecordTypes;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogger;


public class ExEmbed extends RecordContainer {
	private final byte[] _header;

	private RecordAtom embedAtom;

	private ExOleObjAtom oleObjAtom;

	private CString menuName;

	private CString progId;

	private CString clipboardName;

	protected ExEmbed(final byte[] source, final int start, final int len) {
		_header = new byte[8];
		System.arraycopy(source, start, _header, 0, 8);
		_children = Record.findChildRecords(source, (start + 8), (len - 8));
		findInterestingChildren();
	}

	protected ExEmbed(final RecordAtom embedAtom) {
		this();
		_children[0] = this.embedAtom = embedAtom;
	}

	public ExEmbed() {
		_header = new byte[8];
		_children = new Record[5];
		_header[0] = 15;
		LittleEndian.putShort(_header, 2, ((short) (getRecordType())));
		final CString cs1 = new CString();
		cs1.setOptions((1 << 4));
		final CString cs2 = new CString();
		cs2.setOptions((2 << 4));
		final CString cs3 = new CString();
		cs3.setOptions((3 << 4));
		_children[1] = new ExOleObjAtom();
		_children[2] = cs1;
		_children[3] = cs2;
		_children[4] = cs3;
		findInterestingChildren();
	}

	private void findInterestingChildren() {
		if ((_children[0]) instanceof ExEmbedAtom) {
			embedAtom = ((ExEmbedAtom) (_children[0]));
		}else {
			Record.logger.log(POILogger.ERROR, ("First child record wasn't a ExEmbedAtom, was of type " + (_children[0].getRecordType())));
		}
		if ((_children[1]) instanceof ExOleObjAtom) {
			oleObjAtom = ((ExOleObjAtom) (_children[1]));
		}else {
			Record.logger.log(POILogger.ERROR, ("Second child record wasn't a ExOleObjAtom, was of type " + (_children[1].getRecordType())));
		}
		for (int i = 2; i < (_children.length); i++) {
			if ((_children[i]) instanceof CString) {
				final CString cs = ((CString) (_children[i]));
				final int opts = (cs.getOptions()) >> 4;
				switch (opts) {
					case 1 :
						menuName = cs;
						break;
					case 2 :
						progId = cs;
						break;
					case 3 :
						clipboardName = cs;
						break;
					default :
						break;
				}
			}
		}
	}

	public ExEmbedAtom getExEmbedAtom() {
		return ((ExEmbedAtom) (embedAtom));
	}

	public ExOleObjAtom getExOleObjAtom() {
		return oleObjAtom;
	}

	public String getMenuName() {
		return (menuName) == null ? null : menuName.getText();
	}

	public void setMenuName(final String menuName) {
		this.menuName = safeCString(this.menuName, 1);
		this.menuName.setText(menuName);
	}

	public String getProgId() {
		return (progId) == null ? null : progId.getText();
	}

	public void setProgId(final String progId) {
		this.progId = safeCString(this.progId, 2);
		this.progId.setText(progId);
	}

	public String getClipboardName() {
		return (clipboardName) == null ? null : clipboardName.getText();
	}

	public void setClipboardName(final String clipboardName) {
		this.clipboardName = safeCString(this.clipboardName, 3);
		this.clipboardName.setText(clipboardName);
	}

	@Override
	public long getRecordType() {
		return RecordTypes.ExEmbed.typeID;
	}

	@Override
	public void writeOut(final OutputStream out) throws IOException {
		writeOut(_header[0], _header[1], getRecordType(), _children, out);
	}

	private CString safeCString(CString oldStr, int optionsId) {
		CString newStr = oldStr;
		if (newStr == null) {
			newStr = new CString();
			newStr.setOptions((optionsId << 4));
		}
		boolean found = false;
		for (final Record r : _children) {
			if (r == newStr) {
				found = true;
				break;
			}
		}
		if (!found) {
			appendChildRecord(newStr);
		}
		return newStr;
	}
}

