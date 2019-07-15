import org.apache.poi.hssf.record.aggregates.*;


import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.model.RecordStream;
import org.apache.poi.hssf.record.DVALRecord;
import org.apache.poi.hssf.record.DVRecord;

/**
 * Manages the DVALRecord and DVRecords for a single sheet<p>
 * See OOO excelfileformat.pdf section 4.14
 */
public final class DataValidityTable extends RecordAggregate {

	private final DVALRecord _headerRec;
	/**
	 * The list of data validations for the current sheet.
	 * Note - this may be empty (contrary to OOO documentation)
	 */
	private final List<DVRecord> _validationList;

	public DataValidityTable(RecordStream rs) {
		_headerRec = (DVALRecord) rs.getNext();
		List<DVRecord> temp = new ArrayList<>();
		while (rs.peekNextClass() == DVRecord.class) {
			temp.add((DVRecord) rs.getNext());
		}
		_validationList = temp;
	}

	public DataValidityTable() {
		_headerRec = new DVALRecord();
		_validationList = new ArrayList<>();
	}

	public void visitContainedRecords(RecordVisitor rv) {
		if (_validationList.isEmpty()) {
			return;
		}
		rv.visitRecord(_headerRec);
		for (int i = 0; i < _validationList.size(); i++) {
			rv.visitRecord(_validationList.get(i));
		}
	}

	public void addDataValidation(DVRecord dvRecord) {
		_validationList.add(dvRecord);
		_headerRec.setDVRecNo(_validationList.size());
	}
}
