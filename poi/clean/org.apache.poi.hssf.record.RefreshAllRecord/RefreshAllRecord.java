import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.BitField;
import org.apache.poi.util.BitFieldFactory;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;

/**
 * Title:        Refresh All Record (0x01B7)<p>
 * Description:  Flag whether to refresh all external data when loading a sheet.
 *               (which hssf doesn't support anyhow so who really cares?)<p>
 * REFERENCE:  PG 376 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)
 */
public final class RefreshAllRecord extends StandardRecord {
    public final static short sid = 0x01B7;

    private static final BitField refreshFlag = BitFieldFactory.getInstance(0x0001);

    private int _options;

    private RefreshAllRecord(int options) {
        _options = options;
    }

    public RefreshAllRecord(RecordInputStream in) {
        this(in.readUShort());
    }

    public RefreshAllRecord(boolean refreshAll) {
        this(0);
        setRefreshAll(refreshAll);
    }

    /**
     * set whether to refresh all external data when loading a sheet
     * @param refreshAll or not
     */
    public void setRefreshAll(boolean refreshAll) {
        _options = refreshFlag.setBoolean(_options, refreshAll);
    }

    /**
     * get whether to refresh all external data when loading a sheet
     * @return refreshall or not
     */
    public boolean getRefreshAll() {
        return refreshFlag.isSet(_options);
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[REFRESHALL]\n");
        buffer.append("    .options      = ").append(HexDump.shortToHex(_options)).append("\n");
        buffer.append("[/REFRESHALL]\n");
        return buffer.toString();
    }

    public void serialize(LittleEndianOutput out) {
        out.writeShort(_options);
    }

    protected int getDataSize() {
        return 2;
    }

    public short getSid() {
        return sid;
    }
    @Override
    public Object clone() {
        return new RefreshAllRecord(_options);
    }
}
