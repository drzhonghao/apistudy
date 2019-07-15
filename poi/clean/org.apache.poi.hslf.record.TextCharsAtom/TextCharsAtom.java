import org.apache.poi.hslf.record.*;


import java.io.IOException;
import java.io.OutputStream;

import org.apache.poi.util.HexDump;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.StringUtil;

/**
 * A TextCharsAtom (type 4000). Holds text in byte swapped unicode form.
 * The trailing return character is always stripped from this
 */

public final class TextCharsAtom extends RecordAtom {
    public static final long _type = RecordTypes.TextCharsAtom.typeID;
	//arbitrarily selected; may need to increase
	private static final int MAX_RECORD_LENGTH = 1_000_000;

	private byte[] _header;

	/** The bytes that make up the text */
	private byte[] _text;

	/** Grabs the text. */
	public String getText() {
		return StringUtil.getFromUnicodeLE(_text);
	}

	/** Updates the text in the Atom. */
	public void setText(String text) {
		// Convert to little endian unicode
		_text = IOUtils.safelyAllocate(text.length()*2, MAX_RECORD_LENGTH);
		StringUtil.putUnicodeLE(text,_text,0);

		// Update the size (header bytes 5-8)
		LittleEndian.putInt(_header,4,_text.length);
	}

	/* *************** record code follows ********************** */

	/**
	 * For the TextChars Atom
	 */
	protected TextCharsAtom(byte[] source, int start, int len) {
		// Sanity Checking
		if(len < 8) { len = 8; }

		// Get the header
		_header = new byte[8];
		System.arraycopy(source,start,_header,0,8);

		// Grab the text
		_text = IOUtils.safelyAllocate(len-8, MAX_RECORD_LENGTH);
		System.arraycopy(source,start+8,_text,0,len-8);
	}
	/**
	 * Create an empty TextCharsAtom
	 */
	public TextCharsAtom() {
		// 0 length header
		_header = new byte[] {  0, 0, 0xA0-256, 0x0f, 0, 0, 0, 0 };
		// Empty text
		_text = new byte[0];
	}

	/**
	 * We are of type 4000
	 */
	@Override
    public long getRecordType() { return _type; }

	/**
	 * Write the contents of the record back, so it can be written
	 *  to disk
	 */
	@Override
    public void writeOut(OutputStream out) throws IOException {
		// Header - size or type unchanged
		out.write(_header);

		// Write out our text
		out.write(_text);
	}

	/**
	 * dump debug info; use getText() to return a string
	 * representation of the atom
	 */
	@Override
    public String toString() {
        StringBuffer out = new StringBuffer();
        out.append( "TextCharsAtom:\n");
		out.append( HexDump.dump(_text, 0, 0) );
		return out.toString();
	}
}
