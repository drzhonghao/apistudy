import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.LittleEndianOutput;

/**
 * Title: Sheet Tab Index Array Record (0x013D)<p>
 * Description:  Contains an array of sheet id's.  Sheets always keep their ID
 *               regardless of what their name is.<p>
 * REFERENCE:  PG 412 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)
 */
public final class TabIdRecord extends StandardRecord {
    public final static short sid = 0x013D;
    private static final short[] EMPTY_SHORT_ARRAY = { };

    public short[] _tabids;

    public TabIdRecord() {
        _tabids = EMPTY_SHORT_ARRAY;
    }

    public TabIdRecord(RecordInputStream in) {
        int nTabs = in.remaining() / 2;
        _tabids = new short[nTabs];
        for (int i = 0; i < _tabids.length; i++) {
            _tabids[i] = in.readShort();
        }
    }

    /**
     * set the tab array.  (0,1,2).
     * @param array of tab id's {0,1,2}
     */
    public void setTabIdArray(short[] array) {
        _tabids = array.clone();
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[TABID]\n");
        buffer.append("    .elements        = ").append(_tabids.length).append("\n");
        for (int i = 0; i < _tabids.length; i++) {
            buffer.append("    .element_").append(i).append(" = ").append(_tabids[i]).append("\n");
        }
        buffer.append("[/TABID]\n");
        return buffer.toString();
    }

    public void serialize(LittleEndianOutput out) {
        short[] tabids = _tabids;

        for (int i = 0; i < tabids.length; i++) {
            out.writeShort(tabids[i]);
        }
    }

    protected int getDataSize() {
        return _tabids.length * 2;
    }

    public short getSid() {
        return sid;
    }
}
