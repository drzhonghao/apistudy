

import org.apache.poi.ss.formula.eval.AreaEval;
import org.apache.poi.ss.formula.eval.RefEvalBase;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.ptg.AreaI;
import org.apache.poi.ss.util.CellReference;


public final class LazyRefEval extends RefEvalBase {
	public ValueEval getInnerValueEval(int sheetIndex) {
		return null;
	}

	public AreaEval offset(int relFirstRowIx, int relLastRowIx, int relFirstColIx, int relLastColIx) {
		AreaI area = new AreaI.OffsetArea(getRow(), getColumn(), relFirstRowIx, relLastRowIx, relFirstColIx, relLastColIx);
		return null;
	}

	public boolean isSubTotal() {
		return false;
	}

	public String toString() {
		CellReference cr = new CellReference(getRow(), getColumn());
		return null;
	}
}

