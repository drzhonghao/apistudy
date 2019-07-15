import org.apache.poi.hssf.record.*;


import org.apache.poi.util.HexDump;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;

/**
 * ftGmo (0x0006)<p>
 * The group marker record is used as a position holder for groups.
 */
public final class GroupMarkerSubRecord extends SubRecord implements Cloneable {
    public final static short sid = 0x0006;
    //arbitrarily selected; may need to increase
    private static final int MAX_RECORD_LENGTH = 100_000;


    private static final byte[] EMPTY_BYTE_ARRAY = { };

    // would really love to know what goes in here.
    private byte[] reserved;

    public GroupMarkerSubRecord() {
        reserved = EMPTY_BYTE_ARRAY;
    }

    public GroupMarkerSubRecord(LittleEndianInput in, int size) {
        byte[] buf = IOUtils.safelyAllocate(size, MAX_RECORD_LENGTH);
        in.readFully(buf);
        reserved = buf;
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        String nl = System.getProperty("line.separator");
        buffer.append("[ftGmo]" + nl);
        buffer.append("  reserved = ").append(HexDump.toHex(reserved)).append(nl);
        buffer.append("[/ftGmo]" + nl);
        return buffer.toString();
    }

    public void serialize(LittleEndianOutput out) {
        out.writeShort(sid);
        out.writeShort(reserved.length);
        out.write(reserved);
    }

	protected int getDataSize() {
        return reserved.length;
    }

    public short getSid()
    {
        return sid;
    }

    @Override
    public GroupMarkerSubRecord clone() {
        GroupMarkerSubRecord rec = new GroupMarkerSubRecord();
        rec.reserved = new byte[reserved.length];
        for ( int i = 0; i < reserved.length; i++ )
            rec.reserved[i] = reserved[i];
        return rec;
    }
}
