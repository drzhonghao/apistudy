import org.apache.poi.hssf.record.CellRecord;
import org.apache.poi.hssf.record.*;


import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.util.LittleEndianOutput;

/**
 * NUMBER (0x0203) Contains a numeric cell value. <P>
 * REFERENCE:  PG 334 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)<P>
 * @author Andrew C. Oliver (acoliver at apache dot org)
 * @author Jason Height (jheight at chariot dot net dot au)
 */
public final class NumberRecord extends CellRecord implements Cloneable {
    public static final short sid = 0x0203;
    private double field_4_value;

    /** Creates new NumberRecord */
    public NumberRecord() {
    	// fields uninitialised
    }

    /**
     * @param in the RecordInputstream to read the record from
     */
    public NumberRecord(RecordInputStream in) {
        super(in);
        field_4_value = in.readDouble();
    }

    /**
     * set the value for the cell
     *
     * @param value  double representing the value
     */
    public void setValue(double value){
        field_4_value = value;
    }

    /**
     * get the value for the cell
     *
     * @return double representing the value
     */
    public double getValue(){
        return field_4_value;
    }

    @Override
    protected String getRecordName() {
    	return "NUMBER";
    }

    @Override
    protected void appendValueText(StringBuilder sb) {
    	sb.append("  .value= ").append(NumberToTextConverter.toText(field_4_value));
    }

    @Override
    protected void serializeValue(LittleEndianOutput out) {
        out.writeDouble(getValue());
    }

    @Override
    protected int getValueDataSize() {
    	return 8;
    }

    @Override
    public short getSid() {
        return sid;
    }

    @Override
    public NumberRecord clone() {
      NumberRecord rec = new NumberRecord();
      copyBaseFields(rec);
      rec.field_4_value = field_4_value;
      return rec;
    }
}
