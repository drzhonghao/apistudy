import org.apache.poi.hssf.record.*;


import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;
import org.apache.poi.util.RecordFormatException;

/**
 * ftEnd (0x0000)<p>
 * 
 * The end data record is used to denote the end of the subrecords.
 */
public final class EndSubRecord extends SubRecord implements Cloneable {
    // Note - zero sid is somewhat unusual (compared to plain Records)
    public final static short sid = 0x0000;
    private static final int ENCODED_SIZE = 0;

    public EndSubRecord()
    {

    }

    /**
     * @param in unused (since this record has no data)
     * @param size must be 0
     */
    public EndSubRecord(LittleEndianInput in, int size) {
        if ((size & 0xFF) != ENCODED_SIZE) { // mask out random crap in upper byte
            throw new RecordFormatException("Unexpected size (" + size + ")");
        }
    }

    @Override
    public boolean isTerminating(){
        return true;
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[ftEnd]\n");

        buffer.append("[/ftEnd]\n");
        return buffer.toString();
    }

    public void serialize(LittleEndianOutput out) {
        out.writeShort(sid);
        out.writeShort(ENCODED_SIZE);
    }

	protected int getDataSize() {
        return ENCODED_SIZE;
    }

    public short getSid()
    {
        return sid;
    }

    @Override
    public EndSubRecord clone() {

        return new EndSubRecord();
    }
}
