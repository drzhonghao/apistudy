import org.apache.poi.hslf.record.PositionDependentRecordContainer;
import org.apache.poi.hslf.record.Record;
import org.apache.poi.hslf.record.*;


import org.apache.poi.util.LittleEndian;
import java.io.IOException;
import java.io.OutputStream;

/**
 * If we come across a record we know has children of (potential)
 *  interest, but where the record itself is boring, but where other
 *  records may care about where this one lives, we create one
 *  of these. It allows us to get at the children, and track where on
 *  disk this is, but not much else.
 * Anything done using this should quite quickly be transitioned to its
 *  own proper record class!
 *
 * @author Nick Burch
 */

public final class DummyPositionSensitiveRecordWithChildren extends PositionDependentRecordContainer
{
	private byte[] _header;
	private long _type;

	/**
	 * Create a new holder for a boring record with children, but with
	 *  position dependent characteristics
	 */
	protected DummyPositionSensitiveRecordWithChildren(byte[] source, int start, int len) {
		// Just grab the header, not the whole contents
		_header = new byte[8];
		System.arraycopy(source,start,_header,0,8);
		_type = LittleEndian.getUShort(_header,2);

		// Find our children
		_children = Record.findChildRecords(source,start+8,len-8);
	}

	/**
	 * Return the value we were given at creation
	 */
	public long getRecordType() { return _type; }

	/**
	 * Write the contents of the record back, so it can be written
	 *  to disk
	 */
	public void writeOut(OutputStream out) throws IOException {
		writeOut(_header[0],_header[1],_type,_children,out);
	}
}
