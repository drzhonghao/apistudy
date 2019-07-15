

import org.apache.poi.hssf.record.CFRule12Record;
import org.apache.poi.hssf.record.CFRuleBase;
import org.apache.poi.hssf.record.CFRuleRecord;
import org.apache.poi.hssf.record.aggregates.CFRecordsAggregate;
import org.apache.poi.hssf.record.aggregates.ConditionalFormattingTable;
import org.apache.poi.hssf.usermodel.HSSFConditionalFormatting;
import org.apache.poi.hssf.usermodel.HSSFConditionalFormattingRule;
import org.apache.poi.hssf.usermodel.HSSFExtendedColor;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.ConditionalFormatting;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.ExtendedColor;
import org.apache.poi.ss.usermodel.IconMultiStateFormatting;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressBase;


public final class HSSFSheetConditionalFormatting implements SheetConditionalFormatting {
	private final HSSFSheet _sheet;

	private final ConditionalFormattingTable _conditionalFormattingTable;

	HSSFSheetConditionalFormatting(HSSFSheet sheet) {
		_sheet = sheet;
		_conditionalFormattingTable = null;
	}

	public HSSFConditionalFormattingRule createConditionalFormattingRule(byte comparisonOperation, String formula1, String formula2) {
		CFRuleRecord rr = CFRuleRecord.create(_sheet, comparisonOperation, formula1, formula2);
		return null;
	}

	public HSSFConditionalFormattingRule createConditionalFormattingRule(byte comparisonOperation, String formula1) {
		CFRuleRecord rr = CFRuleRecord.create(_sheet, comparisonOperation, formula1, null);
		return null;
	}

	public HSSFConditionalFormattingRule createConditionalFormattingRule(String formula) {
		CFRuleRecord rr = CFRuleRecord.create(_sheet, formula);
		return null;
	}

	public HSSFConditionalFormattingRule createConditionalFormattingRule(IconMultiStateFormatting.IconSet iconSet) {
		CFRule12Record rr = CFRule12Record.create(_sheet, iconSet);
		return null;
	}

	public HSSFConditionalFormattingRule createConditionalFormattingRule(HSSFExtendedColor color) {
		return null;
	}

	public HSSFConditionalFormattingRule createConditionalFormattingRule(ExtendedColor color) {
		return createConditionalFormattingRule(((HSSFExtendedColor) (color)));
	}

	public HSSFConditionalFormattingRule createConditionalFormattingColorScaleRule() {
		CFRule12Record rr = CFRule12Record.createColorScale(_sheet);
		return null;
	}

	public int addConditionalFormatting(HSSFConditionalFormatting cf) {
		return 0;
	}

	public int addConditionalFormatting(ConditionalFormatting cf) {
		return addConditionalFormatting(((HSSFConditionalFormatting) (cf)));
	}

	public int addConditionalFormatting(CellRangeAddress[] regions, HSSFConditionalFormattingRule[] cfRules) {
		if (regions == null) {
			throw new IllegalArgumentException("regions must not be null");
		}
		for (CellRangeAddress range : regions)
			range.validate(SpreadsheetVersion.EXCEL97);

		if (cfRules == null) {
			throw new IllegalArgumentException("cfRules must not be null");
		}
		if ((cfRules.length) == 0) {
			throw new IllegalArgumentException("cfRules must not be empty");
		}
		if ((cfRules.length) > 3) {
			throw new IllegalArgumentException("Number of rules must not exceed 3");
		}
		CFRuleBase[] rules = new CFRuleBase[cfRules.length];
		for (int i = 0; i != (cfRules.length); i++) {
		}
		CFRecordsAggregate cfra = new CFRecordsAggregate(regions, rules);
		return _conditionalFormattingTable.add(cfra);
	}

	public int addConditionalFormatting(CellRangeAddress[] regions, ConditionalFormattingRule[] cfRules) {
		HSSFConditionalFormattingRule[] hfRules;
		if (cfRules instanceof HSSFConditionalFormattingRule[])
			hfRules = ((HSSFConditionalFormattingRule[]) (cfRules));
		else {
			hfRules = new HSSFConditionalFormattingRule[cfRules.length];
			System.arraycopy(cfRules, 0, hfRules, 0, hfRules.length);
		}
		return addConditionalFormatting(regions, hfRules);
	}

	public int addConditionalFormatting(CellRangeAddress[] regions, HSSFConditionalFormattingRule rule1) {
		return addConditionalFormatting(regions, (rule1 == null ? null : new HSSFConditionalFormattingRule[]{ rule1 }));
	}

	public int addConditionalFormatting(CellRangeAddress[] regions, ConditionalFormattingRule rule1) {
		return addConditionalFormatting(regions, ((HSSFConditionalFormattingRule) (rule1)));
	}

	public int addConditionalFormatting(CellRangeAddress[] regions, HSSFConditionalFormattingRule rule1, HSSFConditionalFormattingRule rule2) {
		return addConditionalFormatting(regions, new HSSFConditionalFormattingRule[]{ rule1, rule2 });
	}

	public int addConditionalFormatting(CellRangeAddress[] regions, ConditionalFormattingRule rule1, ConditionalFormattingRule rule2) {
		return addConditionalFormatting(regions, ((HSSFConditionalFormattingRule) (rule1)), ((HSSFConditionalFormattingRule) (rule2)));
	}

	public HSSFConditionalFormatting getConditionalFormattingAt(int index) {
		CFRecordsAggregate cf = _conditionalFormattingTable.get(index);
		if (cf == null) {
			return null;
		}
		return null;
	}

	public int getNumConditionalFormattings() {
		return _conditionalFormattingTable.size();
	}

	public void removeConditionalFormatting(int index) {
		_conditionalFormattingTable.remove(index);
	}
}

