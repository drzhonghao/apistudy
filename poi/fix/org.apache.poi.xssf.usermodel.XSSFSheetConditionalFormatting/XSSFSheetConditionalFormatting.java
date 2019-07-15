

import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.ComparisonOperator;
import org.apache.poi.ss.usermodel.ConditionalFormatting;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.ExtendedColor;
import org.apache.poi.ss.usermodel.IconMultiStateFormatting;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressBase;
import org.apache.poi.ss.util.CellRangeUtil;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFConditionalFormatting;
import org.apache.poi.xssf.usermodel.XSSFConditionalFormattingRule;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTConditionalFormatting;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorksheet;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STConditionalFormattingOperator;


public class XSSFSheetConditionalFormatting implements SheetConditionalFormatting {
	protected static final String CF_EXT_2009_NS_X14 = "http://schemas.microsoft.com/office/spreadsheetml/2009/9/main";

	private final XSSFSheet _sheet;

	XSSFSheetConditionalFormatting(XSSFSheet sheet) {
		_sheet = sheet;
	}

	public XSSFConditionalFormattingRule createConditionalFormattingRule(byte comparisonOperation, String formula1, String formula2) {
		if (formula2 != null) {
		}
		STConditionalFormattingOperator.Enum operator;
		switch (comparisonOperation) {
			case ComparisonOperator.BETWEEN :
				operator = STConditionalFormattingOperator.BETWEEN;
				break;
			case ComparisonOperator.NOT_BETWEEN :
				operator = STConditionalFormattingOperator.NOT_BETWEEN;
				break;
			case ComparisonOperator.LT :
				operator = STConditionalFormattingOperator.LESS_THAN;
				break;
			case ComparisonOperator.LE :
				operator = STConditionalFormattingOperator.LESS_THAN_OR_EQUAL;
				break;
			case ComparisonOperator.GT :
				operator = STConditionalFormattingOperator.GREATER_THAN;
				break;
			case ComparisonOperator.GE :
				operator = STConditionalFormattingOperator.GREATER_THAN_OR_EQUAL;
				break;
			case ComparisonOperator.EQUAL :
				operator = STConditionalFormattingOperator.EQUAL;
				break;
			case ComparisonOperator.NOT_EQUAL :
				operator = STConditionalFormattingOperator.NOT_EQUAL;
				break;
			default :
				throw new IllegalArgumentException(("Unknown comparison operator: " + comparisonOperation));
		}
		return null;
	}

	public XSSFConditionalFormattingRule createConditionalFormattingRule(byte comparisonOperation, String formula) {
		return createConditionalFormattingRule(comparisonOperation, formula, null);
	}

	public XSSFConditionalFormattingRule createConditionalFormattingRule(String formula) {
		return null;
	}

	public XSSFConditionalFormattingRule createConditionalFormattingRule(XSSFColor color) {
		return null;
	}

	public XSSFConditionalFormattingRule createConditionalFormattingRule(ExtendedColor color) {
		return createConditionalFormattingRule(((XSSFColor) (color)));
	}

	public XSSFConditionalFormattingRule createConditionalFormattingRule(IconMultiStateFormatting.IconSet iconSet) {
		return null;
	}

	public XSSFConditionalFormattingRule createConditionalFormattingColorScaleRule() {
		return null;
	}

	public int addConditionalFormatting(CellRangeAddress[] regions, ConditionalFormattingRule[] cfRules) {
		if (regions == null) {
			throw new IllegalArgumentException("regions must not be null");
		}
		for (CellRangeAddress range : regions)
			range.validate(SpreadsheetVersion.EXCEL2007);

		if (cfRules == null) {
			throw new IllegalArgumentException("cfRules must not be null");
		}
		if ((cfRules.length) == 0) {
			throw new IllegalArgumentException("cfRules must not be empty");
		}
		if ((cfRules.length) > 3) {
			throw new IllegalArgumentException("Number of rules must not exceed 3");
		}
		CellRangeAddress[] mergeCellRanges = CellRangeUtil.mergeCellRanges(regions);
		CTConditionalFormatting cf = _sheet.getCTWorksheet().addNewConditionalFormatting();
		List<String> refs = new ArrayList<>();
		for (CellRangeAddress a : mergeCellRanges)
			refs.add(a.formatAsString());

		cf.setSqref(refs);
		int priority = 1;
		for (CTConditionalFormatting c : _sheet.getCTWorksheet().getConditionalFormattingArray()) {
			priority += c.sizeOfCfRuleArray();
		}
		for (ConditionalFormattingRule rule : cfRules) {
			XSSFConditionalFormattingRule xRule = ((XSSFConditionalFormattingRule) (rule));
		}
		return (_sheet.getCTWorksheet().sizeOfConditionalFormattingArray()) - 1;
	}

	public int addConditionalFormatting(CellRangeAddress[] regions, ConditionalFormattingRule rule1) {
		return addConditionalFormatting(regions, (rule1 == null ? null : new XSSFConditionalFormattingRule[]{ ((XSSFConditionalFormattingRule) (rule1)) }));
	}

	public int addConditionalFormatting(CellRangeAddress[] regions, ConditionalFormattingRule rule1, ConditionalFormattingRule rule2) {
		return addConditionalFormatting(regions, (rule1 == null ? null : new XSSFConditionalFormattingRule[]{ ((XSSFConditionalFormattingRule) (rule1)), ((XSSFConditionalFormattingRule) (rule2)) }));
	}

	public int addConditionalFormatting(ConditionalFormatting cf) {
		XSSFConditionalFormatting xcf = ((XSSFConditionalFormatting) (cf));
		CTWorksheet sh = _sheet.getCTWorksheet();
		return (sh.sizeOfConditionalFormattingArray()) - 1;
	}

	public XSSFConditionalFormatting getConditionalFormattingAt(int index) {
		checkIndex(index);
		CTConditionalFormatting cf = _sheet.getCTWorksheet().getConditionalFormattingArray(index);
		return null;
	}

	public int getNumConditionalFormattings() {
		return _sheet.getCTWorksheet().sizeOfConditionalFormattingArray();
	}

	public void removeConditionalFormatting(int index) {
		checkIndex(index);
		_sheet.getCTWorksheet().removeConditionalFormatting(index);
	}

	private void checkIndex(int index) {
		int cnt = getNumConditionalFormattings();
		if ((index < 0) || (index >= cnt)) {
			throw new IllegalArgumentException((((("Specified CF index " + index) + " is outside the allowable range (0..") + (cnt - 1)) + ")"));
		}
	}
}

