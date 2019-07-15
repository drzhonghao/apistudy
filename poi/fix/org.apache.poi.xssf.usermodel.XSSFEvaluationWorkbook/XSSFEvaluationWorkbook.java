

import org.apache.poi.ss.formula.EvaluationCell;
import org.apache.poi.ss.formula.EvaluationSheet;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.util.Internal;
import org.apache.poi.xssf.usermodel.BaseXSSFEvaluationWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


@Internal
public final class XSSFEvaluationWorkbook extends BaseXSSFEvaluationWorkbook {
	public static XSSFEvaluationWorkbook create(XSSFWorkbook book) {
		if (book == null) {
			return null;
		}
		return new XSSFEvaluationWorkbook(book);
	}

	private XSSFEvaluationWorkbook(XSSFWorkbook book) {
		super(book);
	}

	@Override
	public void clearAllCachedResultValues() {
		super.clearAllCachedResultValues();
	}

	@Override
	public int getSheetIndex(EvaluationSheet evalSheet) {
		return 0;
	}

	@Override
	public EvaluationSheet getSheet(int sheetIndex) {
		return null;
	}

	@Override
	public Ptg[] getFormulaTokens(EvaluationCell evalCell) {
		return null;
	}
}

