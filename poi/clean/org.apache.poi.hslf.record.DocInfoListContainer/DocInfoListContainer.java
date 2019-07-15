import org.apache.poi.hslf.record.RecordContainer;
import org.apache.poi.hslf.record.Record;
import org.apache.poi.hslf.record.*;


import java.io.IOException;
import java.io.OutputStream;

import org.apache.poi.util.LittleEndian;

/**
 * A container record that specifies information about the document and document display settings.
 */
public final class DocInfoListContainer extends RecordContainer {
	private byte[] _header;
	private static long _type = RecordTypes.List.typeID;

	// Links to our more interesting children

	/**
	 * Set things up, and find our more interesting children
	 */
	protected DocInfoListContainer(byte[] source, int start, int len) {
		// Grab the header
		_header = new byte[8];
		System.arraycopy(source,start,_header,0,8);

		// Find our children
		_children = Record.findChildRecords(source,start+8,len-8);
		findInterestingChildren();
	}

	/**
	 * Go through our child records, picking out the ones that are
	 *  interesting, and saving those for use by the easy helper
	 *  methods.
	 */
	private void findInterestingChildren() {

	}

	/**
	 * Create a new DocInfoListContainer, with blank fields - not yet supported
	 */
	private DocInfoListContainer() {
		_header = new byte[8];
		_children = new Record[0];

		// Setup our header block
		_header[0] = 0x0f; // We are a container record
		LittleEndian.putShort(_header, 2, (short)_type);

		// Setup our child records
		findInterestingChildren();
	}

	/**
	 * We are of type 0x7D0
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
