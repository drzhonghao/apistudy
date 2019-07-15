

import java.io.IOException;
import java.io.OutputStream;
import org.apache.poi.hslf.record.CString;
import org.apache.poi.hslf.record.Comment2000Atom;
import org.apache.poi.hslf.record.Record;
import org.apache.poi.hslf.record.RecordContainer;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogger;


public final class Comment2000 extends RecordContainer {
	private byte[] _header;

	private static long _type = 12000;

	private CString authorRecord;

	private CString authorInitialsRecord;

	private CString commentRecord;

	private Comment2000Atom commentAtom;

	public Comment2000Atom getComment2000Atom() {
		return commentAtom;
	}

	public String getAuthor() {
		return (authorRecord) == null ? null : authorRecord.getText();
	}

	public void setAuthor(String author) {
		authorRecord.setText(author);
	}

	public String getAuthorInitials() {
		return (authorInitialsRecord) == null ? null : authorInitialsRecord.getText();
	}

	public void setAuthorInitials(String initials) {
		authorInitialsRecord.setText(initials);
	}

	public String getText() {
		return (commentRecord) == null ? null : commentRecord.getText();
	}

	public void setText(String text) {
		commentRecord.setText(text);
	}

	protected Comment2000(byte[] source, int start, int len) {
		_header = new byte[8];
		System.arraycopy(source, start, _header, 0, 8);
		_children = Record.findChildRecords(source, (start + 8), (len - 8));
		findInterestingChildren();
	}

	private void findInterestingChildren() {
		for (Record r : _children) {
			if (r instanceof CString) {
				CString cs = ((CString) (r));
				int recInstance = (cs.getOptions()) >> 4;
				switch (recInstance) {
					case 0 :
						authorRecord = cs;
						break;
					case 1 :
						commentRecord = cs;
						break;
					case 2 :
						authorInitialsRecord = cs;
						break;
					default :
						break;
				}
			}else
				if (r instanceof Comment2000Atom) {
					commentAtom = ((Comment2000Atom) (r));
				}else {
					Record.logger.log(POILogger.WARN, ((("Unexpected record with type=" + (r.getRecordType())) + " in Comment2000: ") + (r.getClass().getName())));
				}

		}
	}

	public Comment2000() {
		_header = new byte[8];
		_children = new Record[4];
		_header[0] = 15;
		LittleEndian.putShort(_header, 2, ((short) (Comment2000._type)));
		CString csa = new CString();
		CString csb = new CString();
		CString csc = new CString();
		csa.setOptions(0);
		csb.setOptions(16);
		csc.setOptions(32);
		_children[0] = csa;
		_children[1] = csb;
		_children[2] = csc;
		findInterestingChildren();
	}

	public long getRecordType() {
		return Comment2000._type;
	}

	public void writeOut(OutputStream out) throws IOException {
		writeOut(_header[0], _header[1], Comment2000._type, _children, out);
	}
}

