import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.LittleEndianOutput;

/**
 * Title:        Print Gridlines Record<P>
 * Description:  whether to print the gridlines when you enjoy you spreadsheet on paper.<P>
 * REFERENCE:  PG 373 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)<P>
 * @author Andrew C. Oliver (acoliver at apache dot org)
 * @author Jason Height (jheight at chariot dot net dot au)
 * @version 2.0-pre
 */
public final class PrintGridlinesRecord extends StandardRecord {
    public final static short sid = 0x2b;
    private short             field_1_print_gridlines;

    public PrintGridlinesRecord() {
    }

    public PrintGridlinesRecord(RecordInputStream in)
    {
        field_1_print_gridlines = in.readShort();
    }

    /**
     * set whether or not to print the gridlines (and make your spreadsheet ugly)
     *
     * @param pg  make spreadsheet ugly - Y/N
     */
    public void setPrintGridlines(boolean pg) {
        if (pg) {
            field_1_print_gridlines = 1;
        } else {
            field_1_print_gridlines = 0;
        }
    }

    /**
     * get whether or not to print the gridlines (and make your spreadsheet ugly)
     *
     * @return make spreadsheet ugly - Y/N
     */
    public boolean getPrintGridlines()
    {
        return (field_1_print_gridlines == 1);
    }

    public String toString() {
        return "[PRINTGRIDLINES]\n" +
                "    .printgridlines = " + getPrintGridlines() +
                "\n" +
                "[/PRINTGRIDLINES]\n";
    }

    public void serialize(LittleEndianOutput out) {
        out.writeShort(field_1_print_gridlines);
    }

    protected int getDataSize() {
        return 2;
    }

    public short getSid()
    {
        return sid;
    }

    public Object clone() {
      PrintGridlinesRecord rec = new PrintGridlinesRecord();
      rec.field_1_print_gridlines = field_1_print_gridlines;
      return rec;
    }
}
