import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.eventusermodel.*;


import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.model.HSSFFormulaParser;
import org.apache.poi.hssf.model.InternalWorkbook;
import org.apache.poi.hssf.record.BoundSheetRecord;
import org.apache.poi.hssf.record.EOFRecord;
import org.apache.poi.hssf.record.ExternSheetRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.SSTRecord;
import org.apache.poi.hssf.record.SupBookRecord;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/**
 * When working with the EventUserModel, if you want to
 *  process formulas, you need an instance of
 *  {@link InternalWorkbook} to pass to a {@link HSSFWorkbook},
 *  to finally give to {@link HSSFFormulaParser},
 *  and this will build you stub ones.
 * Since you're working with the EventUserModel, you
 *  wouldn't want to get a full {@link InternalWorkbook} and
 *  {@link HSSFWorkbook}, as they would eat too much memory.
 *  Instead, you should collect a few key records as they
 *  go past, then call this once you have them to build a
 *  stub {@link InternalWorkbook}, and from that a stub
 *  {@link HSSFWorkbook}, to use with the {@link HSSFFormulaParser}.
 *
 * The records you should collect are:
 *  * {@link ExternSheetRecord}
 *  * {@link BoundSheetRecord}
 * You should probably also collect {@link SSTRecord},
 *  but it's not required to pass this in.
 *
 * To help, this class includes a HSSFListener wrapper
 *  that will do the collecting for you.
 */
public class EventWorkbookBuilder {


	/**
	 * Creates a stub Workbook from the supplied records,
	 *  suitable for use with the {@link HSSFFormulaParser}
	 * @param externs The ExternSheetRecords in your file
	 * @param bounds The BoundSheetRecords in your file
	 * @param sst The SSTRecord in your file.
	 * @return A stub Workbook suitable for use with {@link HSSFFormulaParser}
	 */
	public static InternalWorkbook createStubWorkbook(ExternSheetRecord[] externs,
			BoundSheetRecord[] bounds, SSTRecord sst) {
		List<Record> wbRecords = new ArrayList<>();

		// Core Workbook records go first
		if(bounds != null) {
			for (BoundSheetRecord bound : bounds) {
				wbRecords.add(bound);
			}
		}
		if(sst != null) {
			wbRecords.add(sst);
		}

		// Now we can have the ExternSheetRecords,
		//  preceded by a SupBookRecord
		if(externs != null) {
			wbRecords.add(SupBookRecord.createInternalReferences(
					(short)externs.length));
			for (ExternSheetRecord extern : externs) {
				wbRecords.add(extern);
			}
		}

		// Finally we need an EoF record
		wbRecords.add(EOFRecord.instance);

		return InternalWorkbook.createWorkbook(wbRecords);
	}

	/**
	 * Creates a stub workbook from the supplied records,
	 *  suitable for use with the {@link HSSFFormulaParser}
	 * @param externs The ExternSheetRecords in your file
	 * @param bounds The BoundSheetRecords in your file
	 * @return A stub Workbook suitable for use with {@link HSSFFormulaParser}
	 */
	public static InternalWorkbook createStubWorkbook(ExternSheetRecord[] externs,
			BoundSheetRecord[] bounds) {
		return createStubWorkbook(externs, bounds, null);
	}


	/**
	 * A wrapping HSSFListener which will collect
	 *  {@link BoundSheetRecord}s and {@link ExternSheetRecord}s as
	 *  they go past, so you can create a Stub {@link InternalWorkbook} from
	 *  them once required.
	 */
	public static class SheetRecordCollectingListener implements HSSFListener {
		private final HSSFListener childListener;
		private final List<BoundSheetRecord> boundSheetRecords = new ArrayList<>();
		private final List<ExternSheetRecord> externSheetRecords = new ArrayList<>();
		private SSTRecord sstRecord;

		public SheetRecordCollectingListener(HSSFListener childListener) {
			this.childListener = childListener;
		}


		public BoundSheetRecord[] getBoundSheetRecords() {
			return boundSheetRecords.toArray(
					new BoundSheetRecord[boundSheetRecords.size()]
			);
		}
		public ExternSheetRecord[] getExternSheetRecords() {
			return externSheetRecords.toArray(
					new ExternSheetRecord[externSheetRecords.size()]
			);
		}
		public SSTRecord getSSTRecord() {
			return sstRecord;
		}

		public HSSFWorkbook getStubHSSFWorkbook() {
		    // Create a base workbook
		    HSSFWorkbook wb = HSSFWorkbook.create(getStubWorkbook());
		    // Stub the sheets, so sheet name lookups work
		    for (BoundSheetRecord bsr : boundSheetRecords) {
		        wb.createSheet(bsr.getSheetname());
		    }
		    // Ready for Formula use!
		    return wb;
		}
		public InternalWorkbook getStubWorkbook() {
			return createStubWorkbook(
					getExternSheetRecords(), getBoundSheetRecords(),
					getSSTRecord()
			);
		}


		/**
		 * Process this record ourselves, and then
		 *  pass it on to our child listener
		 */
		@Override
        public void processRecord(Record record) {
			// Handle it ourselves
			processRecordInternally(record);

			// Now pass on to our child
			childListener.processRecord(record);
		}

		/**
		 * Process the record ourselves, but do not
		 *  pass it on to the child Listener.
		 *  
		 * @param record the record to be processed
		 */
		public void processRecordInternally(Record record) {
			if(record instanceof BoundSheetRecord) {
				boundSheetRecords.add((BoundSheetRecord)record);
			}
			else if(record instanceof ExternSheetRecord) {
				externSheetRecords.add((ExternSheetRecord)record);
			}
			else if(record instanceof SSTRecord) {
				sstRecord = (SSTRecord)record;
			}
		}
	}
}
