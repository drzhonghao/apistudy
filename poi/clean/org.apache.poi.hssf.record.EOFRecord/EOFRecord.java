import org.apache.poi.hssf.record.*;


import org.apache.poi.util.LittleEndianOutput;

/**
 * End Of File record.
 * <P>
 * Description:  Marks the end of records belonging to a particular object in the
 *               HSSF File<P>
 * REFERENCE:  PG 307 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)<P>
 * @author Andrew C. Oliver (acoliver at apache dot org)
 * @author Jason Height (jheight at chariot dot net dot au)
 * @version 2.0-pre
 */
public final class EOFRecord extends StandardRecord implements Cloneable {
    public final static short sid = 0x0A;
	public static final int ENCODED_SIZE = 4;

	public static final EOFRecord instance = new EOFRecord();
	
    private EOFRecord() {
    	// no data fields
    }

    /**
     * @param in unused (since this record has no data)
     */
    public EOFRecord(RecordInputStream in)
    {
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[EOF]\n");
        buffer.append("[/EOF]\n");
        return buffer.toString();
    }

    public void serialize(LittleEndianOutput out) {
    }

    protected int getDataSize() {
        return ENCODED_SIZE - 4;
    }

    public short getSid()
    {
        return sid;
    }

    @Override
    public EOFRecord clone() {
      return instance;
    }
}
