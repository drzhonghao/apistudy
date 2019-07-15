

import java.util.Map;
import org.apache.poi.ss.formula.CollaboratingWorkbooksEnvironment;
import org.apache.poi.ss.formula.EvaluationWorkbook;
import org.apache.poi.ss.formula.WorkbookEvaluator;
import org.apache.poi.ss.formula.WorkbookEvaluatorProvider;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;


public abstract class BaseFormulaEvaluator implements WorkbookEvaluatorProvider , FormulaEvaluator {
	protected final WorkbookEvaluator _bookEvaluator;

	protected BaseFormulaEvaluator(WorkbookEvaluator bookEvaluator) {
		this._bookEvaluator = bookEvaluator;
	}

	public static void setupEnvironment(String[] workbookNames, BaseFormulaEvaluator[] evaluators) {
		WorkbookEvaluator[] wbEvals = new WorkbookEvaluator[evaluators.length];
		for (int i = 0; i < (wbEvals.length); i++) {
			wbEvals[i] = evaluators[i]._bookEvaluator;
		}
		CollaboratingWorkbooksEnvironment.setup(workbookNames, wbEvals);
	}

	@Override
	public void setupReferencedWorkbooks(Map<String, FormulaEvaluator> evaluators) {
		CollaboratingWorkbooksEnvironment.setupFormulaEvaluator(evaluators);
	}

	@Override
	public WorkbookEvaluator _getWorkbookEvaluator() {
		return _bookEvaluator;
	}

	protected EvaluationWorkbook getEvaluationWorkbook() {
		return null;
	}

	@Override
	public void clearAllCachedResultValues() {
		_bookEvaluator.clearAllCachedResultValues();
	}

	@Override
	public CellValue evaluate(Cell cell) {
		if (cell == null) {
			return null;
		}
		switch (cell.getCellType()) {
			case BOOLEAN :
				return CellValue.valueOf(cell.getBooleanCellValue());
			case ERROR :
				return CellValue.getError(cell.getErrorCellValue());
			case FORMULA :
				return evaluateFormulaCellValue(cell);
			case NUMERIC :
				return new CellValue(cell.getNumericCellValue());
			case STRING :
				return new CellValue(cell.getRichStringCellValue().getString());
			case BLANK :
				return null;
			default :
				throw new IllegalStateException((("Bad cell type (" + (cell.getCellType())) + ")"));
		}
	}

	@Override
	public Cell evaluateInCell(Cell cell) {
		if (cell == null) {
			return null;
		}
		if ((cell.getCellType()) == (CellType.FORMULA)) {
			CellValue cv = evaluateFormulaCellValue(cell);
			setCellValue(cell, cv);
			setCellType(cell, cv);
			setCellValue(cell, cv);
		}
		return cell;
	}

	protected abstract CellValue evaluateFormulaCellValue(Cell cell);

	@Override
	public CellType evaluateFormulaCell(Cell cell) {
		if ((cell == null) || ((cell.getCellType()) != (CellType.FORMULA))) {
			return CellType._NONE;
		}
		CellValue cv = evaluateFormulaCellValue(cell);
		setCellValue(cell, cv);
		return cv.getCellType();
	}

	@Deprecated
	@org.apache.poi.util.Removal(version = "4.2")
	@Override
	public CellType evaluateFormulaCellEnum(Cell cell) {
		return evaluateFormulaCell(cell);
	}

	protected void setCellType(Cell cell, CellValue cv) {
		CellType cellType = cv.getCellType();
		switch (cellType) {
			case BOOLEAN :
			case ERROR :
			case NUMERIC :
			case STRING :
				setCellType(cell, cellType);
				return;
			case BLANK :
				throw new IllegalArgumentException("This should never happen. Blanks eventually get translated to zero.");
			case FORMULA :
				throw new IllegalArgumentException("This should never happen. Formulas should have already been evaluated.");
			default :
				throw new IllegalStateException((("Unexpected cell value type (" + cellType) + ")"));
		}
	}

	protected void setCellType(Cell cell, CellType cellType) {
		cell.setCellType(cellType);
	}

	protected abstract RichTextString createRichTextString(String str);

	protected void setCellValue(Cell cell, CellValue cv) {
		CellType cellType = cv.getCellType();
		switch (cellType) {
			case BOOLEAN :
				cell.setCellValue(cv.getBooleanValue());
				break;
			case ERROR :
				cell.setCellErrorValue(cv.getErrorValue());
				break;
			case NUMERIC :
				cell.setCellValue(cv.getNumberValue());
				break;
			case STRING :
				cell.setCellValue(createRichTextString(cv.getStringValue()));
				break;
			case BLANK :
			case FORMULA :
			default :
				throw new IllegalStateException((("Unexpected cell value type (" + cellType) + ")"));
		}
	}

	public static void evaluateAllFormulaCells(Workbook wb) {
		FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
		BaseFormulaEvaluator.evaluateAllFormulaCells(wb, evaluator);
	}

	protected static void evaluateAllFormulaCells(Workbook wb, FormulaEvaluator evaluator) {
		for (int i = 0; i < (wb.getNumberOfSheets()); i++) {
			Sheet sheet = wb.getSheetAt(i);
			for (Row r : sheet) {
				for (Cell c : r) {
					if ((c.getCellType()) == (CellType.FORMULA)) {
						evaluator.evaluateFormulaCell(c);
					}
				}
			}
		}
	}

	@Override
	public void setIgnoreMissingWorkbooks(boolean ignore) {
		_bookEvaluator.setIgnoreMissingWorkbooks(ignore);
	}

	@Override
	public void setDebugEvaluationOutputForNextEval(boolean value) {
		_bookEvaluator.setDebugEvaluationOutputForNextEval(value);
	}
}

