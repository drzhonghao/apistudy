

import java.lang.reflect.Method;
import org.apache.poi.hssf.usermodel.HSSFEvaluationWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.CollaboratingWorkbooksEnvironment;
import org.apache.poi.ss.formula.EvaluationWorkbook;
import org.apache.poi.ss.formula.IStabilityClassifier;
import org.apache.poi.ss.formula.WorkbookEvaluator;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.udf.UDFFinder;
import org.apache.poi.ss.usermodel.Workbook;


public final class ForkedEvaluator {
	private WorkbookEvaluator _evaluator;

	private ForkedEvaluator(EvaluationWorkbook masterWorkbook, IStabilityClassifier stabilityClassifier, UDFFinder udfFinder) {
	}

	private static EvaluationWorkbook createEvaluationWorkbook(Workbook wb) {
		if (wb instanceof HSSFWorkbook) {
			return HSSFEvaluationWorkbook.create(((HSSFWorkbook) (wb)));
		}else {
			try {
				Class<?> evalWB = Class.forName("org.apache.poi.xssf.usermodel.XSSFEvaluationWorkbook");
				Class<?> xssfWB = Class.forName("org.apache.poi.xssf.usermodel.XSSFWorkbook");
				Method createM = evalWB.getDeclaredMethod("create", xssfWB);
				return ((EvaluationWorkbook) (createM.invoke(null, wb)));
			} catch (Exception e) {
				throw new IllegalArgumentException((("Unexpected workbook type (" + (wb.getClass().getName())) + ") - check for poi-ooxml and poi-ooxml schemas jar in the classpath"), e);
			}
		}
	}

	public static ForkedEvaluator create(Workbook wb, IStabilityClassifier stabilityClassifier, UDFFinder udfFinder) {
		return new ForkedEvaluator(ForkedEvaluator.createEvaluationWorkbook(wb), stabilityClassifier, udfFinder);
	}

	public void updateCell(String sheetName, int rowIndex, int columnIndex, ValueEval value) {
	}

	public void copyUpdatedCells(Workbook workbook) {
	}

	public ValueEval evaluate(String sheetName, int rowIndex, int columnIndex) {
		return null;
	}

	public static void setupEnvironment(String[] workbookNames, ForkedEvaluator[] evaluators) {
		WorkbookEvaluator[] wbEvals = new WorkbookEvaluator[evaluators.length];
		for (int i = 0; i < (wbEvals.length); i++) {
			wbEvals[i] = evaluators[i]._evaluator;
		}
		CollaboratingWorkbooksEnvironment.setup(workbookNames, wbEvals);
	}
}

