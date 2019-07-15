

import org.apache.poi.ss.formula.FormulaShifter;
import org.apache.poi.ss.usermodel.helpers.ColumnShifter;
import org.apache.poi.util.Beta;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.xssf.usermodel.XSSFSheet;


@Beta
public final class XSSFColumnShifter extends ColumnShifter {
	private static final POILogger logger = POILogFactory.getLogger(XSSFColumnShifter.class);

	public XSSFColumnShifter(XSSFSheet sh) {
		super(sh);
	}

	@Override
	public void updateNamedRanges(FormulaShifter formulaShifter) {
	}

	@Override
	public void updateFormulas(FormulaShifter formulaShifter) {
	}

	@Override
	public void updateConditionalFormatting(FormulaShifter formulaShifter) {
	}

	@Override
	public void updateHyperlinks(FormulaShifter formulaShifter) {
	}
}

