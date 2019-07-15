import org.apache.poi.hssf.record.CellRecord;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;

/**
 * Title:        Label SST Record<P>
 * Description:  Refers to a string in the shared string table and is a column value.<P>
 * REFERENCE:  PG 325 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)
 */
public final class LabelSSTRecord extends CellRecord implements Cloneable {
    public final static short sid = 0xfd;
    private int field_4_sst_index;

    public LabelSSTRecord() {
    	// fields uninitialised
    }

    public LabelSSTRecord(RecordInputStream in) {
        super(in);
        field_4_sst_index = in.readInt();
    }

    /**
     * set the index to the string in the SSTRecord
     *
     * @param index - of string in the SST Table
     * @see org.apache.poi.hssf.record.SSTRecord
     */
    public void setSSTIndex(int index) {
        field_4_sst_index = index;
    }


    /**
     * get the index to the string in the SSTRecord
     *
     * @return index of string in the SST Table
     * @see org.apache.poi.hssf.record.SSTRecord
     */
    public int getSSTIndex() {
        return field_4_sst_index;
    }
    
    @Override
    protected String getRecordName() {
    	return "LABELSST";
    }

    @Override
    protected void appendValueText(StringBuilder sb) {
		sb.append("  .sstIndex = ");
    	sb.append(HexDump.shortToHex(getSSTIndex()));
    }
    @Override
    protected void serializeValue(LittleEndianOutput out) {
        out.writeInt(getSSTIndex());
    }

    @Override
    protected int getValueDataSize() {
        return 4;
    }

    @Override
    public short getSid() {
        return sid;
    }

    @Override
    public LabelSSTRecord clone() {
      LabelSSTRecord rec = new LabelSSTRecord();
      copyBaseFields(rec);
      rec.field_4_sst_index = field_4_sst_index;
      return rec;
    }
}
