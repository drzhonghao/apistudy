

import org.apache.poi.ss.formula.eval.AreaEval;
import org.apache.poi.ss.formula.eval.AreaEvalBase;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.ptg.AreaI;
import org.apache.poi.ss.util.CellReference;


final class LazyAreaEval extends AreaEvalBase {
	public ValueEval getRelativeValue(int relativeRowIndex, int relativeColumnIndex) {
		return getRelativeValue(getFirstSheetIndex(), relativeRowIndex, relativeColumnIndex);
	}

	public ValueEval getRelativeValue(int sheetIndex, int relativeRowIndex, int relativeColumnIndex) {
		int rowIx = relativeRowIndex + (getFirstRow());
		int colIx = relativeColumnIndex + (getFirstColumn());
		return null;
	}

	public AreaEval offset(int relFirstRowIx, int relLastRowIx, int relFirstColIx, int relLastColIx) {
		AreaI area = new AreaI.OffsetArea(getFirstRow(), getFirstColumn(), relFirstRowIx, relLastRowIx, relFirstColIx, relLastColIx);
		return null;
	}

	public LazyAreaEval getRow(int rowIndex) {
		if (rowIndex >= (getHeight())) {
			throw new IllegalArgumentException((((("Invalid rowIndex " + rowIndex) + ".  Allowable range is (0..") + (getHeight())) + ")."));
		}
		int absRowIx = (getFirstRow()) + rowIndex;
		return null;
	}

	public LazyAreaEval getColumn(int columnIndex) {
		if (columnIndex >= (getWidth())) {
			throw new IllegalArgumentException((((("Invalid columnIndex " + columnIndex) + ".  Allowable range is (0..") + (getWidth())) + ")."));
		}
		int absColIx = (getFirstColumn()) + columnIndex;
		return null;
	}

	public String toString() {
		CellReference crA = new CellReference(getFirstRow(), getFirstColumn());
		CellReference crB = new CellReference(getLastRow(), getLastColumn());
		return null;
	}

	public boolean isSubTotal(int rowIndex, int columnIndex) {
		return false;
	}
}

