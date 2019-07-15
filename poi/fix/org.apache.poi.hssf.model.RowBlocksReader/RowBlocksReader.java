

import java.util.ArrayList;
import java.util.List;
import org.apache.poi.hssf.model.RecordStream;
import org.apache.poi.hssf.record.ArrayRecord;
import org.apache.poi.hssf.record.MergeCellsRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.SharedFormulaRecord;
import org.apache.poi.hssf.record.TableRecord;
import org.apache.poi.hssf.record.aggregates.SharedValueManager;
import org.apache.poi.ss.util.CellReference;


public final class RowBlocksReader {
	private final List<Record> _plainRecords;

	private final SharedValueManager _sfm;

	private final MergeCellsRecord[] _mergedCellsRecords;

	public RowBlocksReader(RecordStream rs) {
		List<Record> plainRecords = new ArrayList<>();
		List<Record> shFrmRecords = new ArrayList<>();
		List<CellReference> firstCellRefs = new ArrayList<>();
		List<Record> arrayRecords = new ArrayList<>();
		List<Record> tableRecords = new ArrayList<>();
		List<Record> mergeCellRecords = new ArrayList<>();
		Record prevRec = null;
		SharedFormulaRecord[] sharedFormulaRecs = new SharedFormulaRecord[shFrmRecords.size()];
		CellReference[] firstCells = new CellReference[firstCellRefs.size()];
		ArrayRecord[] arrayRecs = new ArrayRecord[arrayRecords.size()];
		TableRecord[] tableRecs = new TableRecord[tableRecords.size()];
		shFrmRecords.toArray(sharedFormulaRecs);
		firstCellRefs.toArray(firstCells);
		arrayRecords.toArray(arrayRecs);
		tableRecords.toArray(tableRecs);
		_plainRecords = plainRecords;
		_sfm = SharedValueManager.create(sharedFormulaRecs, firstCells, arrayRecs, tableRecs);
		_mergedCellsRecords = new MergeCellsRecord[mergeCellRecords.size()];
		mergeCellRecords.toArray(_mergedCellsRecords);
	}

	public MergeCellsRecord[] getLooseMergedCells() {
		return _mergedCellsRecords;
	}

	public SharedValueManager getSharedFormulaManager() {
		return _sfm;
	}

	public RecordStream getPlainRecordStream() {
		return new RecordStream(_plainRecords, 0);
	}
}

