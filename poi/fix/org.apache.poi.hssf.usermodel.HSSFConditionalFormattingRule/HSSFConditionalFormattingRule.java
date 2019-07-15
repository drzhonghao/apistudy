

import org.apache.poi.hssf.model.HSSFFormulaParser;
import org.apache.poi.hssf.record.CFRule12Record;
import org.apache.poi.hssf.record.CFRuleBase;
import org.apache.poi.hssf.record.CFRuleRecord;
import org.apache.poi.hssf.record.cf.BorderFormatting;
import org.apache.poi.hssf.record.cf.ColorGradientFormatting;
import org.apache.poi.hssf.record.cf.DataBarFormatting;
import org.apache.poi.hssf.record.cf.FontFormatting;
import org.apache.poi.hssf.record.cf.IconMultiStateFormatting;
import org.apache.poi.hssf.record.cf.PatternFormatting;
import org.apache.poi.hssf.usermodel.HSSFBorderFormatting;
import org.apache.poi.hssf.usermodel.HSSFColorScaleFormatting;
import org.apache.poi.hssf.usermodel.HSSFDataBarFormatting;
import org.apache.poi.hssf.usermodel.HSSFFontFormatting;
import org.apache.poi.hssf.usermodel.HSSFIconMultiStateFormatting;
import org.apache.poi.hssf.usermodel.HSSFPatternFormatting;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.usermodel.ConditionFilterData;
import org.apache.poi.ss.usermodel.ConditionFilterType;
import org.apache.poi.ss.usermodel.ConditionType;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.ExcelNumberFormat;

import static org.apache.poi.hssf.record.CFRuleBase.ComparisonOperator.BETWEEN;
import static org.apache.poi.hssf.record.CFRuleBase.ComparisonOperator.NOT_BETWEEN;


public final class HSSFConditionalFormattingRule implements ConditionalFormattingRule {
	private static final byte CELL_COMPARISON = CFRuleRecord.CONDITION_TYPE_CELL_VALUE_IS;

	private final CFRuleBase cfRuleRecord;

	private final HSSFWorkbook workbook;

	private final HSSFSheet sheet;

	HSSFConditionalFormattingRule(HSSFSheet pSheet, CFRuleBase pRuleRecord) {
		if (pSheet == null) {
			throw new IllegalArgumentException("pSheet must not be null");
		}
		if (pRuleRecord == null) {
			throw new IllegalArgumentException("pRuleRecord must not be null");
		}
		sheet = pSheet;
		workbook = pSheet.getWorkbook();
		cfRuleRecord = pRuleRecord;
	}

	public int getPriority() {
		CFRule12Record rule12 = getCFRule12Record(false);
		if (rule12 == null)
			return 0;

		return rule12.getPriority();
	}

	public boolean getStopIfTrue() {
		return true;
	}

	CFRuleBase getCfRuleRecord() {
		return cfRuleRecord;
	}

	private CFRule12Record getCFRule12Record(boolean create) {
		if ((cfRuleRecord) instanceof CFRule12Record) {
		}else {
			if (create)
				throw new IllegalArgumentException("Can't convert a CF into a CF12 record");

			return null;
		}
		return ((CFRule12Record) (cfRuleRecord));
	}

	public ExcelNumberFormat getNumberFormat() {
		return null;
	}

	private HSSFFontFormatting getFontFormatting(boolean create) {
		FontFormatting fontFormatting = cfRuleRecord.getFontFormatting();
		if (fontFormatting == null) {
			if (!create)
				return null;

			fontFormatting = new FontFormatting();
			cfRuleRecord.setFontFormatting(fontFormatting);
		}
		return null;
	}

	public HSSFFontFormatting getFontFormatting() {
		return getFontFormatting(false);
	}

	public HSSFFontFormatting createFontFormatting() {
		return getFontFormatting(true);
	}

	private HSSFBorderFormatting getBorderFormatting(boolean create) {
		BorderFormatting borderFormatting = cfRuleRecord.getBorderFormatting();
		if (borderFormatting == null) {
			if (!create)
				return null;

			borderFormatting = new BorderFormatting();
			cfRuleRecord.setBorderFormatting(borderFormatting);
		}
		return null;
	}

	public HSSFBorderFormatting getBorderFormatting() {
		return getBorderFormatting(false);
	}

