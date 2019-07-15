import org.apache.poi.hssf.record.*;


import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;

/**
 * Title:        Continue Record(0x003C) - Helper class used primarily for SST Records <P>
 * Description:  handles overflow for prior record in the input
 *               stream; content is tailored to that prior record<P>
 * @author Marc Johnson (mjohnson at apache dot org)
 * @author Andrew C. Oliver (acoliver at apache dot org)
 * @author Csaba Nagy (ncsaba at yahoo dot com)
 */
public final class ContinueRecord extends StandardRecord implements Cloneable {
    public final static short sid = 0x003C;
    private byte[] _data;

    public ContinueRecord(byte[] data) {
        _data = data;
    }

    protected int getDataSize() {
        return _data.length;
    }

    public void serialize(LittleEndianOutput out) {
        out.write(_data);
    }

    /**
     * get the data for continuation
     * @return byte array containing all of the continued data
     */
    public byte[] getData() {
        return _data;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[CONTINUE RECORD]\n");
        buffer.append("    .data = ").append(HexDump.toHex(_data)).append("\n");
        buffer.append("[/CONTINUE RECORD]\n");
        return buffer.toString();
    }

    public short getSid() {
        return sid;
    }

    public ContinueRecord(RecordInputStream in) {
        _data = in.readRemainder();
    }

    @Override
    public ContinueRecord clone() {
        return new ContinueRecord(_data);
    }
}
