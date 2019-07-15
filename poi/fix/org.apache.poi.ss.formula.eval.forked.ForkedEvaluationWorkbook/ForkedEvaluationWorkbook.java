

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.EvaluationCell;
import org.apache.poi.ss.formula.EvaluationName;
import org.apache.poi.ss.formula.EvaluationSheet;
import org.apache.poi.ss.formula.EvaluationWorkbook;
import org.apache.poi.ss.formula.ptg.NamePtg;
import org.apache.poi.ss.formula.ptg.NameXPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.udf.UDFFinder;
import org.apache.poi.util.Internal;


@Internal
final class ForkedEvaluationWorkbook implements EvaluationWorkbook {
	private final EvaluationWorkbook _masterBook;

	public ForkedEvaluationWorkbook(EvaluationWorkbook master) {
		_masterBook = master;
	}

	public EvaluationCell getEvaluationCell(String sheetName, int rowIndex, int columnIndex) {
		return null;
	}

	@Override
	public int convertFromExternSheetIndex(int externSheetIndex) {
		return _masterBook.convertFromExternSheetIndex(externSheetIndex);
	}

	@Override
	public EvaluationWorkbook.ExternalSheet getExternalSheet(int externSheetIndex) {
		return _masterBook.getExternalSheet(externSheetIndex);
	}

	@Override
	public EvaluationWorkbook.ExternalSheet getExternalSheet(String firstSheetName, String lastSheetName, int externalWorkbookNumber) {
		return _masterBook.getExternalSheet(firstSheetName, lastSheetName, externalWorkbookNumber);
	}

	@Override
	public Ptg[] getFormulaTokens(EvaluationCell cell) {
		return _masterBook.getFormulaTokens(cell);
	}

	@Override
	public EvaluationName getName(NamePtg namePtg) {
		return _masterBook.getName(namePtg);
	}

	@Override
	public EvaluationName getName(String name, int sheetIndex) {
		return _masterBook.getName(name, sheetIndex);
	}

	@Override
	public EvaluationSheet getSheet(int sheetIndex) {
		return null;
	}

	@Override
	public EvaluationWorkbook.ExternalName getExternalName(int externSheetIndex, int externNameIndex) {
		return _masterBook.getExternalName(externSheetIndex, externNameIndex);
	}

	@Override
	public EvaluationWorkbook.ExternalName getExternalName(String nameName, String sheetName, int externalWorkbookNumber) {
		return _masterBook.getExternalName(nameName, sheetName, externalWorkbookNumber);
	}

	@Override
	public int getSheetIndex(EvaluationSheet sheet) {
		return _masterBook.getSheetIndex(sheet);
	}

	@Override
	public int getSheetIndex(String sheetName) {
		return _masterBook.getSheetIndex(sheetName);
	}

	@Override
	public String getSheetName(int sheetIndex) {
		return _masterBook.getSheetName(sheetIndex);
	}

	@Override
	public String resolveNameXText(NameXPtg ptg) {
		return _masterBook.resolveNameXText(ptg);
	}

	@Override
	public UDFFinder getUDFFinder() {
		return _masterBook.getUDFFinder();
	}

	public SpreadsheetVersion getSpreadsheetVersion() {
		return _masterBook.getSpreadsheetVersion();
	}

	@Override
	public void clearAllCachedResultValues() {
		_masterBook.clearAllCachedResultValues();
	}
}

