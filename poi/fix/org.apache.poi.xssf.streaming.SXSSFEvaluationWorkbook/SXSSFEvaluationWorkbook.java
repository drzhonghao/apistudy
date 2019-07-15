

import org.apache.poi.ss.formula.EvaluationCell;
import org.apache.poi.ss.formula.EvaluationSheet;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.util.Internal;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.BaseXSSFEvaluationWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


@Internal
public final class SXSSFEvaluationWorkbook extends BaseXSSFEvaluationWorkbook {
	private final SXSSFWorkbook _uBook;

	public static SXSSFEvaluationWorkbook create(SXSSFWorkbook book) {
		if (book == null) {
			return null;
		}
		return new SXSSFEvaluationWorkbook(book);
	}

	private SXSSFEvaluationWorkbook(SXSSFWorkbook book) {
		super(book.getXSSFWorkbook());
		_uBook = book;
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

