

import java.io.IOException;
import java.io.OutputStream;
import org.apache.poi.hslf.record.CString;
import org.apache.poi.hslf.record.ExHyperlinkAtom;
import org.apache.poi.hslf.record.Record;
import org.apache.poi.hslf.record.RecordContainer;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogger;


public class ExHyperlink extends RecordContainer {
	private byte[] _header;

	private static long _type = 4055;

	private ExHyperlinkAtom linkAtom;

	private CString linkDetailsA;

	private CString linkDetailsB;

	public ExHyperlinkAtom getExHyperlinkAtom() {
		return linkAtom;
	}

	public String getLinkURL() {
		return (linkDetailsB) == null ? null : linkDetailsB.getText();
	}

	public String getLinkTitle() {
		return (linkDetailsA) == null ? null : linkDetailsA.getText();
	}

	public void setLinkURL(String url) {
		if ((linkDetailsB) != null) {
			linkDetailsB.setText(url);
		}
	}

	public void setLinkOptions(int options) {
		if ((linkDetailsB) != null) {
			linkDetailsB.setOptions(options);
		}
	}

	public void setLinkTitle(String title) {
		if ((linkDetailsA) != null) {
			linkDetailsA.setText(title);
		}
	}

	public String _getDetailsA() {
		return (linkDetailsA) == null ? null : linkDetailsA.getText();
	}

	public String _getDetailsB() {
		return (linkDetailsB) == null ? null : linkDetailsB.getText();
	}

	protected ExHyperlink(byte[] source, int start, int len) {
		_header = new byte[8];
		System.arraycopy(source, start, _header, 0, 8);
		_children = Record.findChildRecords(source, (start + 8), (len - 8));
		findInterestingChildren();
	}

	private void findInterestingChildren() {
		if ((_children[0]) instanceof ExHyperlinkAtom) {
			linkAtom = ((ExHyperlinkAtom) (_children[0]));
		}else {
			Record.logger.log(POILogger.ERROR, ("First child record wasn't a ExHyperlinkAtom, was of type " + (_children[0].getRecordType())));
		}
		for (int i = 1; i < (_children.length); i++) {
			if ((_children[i]) instanceof CString) {
				if ((linkDetailsA) == null)
					linkDetailsA = ((CString) (_children[i]));
				else
					linkDetailsB = ((CString) (_children[i]));

			}else {
				Record.logger.log(POILogger.ERROR, ("Record after ExHyperlinkAtom wasn't a CString, was of type " + (_children[1].getRecordType())));
			}
		}
	}

	public ExHyperlink() {
		_header = new byte[8];
		_children = new Record[3];
		_header[0] = 15;
		LittleEndian.putShort(_header, 2, ((short) (ExHyperlink._type)));
		CString csa = new CString();
		CString csb = new CString();
		csa.setOptions(0);
		csb.setOptions(16);
		_children[1] = csa;
		_children[2] = csb;
		findInterestingChildren();
	}

	public long getRecordType() {
		return ExHyperlink._type;
	}

	public void writeOut(OutputStream out) throws IOException {
		writeOut(_header[0], _header[1], ExHyperlink._type, _children, out);
	}
}

