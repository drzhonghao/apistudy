

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.ss.formula.EvaluationCell;
import org.apache.poi.ss.formula.EvaluationSheet;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.CellRangeAddress;


final class HSSFEvaluationCell implements EvaluationCell {
	private final EvaluationSheet _evalSheet;

	private final HSSFCell _cell;

	public HSSFEvaluationCell(HSSFCell cell, EvaluationSheet evalSheet) {
		_cell = cell;
		_evalSheet = evalSheet;
	}

	public HSSFEvaluationCell(HSSFCell cell) {
		_cell = null;
		_evalSheet = null;
	}

	@Override
	public Object getIdentityKey() {
		return _cell;
	}

	public HSSFCell getHSSFCell() {
		return _cell;
	}

	@Override
	public boolean getBooleanCellValue() {
		return _cell.getBooleanCellValue();
	}

	@Override
	public CellType getCellType() {
		return _cell.getCellType();
	}

	@Deprecated
	@org.apache.poi.util.Removal(version = "4.2")
	@Override
	public CellType getCellTypeEnum() {
		return getCellType();
	}

	@Override
	public int getColumnIndex() {
		return _cell.getColumnIndex();
	}

	@Override
	public int getErrorCellValue() {
		return _cell.getErrorCellValue();
	}

	@Override
	public double getNumericCellValue() {
		return _cell.getNumericCellValue();
	}

	@Override
	public int getRowIndex() {
		return _cell.getRowIndex();
	}

	@Override
	public EvaluationSheet getSheet() {
		return _evalSheet;
	}

	@Override
	public String getStringCellValue() {
		return _cell.getRichStringCellValue().getString();
	}

	@Override
	public CellRangeAddress getArrayFormulaRange() {
		return _cell.getArrayFormulaRange();
	}

	@Override
	public boolean isPartOfArrayFormulaGroup() {
		return _cell.isPartOfArrayFormulaGroup();
	}

	@Override
	public CellType getCachedFormulaResultType() {
		return _cell.getCachedFormulaResultType();
	}

	@Deprecated
	@org.apache.poi.util.Removal(version = "4.2")
	@Override
	public CellType getCachedFormulaResultTypeEnum() {
		return getCachedFormulaResultType();
	}
}

