import org.apache.poi.hslf.dev.*;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Locale;

import org.apache.poi.ddf.DefaultEscherRecordFactory;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.ddf.EscherTextboxRecord;
import org.apache.poi.hslf.record.HSLFEscherRecordFactory;
import org.apache.poi.hslf.record.RecordTypes;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;

/**
 * This class provides a way to "peek" inside a powerpoint file. It
 *  will print out all the types it find, and for those it know aren't
 *  atoms, what they contain
 *
 * To figure out what things are, and if they are atoms or not, used the
 *  list in hslf.record.RecordTypes
 *
 * To peek inside PPDrawings, which hold Escher drawings, we use the
 *  DDF package from POI (but we can fake it by using the Escher listings
 *  from hslf.record.RecordTypes also)
 */
public final class SlideShowDumper {

	//arbitrarily selected; may need to increase
	private static final int MAX_RECORD_LENGTH = 100_000;

	private byte[] docstream;

  /** Do we try to use DDF to understand the escher objects? */
  private boolean ddfEscher;
  /** Do we use our own built-in basic escher groker to understand the escher objects? */
  private boolean basicEscher;
  
  private PrintStream out;

  /**
   *  right now this function takes one parameter: a ppt file, and outputs
   *  a dump of what it contains
   */
  public static void main(String args[]) throws IOException
  {
	if(args.length == 0) {
		System.err.println("Useage: SlideShowDumper [-escher|-basicescher] <filename>");
		return;
	}

	String filename = args[0];
	if(args.length > 1) {
		filename = args[1];
	}

	POIFSFileSystem poifs = new POIFSFileSystem(new File(filename));
	SlideShowDumper foo = new SlideShowDumper(poifs, System.out);
    poifs.close();

	if(args.length > 1) {
		if(args[0].equalsIgnoreCase("-escher")) {
			foo.setDDFEscher(true);
		} else {
			foo.setBasicEscher(true);
		}
	}

	foo.printDump();
  }

  /**
   * Constructs a Powerpoint dump from a POIFS Filesystem. Parses the
   * document and dumps out the contents
   *
   * @param filesystem the POIFS FileSystem to read from
   * @throws IOException if there is a problem while parsing the document.
   */
  public SlideShowDumper(POIFSFileSystem filesystem, PrintStream out) throws IOException {
	// Grab the document stream
	InputStream is = filesystem.createDocumentInputStream(HSLFSlideShow.POWERPOINT_DOCUMENT);
	docstream = IOUtils.toByteArray(is);
	is.close();
	this.out = out;
  }

  /**
   * Control dumping of any Escher records found - should DDF be used?
   */
  public void setDDFEscher(boolean grok) {
	ddfEscher = grok;
	basicEscher = !(grok);
  }

  /**
   * Control dumping of any Escher records found - should our built in
   *  basic groker be used?
   */
  public void setBasicEscher(boolean grok) {
	basicEscher = grok;
	ddfEscher = !(grok);
  }

  public void printDump() throws IOException {
	// The format of records in a powerpoint file are:
	//   <little endian 2 byte "info">
	//   <little endian 2 byte "type">
	//   <little endian 4 byte "length">
	// If it has a zero length, following it will be another record
	//		<xx xx yy yy 00 00 00 00> <xx xx yy yy zz zz zz zz>
	// If it has a length, depending on its type it may have children or data
	// If it has children, these will follow straight away
	//		<xx xx yy yy zz zz zz zz <xx xx yy yy zz zz zz zz>>
	// If it has data, this will come straigh after, and run for the length
	//      <xx xx yy yy zz zz zz zz dd dd dd dd dd dd dd>
	// All lengths given exclude the 8 byte record header
	// (Data records are known as Atoms)

	// Document should start with:
	//   0F 00 E8 03 ## ## ## ##
    //     (type 1000 = document, info 00 0f is normal, rest is document length)
	//   01 00 E9 03 28 00 00 00
	//     (type 1001 = document atom, info 00 01 normal, 28 bytes long)

	// When parsing a document, look to see if you know about that type
	//  of the current record. If you know it's a type that has children,
	//  process the record's data area looking for more records
	// If you know about the type and it doesn't have children, either do
	//  something with the data (eg TextRun) or skip over it
	// Otherwise, check the first byte. If you do a BINARY_AND on it with
	//  0x0f (15) and get back 0x0f, you know it has children. Otherwise
	//  it doesn't

	walkTree(0,0,docstream.length);
}

public void walkTree(int depth, int startPos, int maxLen) throws IOException {
	int pos = startPos;
	int endPos = startPos + maxLen;
	final String ind = (depth == 0) ? "%1$s" : "%1$"+depth+"s";
	while(pos <= endPos - 8) {
		long type = LittleEndian.getUShort(docstream,pos+2);
		long len = LittleEndian.getUInt(docstream,pos+4);
		byte opt = docstream[pos];

		String fmt = ind+"At position %2$d (%2$04x): type is %3$d (%3$04x), len is %4$d (%4$04x)";
		out.println(String.format(Locale.ROOT, fmt, "", pos, type, len));

		// See if we know about the type of it
		String recordName = RecordTypes.forTypeID((short)type).name();

		// Jump over header, and think about going on more
		pos += 8;
		out.println(String.format(Locale.ROOT, ind+"That's a %2$s", "", recordName));

		// Now check if it's a container or not
		int container = opt & 0x0f;

		// BinaryTagData seems to contain records, but it
		//  isn't tagged as doing so. Try stepping in anyway
		if(type == 5003L && opt == 0L) {
			container = 0x0f;
		}

        out.println();
		if (type != 0L && container == 0x0f) {
		    if (type == 1035L || type == 1036L) {
    			// Special Handling of 1035=PPDrawingGroup and 1036=PPDrawing
    			if(ddfEscher) {
    				// Seems to be:
    				walkEscherDDF((depth+3),pos+8,(int)len-8);
    			} else if(basicEscher) {
    				walkEscherBasic((depth+3),pos+8,(int)len-8);
    			}
    		} else {
    			// General container record handling code
    			walkTree((depth+2),pos,(int)len);
    		}
		}

		pos += (int)len;
	}
  }

