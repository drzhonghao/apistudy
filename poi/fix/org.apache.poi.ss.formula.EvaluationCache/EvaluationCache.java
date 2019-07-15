

import org.apache.poi.ss.formula.EvaluationCell;
import org.apache.poi.ss.formula.eval.BlankEval;
import org.apache.poi.ss.formula.eval.BoolEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.StringEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.usermodel.CellType;


final class EvaluationCache {
	public void notifyUpdateCell(int bookIndex, int sheetIndex, EvaluationCell cell) {
		int rowIndex = cell.getRowIndex();
		int columnIndex = cell.getColumnIndex();
		if ((cell.getCellType()) == (CellType.FORMULA)) {
		}else {
		}
	}

	private void updateAnyBlankReferencingFormulas(int bookIndex, int sheetIndex, final int rowIndex, final int columnIndex) {
	}

	private boolean areValuesEqual(ValueEval a, ValueEval b) {
		if (a == null) {
			return false;
		}
		Class<?> cls = a.getClass();
		if (cls != (b.getClass())) {
			return false;
		}
		if (a == (BlankEval.instance)) {
			return b == a;
		}
		if (cls == (NumberEval.class)) {
			return (((NumberEval) (a)).getNumberValue()) == (((NumberEval) (b)).getNumberValue());
		}
		if (cls == (StringEval.class)) {
			return ((StringEval) (a)).getStringValue().equals(((StringEval) (b)).getStringValue());
		}
		if (cls == (BoolEval.class)) {
			return (((BoolEval) (a)).getBooleanValue()) == (((BoolEval) (b)).getBooleanValue());
		}
		if (cls == (ErrorEval.class)) {
			return (((ErrorEval) (a)).getErrorCode()) == (((ErrorEval) (b)).getErrorCode());
		}
		throw new IllegalStateException((("Unexpected value class (" + (cls.getName())) + ")"));
	}

	public void clear() {
	}

	public void notifyDeleteCell(int bookIndex, int sheetIndex, EvaluationCell cell) {
		if ((cell.getCellType()) == (CellType.FORMULA)) {
		}else {
		}
	}
}