	public HSSFBorderFormatting createBorderFormatting() {
		return getBorderFormatting(true);
	}

	private HSSFPatternFormatting getPatternFormatting(boolean create) {
		PatternFormatting patternFormatting = cfRuleRecord.getPatternFormatting();
		if (patternFormatting == null) {
			if (!create)
				return null;

			patternFormatting = new PatternFormatting();
			cfRuleRecord.setPatternFormatting(patternFormatting);
		}
		return null;
	}

	public HSSFPatternFormatting getPatternFormatting() {
		return getPatternFormatting(false);
	}

	public HSSFPatternFormatting createPatternFormatting() {
		return getPatternFormatting(true);
	}

	private HSSFDataBarFormatting getDataBarFormatting(boolean create) {
		CFRule12Record cfRule12Record = getCFRule12Record(create);
		if (cfRule12Record == null)
			return null;

		DataBarFormatting databarFormatting = cfRule12Record.getDataBarFormatting();
		if (databarFormatting == null) {
			if (!create)
				return null;

			cfRule12Record.createDataBarFormatting();
		}
		return null;
	}

	public HSSFDataBarFormatting getDataBarFormatting() {
		return getDataBarFormatting(false);
	}

	public HSSFDataBarFormatting createDataBarFormatting() {
		return getDataBarFormatting(true);
	}

	private HSSFIconMultiStateFormatting getMultiStateFormatting(boolean create) {
		CFRule12Record cfRule12Record = getCFRule12Record(create);
		if (cfRule12Record == null)
			return null;

		IconMultiStateFormatting iconFormatting = cfRule12Record.getMultiStateFormatting();
		if (iconFormatting == null) {
			if (!create)
				return null;

			cfRule12Record.createMultiStateFormatting();
		}
		return null;
	}

	public HSSFIconMultiStateFormatting getMultiStateFormatting() {
		return getMultiStateFormatting(false);
	}

	public HSSFIconMultiStateFormatting createMultiStateFormatting() {
		return getMultiStateFormatting(true);
	}

	private HSSFColorScaleFormatting getColorScaleFormatting(boolean create) {
		CFRule12Record cfRule12Record = getCFRule12Record(create);
		if (cfRule12Record == null)
			return null;

		ColorGradientFormatting colorFormatting = cfRule12Record.getColorGradientFormatting();
		if (colorFormatting == null) {
			if (!create)
				return null;

			cfRule12Record.createColorGradientFormatting();
		}
		return null;
	}

	public HSSFColorScaleFormatting getColorScaleFormatting() {
		return getColorScaleFormatting(false);
	}

	public HSSFColorScaleFormatting createColorScaleFormatting() {
		return getColorScaleFormatting(true);
	}

	@Override
	public ConditionType getConditionType() {
		byte code = cfRuleRecord.getConditionType();
		return ConditionType.forId(code);
	}

	public ConditionFilterType getConditionFilterType() {
		return (getConditionType()) == (ConditionType.FILTER) ? ConditionFilterType.FILTER : null;
	}

	public ConditionFilterData getFilterConfiguration() {
		return null;
	}

	@Override
	public byte getComparisonOperation() {
		return cfRuleRecord.getComparisonOperation();
	}

	public String getFormula1() {
		return toFormulaString(cfRuleRecord.getParsedExpression1());
	}

	public String getFormula2() {
		byte conditionType = cfRuleRecord.getConditionType();
		if (conditionType == (HSSFConditionalFormattingRule.CELL_COMPARISON)) {
			byte comparisonOperation = cfRuleRecord.getComparisonOperation();
			switch (comparisonOperation) {
				case BETWEEN :
				case NOT_BETWEEN :
					return toFormulaString(cfRuleRecord.getParsedExpression2());
			}
		}
		return null;
	}

	public String getText() {
		return null;
	}

	protected String toFormulaString(Ptg[] parsedExpression) {
		return HSSFConditionalFormattingRule.toFormulaString(parsedExpression, workbook);
	}

	protected static String toFormulaString(Ptg[] parsedExpression, HSSFWorkbook workbook) {
		if ((parsedExpression == null) || ((parsedExpression.length) == 0)) {
			return null;
		}
		return HSSFFormulaParser.toFormulaString(workbook, parsedExpression);
	}

	public int getStripeSize() {
		return 0;
	}
}

