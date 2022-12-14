import org.apache.poi.hssf.record.aggregates.*;


import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.model.RecordStream;
import org.apache.poi.hssf.record.CFHeader12Record;
import org.apache.poi.hssf.record.CFHeaderRecord;
import org.apache.poi.ss.formula.FormulaShifter;

/**
 * Holds all the conditional formatting for a workbook sheet.<p>
 * 
 * See OOO exelfileformat.pdf sec 4.12 'Conditional Formatting Table'
 */
public final class ConditionalFormattingTable extends RecordAggregate {
	private final List<CFRecordsAggregate> _cfHeaders;

	/**
	 * Creates an empty ConditionalFormattingTable
	 */
	public ConditionalFormattingTable() {
		_cfHeaders = new ArrayList<>();
	}

	public ConditionalFormattingTable(RecordStream rs) {

		List<CFRecordsAggregate> temp = new ArrayList<>();
		while (rs.peekNextClass() == CFHeaderRecord.class ||
		       rs.peekNextClass() == CFHeader12Record.class) {
			temp.add(CFRecordsAggregate.createCFAggregate(rs));
		}
		_cfHeaders = temp;
	}

	public void visitContainedRecords(RecordVisitor rv) {
		for (CFRecordsAggregate subAgg : _cfHeaders) {
			subAgg.visitContainedRecords(rv);
		}
	}

	/**
	 * @return index of the newly added CF header aggregate
	 */
	public int add(CFRecordsAggregate cfAggregate) {
	    cfAggregate.getHeader().setID(_cfHeaders.size());
		_cfHeaders.add(cfAggregate);
		return _cfHeaders.size() - 1;
	}

	public int size() {
		return _cfHeaders.size();
	}

	public CFRecordsAggregate get(int index) {
		checkIndex(index);
		return _cfHeaders.get(index);
	}

	public void remove(int index) {
		checkIndex(index);
		_cfHeaders.remove(index);
	}

	private void checkIndex(int index) {
		if (index < 0 || index >= _cfHeaders.size()) {
			throw new IllegalArgumentException("Specified CF index " + index
					+ " is outside the allowable range (0.." + (_cfHeaders.size() - 1) + ")");
		}
	}

	public void updateFormulasAfterCellShift(FormulaShifter shifter, int externSheetIndex) {
		for (int i = 0; i < _cfHeaders.size(); i++) {
			CFRecordsAggregate subAgg = _cfHeaders.get(i);
			boolean shouldKeep = subAgg.updateFormulasAfterCellShift(shifter, externSheetIndex);
			if (!shouldKeep) {
				_cfHeaders.remove(i);
				i--;
			}
		}
	}
}
