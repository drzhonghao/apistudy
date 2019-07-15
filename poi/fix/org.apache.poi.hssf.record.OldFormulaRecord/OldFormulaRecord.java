

import org.apache.poi.hssf.record.OldCellRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.ss.formula.Formula;
import org.apache.poi.ss.formula.ptg.Ptg;


public final class OldFormulaRecord extends OldCellRecord {
	public static final short biff2_sid = 6;

	public static final short biff3_sid = 518;

	public static final short biff4_sid = 1030;

	public static final short biff5_sid = 6;

	private double field_4_value;

	private short field_5_options;

	private Formula field_6_parsed_expr;

	public OldFormulaRecord(RecordInputStream ris) {
		super(ris, ((ris.getSid()) == (OldFormulaRecord.biff2_sid)));
		if (isBiff2()) {
			field_4_value = ris.readDouble();
		}else {
			long valueLongBits = ris.readLong();
		}
		if (isBiff2()) {
			field_5_options = ((short) (ris.readUByte()));
		}else {
			field_5_options = ris.readShort();
		}
		int expression_len = ris.readShort();
		int nBytesAvailable = ris.available();
		field_6_parsed_expr = Formula.read(expression_len, ris, nBytesAvailable);
	}

	public int getCachedResultType() {
		return 0;
	}

	public boolean getCachedBooleanValue() {
		return false;
	}

	public int getCachedErrorValue() {
		return 0;
	}

	public double getValue() {
		return field_4_value;
	}

	public short getOptions() {
		return field_5_options;
	}

	public Ptg[] getParsedExpression() {
		return field_6_parsed_expr.getTokens();
	}

	public Formula getFormula() {
		return field_6_parsed_expr;
	}

	protected void appendValueText(StringBuilder sb) {
		sb.append("    .value       = ").append(getValue()).append("\n");
	}

	protected String getRecordName() {
		return "Old Formula";
	}
}

