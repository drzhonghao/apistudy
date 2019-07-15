

import org.apache.poi.ss.formula.EvaluationCell;
import org.apache.poi.ss.formula.EvaluationSheet;
import org.apache.poi.ss.formula.WorkbookEvaluator;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.usermodel.CellType;


final class SheetRefEvaluator {
	private final WorkbookEvaluator _bookEvaluator = null;

	private final int _sheetIndex = 0;

	private EvaluationSheet _sheet;

	public String getSheetName() {
		return null;
	}

	public ValueEval getEvalForCell(int rowIndex, int columnIndex) {
		return null;
	}

	private EvaluationSheet getSheet() {
		if ((_sheet) == null) {
		}
		return _sheet;
	}

	public boolean isSubTotal(int rowIndex, int columnIndex) {
		boolean subtotal = false;
		EvaluationCell cell = getSheet().getCell(rowIndex, columnIndex);
		if ((cell != null) && ((cell.getCellType()) == (CellType.FORMULA))) {
		}
		return subtotal;
	}
}

