

import java.util.Map;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFEvaluationWorkbook;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.BaseFormulaEvaluator;
import org.apache.poi.ss.formula.CollaboratingWorkbooksEnvironment;
import org.apache.poi.ss.formula.IStabilityClassifier;
import org.apache.poi.ss.formula.WorkbookEvaluator;
import org.apache.poi.ss.formula.udf.UDFFinder;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Workbook;


public class HSSFFormulaEvaluator extends BaseFormulaEvaluator {
	private final HSSFWorkbook _book;

	public HSSFFormulaEvaluator(HSSFWorkbook workbook) {
		this(workbook, null);
	}

	public HSSFFormulaEvaluator(HSSFWorkbook workbook, IStabilityClassifier stabilityClassifier) {
		this(workbook, stabilityClassifier, null);
	}

	private HSSFFormulaEvaluator(HSSFWorkbook workbook, IStabilityClassifier stabilityClassifier, UDFFinder udfFinder) {
		super(new WorkbookEvaluator(HSSFEvaluationWorkbook.create(workbook), stabilityClassifier, udfFinder));
		_book = workbook;
	}

	public static HSSFFormulaEvaluator create(HSSFWorkbook workbook, IStabilityClassifier stabilityClassifier, UDFFinder udfFinder) {
		return new HSSFFormulaEvaluator(workbook, stabilityClassifier, udfFinder);
	}

	@Override
	protected RichTextString createRichTextString(String str) {
		return new HSSFRichTextString(str);
	}

	public static void setupEnvironment(String[] workbookNames, HSSFFormulaEvaluator[] evaluators) {
		BaseFormulaEvaluator.setupEnvironment(workbookNames, evaluators);
	}

	@Override
	public void setupReferencedWorkbooks(Map<String, FormulaEvaluator> evaluators) {
		CollaboratingWorkbooksEnvironment.setupFormulaEvaluator(evaluators);
	}

	public void notifyUpdateCell(HSSFCell cell) {
	}

	@Override
	public void notifyUpdateCell(Cell cell) {
	}

	public void notifyDeleteCell(HSSFCell cell) {
	}

	@Override
	public void notifyDeleteCell(Cell cell) {
	}

	@Override
	public void notifySetFormula(Cell cell) {
	}

	@Override
	public HSSFCell evaluateInCell(Cell cell) {
		return ((HSSFCell) (super.evaluateInCell(cell)));
	}

	public static void evaluateAllFormulaCells(HSSFWorkbook wb) {
		BaseFormulaEvaluator.evaluateAllFormulaCells(wb, new HSSFFormulaEvaluator(wb));
	}

	public static void evaluateAllFormulaCells(Workbook wb) {
		BaseFormulaEvaluator.evaluateAllFormulaCells(wb);
	}

	@Override
	public void evaluateAll() {
		BaseFormulaEvaluator.evaluateAllFormulaCells(_book, this);
	}

	protected CellValue evaluateFormulaCellValue(Cell cell) {
		return null;
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

