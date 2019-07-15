import org.apache.poi.hpbf.model.*;


import java.io.IOException;
import java.util.ArrayList;

import org.apache.poi.ddf.DefaultEscherRecordFactory;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.util.IOUtils;

/**
 * Parent class of all Escher parts
 */
public abstract class EscherPart extends HPBFPart {

	//arbitrarily selected; may need to increase
	private static final int MAX_RECORD_LENGTH = 1_000_000;

	private EscherRecord[] records;

	/**
	 * Creates the Escher Part, and finds our child
	 *  escher records
	 */
	public EscherPart(DirectoryNode baseDir, String[] parts) throws IOException {
		super(baseDir, parts);

		// Now create our Escher children
		DefaultEscherRecordFactory erf =
			new DefaultEscherRecordFactory();

		ArrayList<EscherRecord> ec = new ArrayList<>();
		byte data[] = getData();
		int left = data.length;
		while(left > 0) {
			EscherRecord er = erf.createRecord(data, 0);
			er.fillFields(data, 0, erf);
			left -= er.getRecordSize();

			ec.add(er);
		}

		records = ec.toArray(new EscherRecord[ec.size()]);
	}

	public EscherRecord[] getEscherRecords() {
		return records;
	}

	/**
	 * Serialises our Escher children back
	 *  into bytes.
	 */
	protected void generateData() {
		int size = 0;
		for(int i=0; i<records.length; i++) {
			size += records[i].getRecordSize();
		}

		byte data[] = IOUtils.safelyAllocate(size, MAX_RECORD_LENGTH);
		size = 0;
		for(int i=0; i<records.length; i++) {
			int thisSize =
				records[i].serialize(size, data);
			size += thisSize;
		}
		
		setData(data);
	}
}
