

import java.util.List;
import org.apache.poi.ss.formula.ConditionalFormattingEvaluator;
import org.apache.poi.ss.formula.EvaluationConditionalFormatRule;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;


public class ExcelNumberFormat {
	private final int idx;

	private final String format;

	public static ExcelNumberFormat from(CellStyle style) {
		if (style == null)
			return null;

		return new ExcelNumberFormat(style.getDataFormat(), style.getDataFormatString());
	}

	public static ExcelNumberFormat from(Cell cell, ConditionalFormattingEvaluator cfEvaluator) {
		if (cell == null)
			return null;

		ExcelNumberFormat nf = null;
		if (cfEvaluator != null) {
			List<EvaluationConditionalFormatRule> rules = cfEvaluator.getConditionalFormattingForCell(cell);
			for (EvaluationConditionalFormatRule rule : rules) {
				if (nf != null)
					break;

			}
		}
		if (nf == null) {
			CellStyle style = cell.getCellStyle();
			nf = ExcelNumberFormat.from(style);
		}
		return nf;
	}

	public ExcelNumberFormat(int idx, String format) {
		this.idx = idx;
		this.format = format;
	}

	public int getIdx() {
		return idx;
	}

	public String getFormat() {
		return format;
	}
}