  /**
   * Use the DDF code to walk the Escher records
   */
  public void walkEscherDDF(int indent, int pos, int len) {
	if(len < 8) { return; }

	final String ind = (indent == 0) ? "%1$s" : "%1$"+indent+"s";

	byte[] contents = IOUtils.safelyAllocate(len, MAX_RECORD_LENGTH);
	System.arraycopy(docstream,pos,contents,0,len);
	DefaultEscherRecordFactory erf = new HSLFEscherRecordFactory();
	EscherRecord record = erf.createRecord(contents,0);

	// For now, try filling in the fields
	record.fillFields(contents,0,erf);

	long atomType = LittleEndian.getUShort(contents,2);
	// This lacks the 8 byte header size
	long atomLen = LittleEndian.getUShort(contents,4);
	// This (should) include the 8 byte header size
	int recordLen = record.getRecordSize();

    String fmt = ind+"At position %2$d (%2$04x): type is %3$d (%3$04x), len is %4$d (%4$04x) (%5$d) - record claims %6$d";
    out.println(String.format(Locale.ROOT, fmt, "", pos, atomType, atomLen, atomLen+8, recordLen));
	
	
	// Check for corrupt / lying ones
	if(recordLen != 8 && (recordLen != (atomLen+8))) {
		out.println(String.format(Locale.ROOT, ind+"** Atom length of $2d ($3d) doesn't match record length of %4d", "", atomLen, atomLen+8, recordLen));
	}

	// Print the record's details
    String recordStr = record.toString().replace("\n", String.format(Locale.ROOT,  "\n"+ind, ""));
    out.println(String.format(Locale.ROOT, ind+"%2$s", "", recordStr));

    if(record instanceof EscherContainerRecord) {
		walkEscherDDF((indent+3), pos + 8, (int)atomLen );
	}

	// Handle records that seem to lie
	if(atomType == 61451L) {
		// Normally claims a size of 8
		recordLen = (int)atomLen + 8;
	}
	if(atomType == 61453L) {
		// Returns EscherContainerRecord, but really msofbtClientTextbox
		recordLen = (int)atomLen + 8;
		record.fillFields( contents, 0, erf );
		if(! (record instanceof EscherTextboxRecord)) {
			out.println(String.format(Locale.ROOT, ind+"%2$s", "", "** Really a msofbtClientTextbox !"));
		}
	}

	// Decide on what to do, based on how the lengths match up
	if(recordLen == 8 && atomLen > 8 ) {
		// Assume it has children, rather than being corrupted
		walkEscherDDF((indent+3), pos + 8, (int)atomLen );

		// Wind on our length + our header
		pos += atomLen;
		pos += 8;
		len -= atomLen;
		len -= 8;
	} else {
		// No children, wind on our real length
		pos += atomLen;
		pos += 8;
		len -= atomLen;
		len -= 8;
	}

	// Move on to the next one, if we're not at the end yet
	if(len >= 8) {
		walkEscherDDF(indent, pos, len );
	}
  }

  /**
   * Use the basic record format groking code to walk the Escher records
   */
  public void walkEscherBasic(int indent, int pos, int len) throws IOException {
	if(len < 8) { return; }

	final String ind = (indent == 0) ? "%1$s" : "%1$"+indent+"s";

	long type = LittleEndian.getUShort(docstream,pos+2);
	long atomlen = LittleEndian.getUInt(docstream,pos+4);

	String fmt = ind+"At position %2$d ($2$04x): type is %3$d (%3$04x), len is %4$d (%4$04x)";
	out.println(String.format(Locale.ROOT, fmt, "", pos, type, atomlen));

	String typeName = RecordTypes.forTypeID((short)type).name();
	out.println(String.format(Locale.ROOT, ind+"%2$s", "That's an Escher Record: ", typeName));

	// Record specific dumps
	if(type == 61453L) {
		// Text Box. Print out first 8 bytes of data, then 8 4 later
	    HexDump.dump(docstream, 0, out, pos+8, 8);
	    HexDump.dump(docstream, 0, out, pos+20, 8);
		out.println();
	}


	// Blank line before next entry
	out.println();

	// Look in children if we are a container
	if(type == 61443L || type == 61444L) {
		walkEscherBasic((indent+3), pos+8, (int)atomlen);
	}

	// Keep going if not yet at end
	if(atomlen < len) {
		int atomleni = (int)atomlen;
		walkEscherBasic(indent, pos+atomleni+8, len-atomleni-8);
	}
  }
}