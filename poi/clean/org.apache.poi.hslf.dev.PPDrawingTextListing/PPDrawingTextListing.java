import org.apache.poi.hslf.dev.*;


import java.io.IOException;

import org.apache.poi.hslf.record.EscherTextboxWrapper;
import org.apache.poi.hslf.record.PPDrawing;
import org.apache.poi.hslf.record.Record;
import org.apache.poi.hslf.record.TextBytesAtom;
import org.apache.poi.hslf.record.TextCharsAtom;
import org.apache.poi.hslf.usermodel.HSLFSlideShowImpl;


/**
 * Uses record level code to locate PPDrawing entries.
 * Having found them, it sees if they have DDF Textbox records, and if so,
 *  searches those for text. Prints out any text it finds
 */
public final class PPDrawingTextListing {
	public static void main(String[] args) throws IOException {
		if(args.length < 1) {
			System.err.println("Need to give a filename");
			System.exit(1);
		}

		HSLFSlideShowImpl ss = new HSLFSlideShowImpl(args[0]);

		// Find PPDrawings at any second level position
		Record[] records = ss.getRecords();
		for(int i=0; i<records.length; i++) {
			Record[] children = records[i].getChildRecords();
			if(children != null && children.length != 0) {
				for(int j=0; j<children.length; j++) {
					if(children[j] instanceof PPDrawing) {
						System.out.println("Found PPDrawing at " + j + " in top level record " + i + " (" + records[i].getRecordType() + ")" );

						// Look for EscherTextboxWrapper's
						PPDrawing ppd = (PPDrawing)children[j];
						EscherTextboxWrapper[] wrappers = ppd.getTextboxWrappers();
						System.out.println("  Has " + wrappers.length + " textbox wrappers within");

						// Loop over the wrappers, showing what they contain
						for(int k=0; k<wrappers.length; k++) {
							EscherTextboxWrapper tbw = wrappers[k];
							System.out.println("    " + k + " has " + tbw.getChildRecords().length + " PPT atoms within");

							// Loop over the records, printing the text
							Record[] pptatoms = tbw.getChildRecords();
							for(int l=0; l<pptatoms.length; l++) {
								String text = null;
								if(pptatoms[l] instanceof TextBytesAtom) {
									TextBytesAtom tba = (TextBytesAtom)pptatoms[l];
									text = tba.getText();
								}
								if(pptatoms[l] instanceof TextCharsAtom) {
									TextCharsAtom tca = (TextCharsAtom)pptatoms[l];
									text = tca.getText();
								}

								if(text != null) {
									text = text.replace('\r','\n');
									System.out.println("        ''" + text + "''");
								}
							}
						}
					}
				}
			}
		}
		
		ss.close();
	}
}
