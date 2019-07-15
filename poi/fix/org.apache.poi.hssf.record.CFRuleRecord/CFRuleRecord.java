

import java.util.Arrays;
import org.apache.poi.hssf.record.CFRuleBase;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.cf.BorderFormatting;
import org.apache.poi.hssf.record.cf.FontFormatting;
import org.apache.poi.hssf.record.cf.PatternFormatting;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.formula.Formula;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.util.LittleEndianOutput;

import static org.apache.poi.hssf.record.CFRuleBase.ComparisonOperator.NO_COMPARISON;


public final class CFRuleRecord extends CFRuleBase implements Cloneable {
	public static final short sid = 433;

	private CFRuleRecord(byte conditionType, byte comparisonOperation) {
		super(conditionType, comparisonOperation);
		setDefaults();
	}

	private CFRuleRecord(byte conditionType, byte comparisonOperation, Ptg[] formula1, Ptg[] formula2) {
		super(conditionType, comparisonOperation, formula1, formula2);
		setDefaults();
	}

	private void setDefaults() {
		formatting_not_used = ((short) (32770));
		_fontFormatting = null;
		_borderFormatting = null;
		_patternFormatting = null;
	}

	public static CFRuleRecord create(HSSFSheet sheet, String formulaText) {
		Ptg[] formula1 = CFRuleBase.parseFormula(formulaText, sheet);
		return new CFRuleRecord(CFRuleBase.CONDITION_TYPE_FORMULA, NO_COMPARISON, formula1, null);
	}

	public static CFRuleRecord create(HSSFSheet sheet, byte comparisonOperation, String formulaText1, String formulaText2) {
		Ptg[] formula1 = CFRuleBase.parseFormula(formulaText1, sheet);
		Ptg[] formula2 = CFRuleBase.parseFormula(formulaText2, sheet);
		return new CFRuleRecord(CFRuleBase.CONDITION_TYPE_CELL_VALUE_IS, comparisonOperation, formula1, formula2);
	}

	public CFRuleRecord(RecordInputStream in) {
		setConditionType(in.readByte());
		setComparisonOperation(in.readByte());
		int field_3_formula1_len = in.readUShort();
		int field_4_formula2_len = in.readUShort();
		readFormatOptions(in);
		setFormula1(Formula.read(field_3_formula1_len, in));
		setFormula2(Formula.read(field_4_formula2_len, in));
	}

	@Override
	public short getSid() {
		return CFRuleRecord.sid;
	}

	@Override
	public void serialize(LittleEndianOutput out) {
		int formula1Len = CFRuleBase.getFormulaSize(getFormula1());
		int formula2Len = CFRuleBase.getFormulaSize(getFormula2());
		out.writeByte(getConditionType());
		out.writeByte(getComparisonOperation());
		out.writeShort(formula1Len);
		out.writeShort(formula2Len);
		serializeFormattingBlock(out);
		getFormula1().serializeTokens(out);
		getFormula2().serializeTokens(out);
	}

	@Override
	protected int getDataSize() {
		return ((6 + (getFormattingBlockSize())) + (CFRuleBase.getFormulaSize(getFormula1()))) + (CFRuleBase.getFormulaSize(getFormula2()));
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[CFRULE]\n");
		buffer.append("    .condition_type   =").append(getConditionType()).append("\n");
		buffer.append("    OPTION FLAGS=0x").append(Integer.toHexString(getOptions())).append("\n");
		if (containsFontFormattingBlock()) {
			buffer.append(_fontFormatting).append("\n");
		}
		if (containsBorderFormattingBlock()) {
			buffer.append(_borderFormatting).append("\n");
		}
		if (containsPatternFormattingBlock()) {
			buffer.append(_patternFormatting).append("\n");
		}
		buffer.append("    Formula 1 =").append(Arrays.toString(getFormula1().getTokens())).append("\n");
		buffer.append("    Formula 2 =").append(Arrays.toString(getFormula2().getTokens())).append("\n");
		buffer.append("[/CFRULE]\n");
		return buffer.toString();
	}

	@Override
	public CFRuleRecord clone() {
		CFRuleRecord rec = new CFRuleRecord(getConditionType(), getComparisonOperation());
		super.copyTo(rec);
		return rec;
	}
}

