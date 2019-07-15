import org.apache.poi.hslf.dev.*;


import java.io.IOException;

import org.apache.poi.hslf.record.Notes;
import org.apache.poi.hslf.record.NotesAtom;
import org.apache.poi.hslf.record.Record;
import org.apache.poi.hslf.record.Slide;
import org.apache.poi.hslf.record.SlideAtom;
import org.apache.poi.hslf.usermodel.HSLFSlideShowImpl;


/**
 * Uses record level code to locate Notes and Slide records.
 * Having found them, it asks their SlideAtom or NotesAtom entries
 *  what they are all about. Useful for checking the matching between
 *  Slides, Master Slides and Notes
 */
public final class SlideAndNotesAtomListing {
	public static void main(String[] args) throws IOException {
		if(args.length < 1) {
			System.err.println("Need to give a filename");
			System.exit(1);
		}

		HSLFSlideShowImpl ss = new HSLFSlideShowImpl(args[0]);
		System.out.println("");

		// Find either Slides or Notes
		Record[] records = ss.getRecords();
		for(int i=0; i<records.length; i++) {
			Record r = records[i];

			// When we find them, print out their IDs
			if(r instanceof Slide) {
				Slide s = (Slide)r;
				SlideAtom sa = s.getSlideAtom();
				System.out.println("Found Slide at " + i);
				System.out.println("  Slide's master ID is " + sa.getMasterID());
				System.out.println("  Slide's notes ID is  " + sa.getNotesID());
				System.out.println("");
			}
			if(r instanceof Notes) {
				Notes n = (Notes)r;
				NotesAtom na = n.getNotesAtom();
				System.out.println("Found Notes at " + i);
				System.out.println("  Notes ID is " + na.getSlideID());
				System.out.println("");
			}
		}
		
		ss.close();
	}
}
