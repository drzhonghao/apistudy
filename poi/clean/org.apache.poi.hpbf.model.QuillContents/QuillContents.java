import org.apache.poi.hpbf.model.HPBFPart;
import org.apache.poi.hpbf.model.*;


import java.io.IOException;

import org.apache.poi.hpbf.model.qcbits.QCBit;
import org.apache.poi.hpbf.model.qcbits.QCPLCBit;
import org.apache.poi.hpbf.model.qcbits.QCTextBit;
import org.apache.poi.hpbf.model.qcbits.UnknownQCBit;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

/**
 * Read Quill Contents (/Quill/QuillSub/CONTENTS) from an HPBF (Publisher .pub) document
 */
public final class QuillContents extends HPBFPart {
	private static POILogger logger = POILogFactory.getLogger(QuillContents.class);
	//arbitrarily selected; may need to increase
	private static final int MAX_RECORD_LENGTH = 1_000_000;

	private static final String[] PATH = { "Quill", "QuillSub", "CONTENTS", };
	private QCBit[] bits;

	public QuillContents(DirectoryNode baseDir) throws IOException {
		super(baseDir, PATH);

		// Now parse the first 512 bytes, and produce
		//  all our bits
		byte data[] = getData();

		// Check first 8 bytes
		String f8 = new String(data, 0, 8, LocaleUtil.CHARSET_1252);
		if(! f8.equals("CHNKINK ")) {
			throw new IllegalArgumentException("Expecting 'CHNKINK ' but was '"+f8+"'");
		}
		// Ignore the next 24, for now at least

		// Now, parse all our QC Bits
		bits = new QCBit[20];
		for(int i=0; i<20; i++) {
			int offset = 0x20 + i*24;
			if(data[offset] == 0x18 && data[offset+1] == 0x00) {
				// Has some data
				String thingType = new String(data, offset+2, 4, LocaleUtil.CHARSET_1252);
				int optA = LittleEndian.getUShort(data, offset+6);
				int optB = LittleEndian.getUShort(data, offset+8);
				int optC = LittleEndian.getUShort(data, offset+10);
				String bitType = new String(data, offset+12, 4, LocaleUtil.CHARSET_1252);
				int from = (int)LittleEndian.getUInt(data, offset+16);
				int len = (int)LittleEndian.getUInt(data, offset+20);

				byte[] bitData = IOUtils.safelyAllocate(len, MAX_RECORD_LENGTH);
				System.arraycopy(data, from, bitData, 0, len);

				// Create
				if(bitType.equals("TEXT")) {
					bits[i] = new QCTextBit(thingType, bitType, bitData);
				} else if(bitType.equals("PLC ")) {
					try {
						bits[i] = QCPLCBit.createQCPLCBit(thingType, bitType, bitData);
					} catch (ArrayIndexOutOfBoundsException e) {
						// bug 60685: fall back so that the rest of the document can be read
						logger.log(POILogger.WARN, "Unable to read Quill Contents PLC Bit record. Ignoring this record.");
						bits[i] = new UnknownQCBit(thingType, bitType, bitData);
					}
				} else {
					bits[i] = new UnknownQCBit(thingType, bitType, bitData);
				}
				bits[i].setOptA(optA);
				bits[i].setOptB(optB);
				bits[i].setOptC(optC);
				bits[i].setDataOffset(from);
			} else {
				// Doesn't have data
			}
		}
	}

	public QCBit[] getBits() {
		return bits;
	}

	protected void generateData() {
		// TODO
		throw new IllegalStateException("Not done yet!");
	}
}
