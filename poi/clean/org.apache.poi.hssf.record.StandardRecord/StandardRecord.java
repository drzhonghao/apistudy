import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.LittleEndianByteArrayOutputStream;
import org.apache.poi.util.LittleEndianOutput;

/**
 * Subclasses of this class (the majority of BIFF records) are non-continuable.  This allows for
 * some simplification of serialization logic
 */
public abstract class StandardRecord extends Record {
	protected abstract int getDataSize();
	public final int getRecordSize() {
		return 4 + getDataSize();
	}

    /**
     * Write the data content of this BIFF record including the sid and record length.
     * <p>
     * The subclass must write the exact number of bytes as reported by
     *  {@link org.apache.poi.hssf.record.Record#getRecordSize()}}
     */
	@Override
	public final int serialize(int offset, byte[] data) {
		int dataSize = getDataSize();
		int recSize = 4 + dataSize;
		LittleEndianByteArrayOutputStream out = new LittleEndianByteArrayOutputStream(data, offset, recSize);
		out.writeShort(getSid());
		out.writeShort(dataSize);
		serialize(out);
		if (out.getWriteIndex() - offset != recSize) {
			throw new IllegalStateException("Error in serialization of (" + getClass().getName() + "): "
					+ "Incorrect number of bytes written - expected "
					+ recSize + " but got " + (out.getWriteIndex() - offset));
		}
		return recSize;
	}

    /**
     * Write the data content of this BIFF record.  The 'ushort sid' and 'ushort size' header fields
     * have already been written by the superclass.
     * <p>
     * The number of bytes written must equal the record size reported by
     *  {@link org.apache.poi.hssf.record.Record#getRecordSize()}} minus four
     *  ( record header consisting of a 'ushort sid' and 'ushort reclength' has already been written
     *  by their superclass).
     * 
     * @param out the output object
     */
	protected abstract void serialize(LittleEndianOutput out);
}
