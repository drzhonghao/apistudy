import org.apache.poi.hssf.record.AbstractEscherHolderRecord;
import org.apache.poi.hssf.record.*;


import java.io.ByteArrayInputStream;

/**
 * This is purely for the biff viewer.  During normal operations we don't want
 * to be seeing this.
 */
public final class DrawingRecordForBiffViewer extends AbstractEscherHolderRecord {
    public static final short sid = 0xEC;

    public DrawingRecordForBiffViewer()
    {
    }

    public DrawingRecordForBiffViewer( RecordInputStream in)
    {
        super(in);
    }

    public DrawingRecordForBiffViewer(DrawingRecord r)
    {
    	super(convertToInputStream(r));
    	convertRawBytesToEscherRecords();
    }
    private static RecordInputStream convertToInputStream(DrawingRecord r)
    {
    	byte[] data = r.serialize();
    	RecordInputStream rinp = new RecordInputStream(
    			new ByteArrayInputStream(data)
    	);
    	rinp.nextRecord();
    	return rinp;
    }

    protected String getRecordName()
    {
        return "MSODRAWING";
    }

    public short getSid()
    {
        return sid;
    }
}
