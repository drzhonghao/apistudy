

import org.apache.poi.hssf.record.CFHeaderBase;
import org.apache.poi.hssf.record.CFRuleBase;
import org.apache.poi.hssf.record.aggregates.CFRecordsAggregate;
import org.apache.poi.hssf.usermodel.HSSFConditionalFormattingRule;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.usermodel.ConditionalFormatting;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.util.CellRangeAddress;


public final class HSSFConditionalFormatting implements ConditionalFormatting {
	private final HSSFSheet sheet;

	private final CFRecordsAggregate cfAggregate;

	HSSFConditionalFormatting(HSSFSheet sheet, CFRecordsAggregate cfAggregate) {
		if (sheet == null) {
			throw new IllegalArgumentException("sheet must not be null");
		}
		if (cfAggregate == null) {
			throw new IllegalArgumentException("cfAggregate must not be null");
		}
		this.sheet = sheet;
		this.cfAggregate = cfAggregate;
	}

	CFRecordsAggregate getCFRecordsAggregate() {
		return cfAggregate;
	}

	@Override
	public CellRangeAddress[] getFormattingRanges() {
		return cfAggregate.getHeader().getCellRanges();
	}

	@Override
	public void setFormattingRanges(final CellRangeAddress[] ranges) {
		cfAggregate.getHeader().setCellRanges(ranges);
	}

	public void setRule(int idx, HSSFConditionalFormattingRule cfRule) {
	}

	@Override
	public void setRule(int idx, ConditionalFormattingRule cfRule) {
		setRule(idx, ((HSSFConditionalFormattingRule) (cfRule)));
	}

	public void addRule(HSSFConditionalFormattingRule cfRule) {
	}

	@Override
	public void addRule(ConditionalFormattingRule cfRule) {
		addRule(((HSSFConditionalFormattingRule) (cfRule)));
	}

	@Override
	public HSSFConditionalFormattingRule getRule(int idx) {
		CFRuleBase ruleRecord = cfAggregate.getRule(idx);
		return null;
	}

	@Override
	public int getNumberOfRules() {
		return cfAggregate.getNumberOfRules();
	}

	@Override
	public String toString() {
		return cfAggregate.toString();
	}
}

