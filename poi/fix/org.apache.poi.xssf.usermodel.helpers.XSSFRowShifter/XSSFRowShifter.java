

import org.apache.poi.ss.formula.FormulaShifter;
import org.apache.poi.ss.usermodel.helpers.RowShifter;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;


public final class XSSFRowShifter extends RowShifter {
	private static final POILogger logger = POILogFactory.getLogger(XSSFRowShifter.class);

	public XSSFRowShifter(XSSFSheet sh) {
		super(sh);
	}

	@Override
	public void updateNamedRanges(FormulaShifter formulaShifter) {
	}

	@Override
	public void updateFormulas(FormulaShifter formulaShifter) {
	}

	@org.apache.poi.util.Internal(since = "3.15 beta 2")
	public void updateRowFormulas(XSSFRow row, FormulaShifter formulaShifter) {
	}

	@Override
	public void updateConditionalFormatting(FormulaShifter formulaShifter) {
	}

	@Override
	public void updateHyperlinks(FormulaShifter formulaShifter) {
	}
}

