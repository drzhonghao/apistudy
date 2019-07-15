

import org.apache.poi.hssf.record.CFRuleBase;
import org.apache.poi.hssf.record.cf.Threshold;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.usermodel.ConditionalFormattingThreshold;

import static org.apache.poi.ss.usermodel.ConditionalFormattingThreshold.RangeType.byId;


public final class HSSFConditionalFormattingThreshold implements ConditionalFormattingThreshold {
	private final Threshold threshold;

	private final HSSFSheet sheet;

	private final HSSFWorkbook workbook;

	protected HSSFConditionalFormattingThreshold(Threshold threshold, HSSFSheet sheet) {
		this.threshold = threshold;
		this.sheet = sheet;
		this.workbook = sheet.getWorkbook();
	}

	protected Threshold getThreshold() {
		return threshold;
	}

	public ConditionalFormattingThreshold.RangeType getRangeType() {
		return byId(threshold.getType());
	}

	public void setRangeType(ConditionalFormattingThreshold.RangeType type) {
		threshold.setType(((byte) (type.id)));
	}

	public String getFormula() {
		return null;
	}

	public void setFormula(String formula) {
		threshold.setParsedExpression(CFRuleBase.parseFormula(formula, sheet));
	}

	public Double getValue() {
		return threshold.getValue();
	}

	public void setValue(Double value) {
		threshold.setValue(value);
	}
}

