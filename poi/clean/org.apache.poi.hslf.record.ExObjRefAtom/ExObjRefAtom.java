import org.apache.poi.hslf.record.*;


import org.apache.poi.util.LittleEndian;
import java.io.IOException;
import java.io.OutputStream;

/**
 * ExObjRefAtom (3009).
 * <p>
 *  An atom record that specifies a reference to an external object.
 * </p>
 */

public final class ExObjRefAtom extends RecordAtom {
	private byte[] _header;

	/**
	 * A 4-byte unsigned integer that specifies a reference to an external object.
	 * It MUST be equal to the value of the exObjId field of an ExMediaAtom record
	 * or the value of the exObjId field of an ExOleObjAtom record.
	 */
	private int exObjIdRef;

    /**
     * Create a new instance of <code>ExObjRefAtom</code>
     */
    public ExObjRefAtom() {
        _header = new byte[8];
        LittleEndian.putUShort(_header, 0, 0);
        LittleEndian.putUShort(_header, 2, (int)getRecordType());
        LittleEndian.putInt(_header, 4, 4);
        exObjIdRef = 0;
    }

    /**
     * Build an instance of <code>ExObjRefAtom</code> from on-disk data
     * 
     * @param source the source data as a byte array.
     * @param start the start offset into the byte array.
     * @param len the length of the slice in the byte array.
     */
	protected ExObjRefAtom(byte[] source, int start, int len) {
		_header = new byte[8];
        int offset = start;
        System.arraycopy(source,start,_header,0,8);
        offset += _header.length;
        exObjIdRef = (int)LittleEndian.getUInt(source, offset);
	}

    /**
     * @return type of this record {@link RecordTypes#ExObjRefAtom}.
     */
	public long getRecordType() {
	    return RecordTypes.ExObjRefAtom.typeID;
    }

    public int getExObjIdRef(){
        return exObjIdRef;
    }

   public void setExObjIdRef(int id){
       exObjIdRef = id;
    }

   /**
	 * Write the contents of the record back, so it can be written
	 *  to disk
	 */
	public void writeOut(OutputStream out) throws IOException {
		out.write(_header);

        byte[] recdata = new byte[4];
        LittleEndian.putUInt(recdata, 0, exObjIdRef);

        out.write(recdata);
	}
}
