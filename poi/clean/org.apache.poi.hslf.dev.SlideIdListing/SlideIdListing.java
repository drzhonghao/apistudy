import org.apache.poi.hslf.dev.*;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.poi.hslf.record.Document;
import org.apache.poi.hslf.record.Notes;
import org.apache.poi.hslf.record.NotesAtom;
import org.apache.poi.hslf.record.PersistPtrHolder;
import org.apache.poi.hslf.record.PositionDependentRecord;
import org.apache.poi.hslf.record.Record;
import org.apache.poi.hslf.record.Slide;
import org.apache.poi.hslf.record.SlideAtom;
import org.apache.poi.hslf.record.SlideListWithText;
import org.apache.poi.hslf.record.SlidePersistAtom;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFSlideShowImpl;
import org.apache.poi.util.LittleEndian;

/**
 * Gets all the different things that have Slide IDs (of sorts)
 *  in them, and displays them, so you can try to guess what they
 *  all mean
 */
public final class SlideIdListing {
	private static byte[] fileContents;

	public static void main(String[] args) throws IOException {
		if(args.length < 1) {
			System.err.println("Need to give a filename");
			System.exit(1);
		}


		// Create the slideshow object, for normal working with
		HSLFSlideShowImpl hss = new HSLFSlideShowImpl(args[0]);
		HSLFSlideShow ss = new HSLFSlideShow(hss);

		// Grab the base contents
		fileContents = hss.getUnderlyingBytes();
		Record[] records = hss.getRecords();
		Record[] latestRecords = ss.getMostRecentCoreRecords();

		// Grab any records that interest us
		Document document = null;
		for(int i=0; i<latestRecords.length; i++) {
			if(latestRecords[i] instanceof Document) {
				document = (Document)latestRecords[i];
			}
		}

		System.out.println("");


		// Look for SlidePersistAtoms, and report what they have to
		//  say about possible slide IDs
		SlideListWithText[] slwts = document.getSlideListWithTexts();
		for(int i=0; i<slwts.length; i++) {
			Record[] cr = slwts[i].getChildRecords();
			for(int j=0; j<cr.length; j++) {
				if(cr[j] instanceof SlidePersistAtom) {
					SlidePersistAtom spa = (SlidePersistAtom)cr[j];
					System.out.println("SlidePersistAtom knows about slide:");
					System.out.println("\t" + spa.getRefID());
					System.out.println("\t" + spa.getSlideIdentifier());
				}
			}
		}

		System.out.println("");

		// Look for latest core records that are slides or notes
		for(int i=0; i<latestRecords.length; i++) {
			if(latestRecords[i] instanceof Slide) {
				Slide s = (Slide)latestRecords[i];
				SlideAtom sa = s.getSlideAtom();
				System.out.println("Found the latest version of a slide record:");
				System.out.println("\tCore ID is " + s.getSheetId());
				System.out.println("\t(Core Records count is " + i + ")");
				System.out.println("\tDisk Position is " + s.getLastOnDiskOffset());
				System.out.println("\tMaster ID is " + sa.getMasterID());
				System.out.println("\tNotes ID is " + sa.getNotesID());
			}
		}
		System.out.println("");
		for(int i=0; i<latestRecords.length; i++) {
			if(latestRecords[i] instanceof Notes) {
				Notes n = (Notes)latestRecords[i];
				NotesAtom na = n.getNotesAtom();
				System.out.println("Found the latest version of a notes record:");
				System.out.println("\tCore ID is " + n.getSheetId());
				System.out.println("\t(Core Records count is " + i + ")");
				System.out.println("\tDisk Position is " + n.getLastOnDiskOffset());
				System.out.println("\tMatching slide is " + na.getSlideID());
			}
		}

		System.out.println("");

		// Find any persist ones first
		int pos = 0;
		for(int i=0; i<records.length; i++) {
			Record r = records[i];

			if(r.getRecordType() == 6001l) {
				// PersistPtrFullBlock
				System.out.println("Found PersistPtrFullBlock at " + pos + " (" + Integer.toHexString(pos) + ")");
			}
			if(r.getRecordType() == 6002l) {
				// PersistPtrIncrementalBlock
				System.out.println("Found PersistPtrIncrementalBlock at " + pos + " (" + Integer.toHexString(pos) + ")");
				PersistPtrHolder pph = (PersistPtrHolder)r;

				// Check the sheet offsets
				int[] sheetIDs = pph.getKnownSlideIDs();
				Map<Integer,Integer> sheetOffsets = pph.getSlideLocationsLookup();
				for(int j=0; j<sheetIDs.length; j++) {
					Integer id = sheetIDs[j];
					Integer offset = sheetOffsets.get(id);

					System.out.println("  Knows about sheet " + id);
					System.out.println("    That sheet lives at " + offset);

					Record atPos = findRecordAtPos(offset.intValue());
					System.out.println("    The record at that pos is of type " + atPos.getRecordType());
					System.out.println("    The record at that pos has class " + atPos.getClass().getName());

					if(! (atPos instanceof PositionDependentRecord)) {
						System.out.println("    ** The record class isn't position aware! **");
					}
				}
			}

			// Increase the position by the on disk size
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			r.writeOut(baos);
			pos += baos.size();
		}

		ss.close();
		
		System.out.println("");
	}


	// Finds the record at a given position
	public static Record findRecordAtPos(int pos) {
		long type = LittleEndian.getUShort(fileContents, pos+2);
		long rlen = LittleEndian.getUInt(fileContents, pos+4);

        return Record.createRecordForType(type,fileContents,pos,(int)rlen+8);
	}
}
