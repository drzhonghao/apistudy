import org.apache.poi.hslf.record.*;


import java.io.IOException;
import java.io.OutputStream;

import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;

/**
 * The atom that holds metadata on a specific embedded object in the document.
 *
 * <!--
 * 0    sint4    followColorScheme  This field indicates how the object follows the color scheme. Valid values are:
 *                                  0 - doesn't follow the color scheme
 *                                  1 - follows the entire color scheme
 *                                  2 - follows the text and background scheme
 *
 * 4    bool1    cantLockServerB    Set if the embedded server can not be locked
 * 5    bool1    noSizeToServerB    Set if don't need to send the dimension to the embedded object
 * 6    Bool1    isTable            Set if the object is a Word table
 * -->
 *
 * @author Daniel Noll
 */
public class ExEmbedAtom extends RecordAtom {

    //arbitrarily selected; may need to increase
    private static final int MAX_RECORD_LENGTH = 1_000_000;

    /**
     * Embedded document does not follow the color scheme.
     */
    public static final int DOES_NOT_FOLLOW_COLOR_SCHEME = 0;

    /**
     * Embedded document follows the entire color scheme.
     */
    public static final int FOLLOWS_ENTIRE_COLOR_SCHEME = 1;

    /**
     * Embedded document follows the text and background scheme.
     */
    public static final int FOLLOWS_TEXT_AND_BACKGROUND_SCHEME = 2;

    /**
     * Record header.
     */
    private byte[] _header;

    /**
     * Record data.
     */
    private byte[] _data;

    /**
     * Constructs a brand new embedded object atom record.
     */
    protected ExEmbedAtom() {
        _header = new byte[8];
        _data = new byte[8];

        LittleEndian.putShort(_header, 2, (short)getRecordType());
        LittleEndian.putInt(_header, 4, _data.length);

        // It is fine for the other values to be zero
    }

    /**
     * Constructs the embedded object atom record from its source data.
     *
     * @param source the source data as a byte array.
     * @param start the start offset into the byte array.
     * @param len the length of the slice in the byte array.
     */
    protected ExEmbedAtom(byte[] source, int start, int len) {
        // Get the header.
        _header = new byte[8];
        System.arraycopy(source,start,_header,0,8);

        // Get the record data.
        _data = IOUtils.safelyAllocate(len-8, MAX_RECORD_LENGTH);
        System.arraycopy(source,start+8,_data,0,len-8);

        // Must be at least 8 bytes long
        if(_data.length < 8) {
        	throw new IllegalArgumentException("The length of the data for a ExEmbedAtom must be at least 4 bytes, but was only " + _data.length);
        }
    }

    /**
     * Gets whether the object follows the color scheme.
     *
     * @return one of {@link #DOES_NOT_FOLLOW_COLOR_SCHEME},
     *                {@link #FOLLOWS_ENTIRE_COLOR_SCHEME}, or
     *                {@link #FOLLOWS_TEXT_AND_BACKGROUND_SCHEME}.
     */
    public int getFollowColorScheme() {
        return LittleEndian.getInt(_data, 0);
    }

    /**
     * Gets whether the embedded server cannot be locked.
     *
     * @return {@code true} if the embedded server cannot be locked.
     */
    public boolean getCantLockServerB() {
        return _data[4] != 0;
    }

    public void setCantLockServerB(boolean cantBeLocked) {
    	_data[4] = (byte)(cantBeLocked ? 1 : 0);
    }
    
    /**
     * Gets whether it is not required to send the dimensions to the embedded object.
     *
     * @return {@code true} if the embedded server does not require the object dimensions.
     */
    public boolean getNoSizeToServerB() {
        return _data[5] != 0;
    }

    /**
     * Getswhether the object is a Word table.
     *
     * @return {@code true} if the object is a Word table.
     */
    public boolean getIsTable() {
        return _data[6] != 0;
    }

    /**
     * Gets the record type.
     * @return the record type.
     */
    public long getRecordType() {
        return RecordTypes.ExEmbedAtom.typeID;
    }

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
