import org.apache.poi.hslf.record.*;


import java.io.IOException;
import java.io.OutputStream;

import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;

/**
 * Tne atom that holds metadata on a specific Link in the document.
 * (The actual link is held in a sibling CString record)
 *
 * @author Nick Burch
 */
public final class ExHyperlinkAtom extends RecordAtom {

    //arbitrarily selected; may need to increase
    private static final int MAX_RECORD_LENGTH = 100_000;

    /**
     * Record header.
     */
    private byte[] _header;

    /**
     * Record data.
     */
    private byte[] _data;

    /**
     * Constructs a brand new link related atom record.
     */
    protected ExHyperlinkAtom() {
        _header = new byte[8];
        _data = new byte[4];

        LittleEndian.putShort(_header, 2, (short)getRecordType());
        LittleEndian.putInt(_header, 4, _data.length);

        // It is fine for the other values to be zero
    }

    /**
     * Constructs the link related atom record from its
     *  source data.
     *
     * @param source the source data as a byte array.
     * @param start the start offset into the byte array.
     * @param len the length of the slice in the byte array.
     */
    protected ExHyperlinkAtom(byte[] source, int start, int len) {
        // Get the header.
        _header = new byte[8];
        System.arraycopy(source,start,_header,0,8);

        // Get the record data.
        _data = IOUtils.safelyAllocate(len-8, MAX_RECORD_LENGTH);
        System.arraycopy(source,start+8,_data,0,len-8);

        // Must be at least 4 bytes long
        if(_data.length < 4) {
        	throw new IllegalArgumentException("The length of the data for a ExHyperlinkAtom must be at least 4 bytes, but was only " + _data.length);
        }
    }

    /**
     * Gets the link number. This will match the one in the
     *  InteractiveInfoAtom which uses the link.
     * @return the link number
     */
    public int getNumber() {
        return LittleEndian.getInt(_data,0);
    }

    /**
     * Sets the link number
     * @param number the link number.
     */
    public void setNumber(int number) {
        LittleEndian.putInt(_data,0,number);
    }

    /**
     * Gets the record type.
     * @return the record type.
     */
    public long getRecordType() { return RecordTypes.ExHyperlinkAtom.typeID; }

    /**
     * Write the contents of the record back, so it can be written
     * to disk
     *
     * @param out the output stream to write to.
     * @throws IOException if an error occurs.
     */
    public void writeOut(OutputStream out) throws IOException {
        out.write(_header);
        out.write(_data);
    }
}
