

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.poi.ss.usermodel.ConditionalFormatting;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFConditionalFormattingRule;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTConditionalFormatting;

import static org.openxmlformats.schemas.spreadsheetml.x2006.main.CTConditionalFormatting.Factory.newInstance;


public class XSSFConditionalFormatting implements ConditionalFormatting {
	private final CTConditionalFormatting _cf;

	private final XSSFSheet _sh;

	XSSFConditionalFormatting(XSSFSheet sh) {
		_cf = newInstance();
		_sh = sh;
	}

	XSSFConditionalFormatting(XSSFSheet sh, CTConditionalFormatting cf) {
		_cf = cf;
		_sh = sh;
	}

	CTConditionalFormatting getCTConditionalFormatting() {
		return _cf;
	}

	@Override
	public CellRangeAddress[] getFormattingRanges() {
		ArrayList<CellRangeAddress> lst = new ArrayList<>();
		for (Object stRef : _cf.getSqref()) {
			String[] regions = stRef.toString().split(" ");
			for (final String region : regions) {
				lst.add(CellRangeAddress.valueOf(region));
			}
		}
		return lst.toArray(new CellRangeAddress[lst.size()]);
	}

	@Override
	public void setFormattingRanges(CellRangeAddress[] ranges) {
		if (ranges == null) {
			throw new IllegalArgumentException("cellRanges must not be null");
		}
		final StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (CellRangeAddress range : ranges) {
			if (!first) {
				sb.append(" ");
			}else {
				first = false;
			}
			sb.append(range.formatAsString());
		}
		_cf.setSqref(Collections.singletonList(sb.toString()));
	}

	@Override
	public void setRule(int idx, ConditionalFormattingRule cfRule) {
		XSSFConditionalFormattingRule xRule = ((XSSFConditionalFormattingRule) (cfRule));
	}

	@Override
	public void addRule(ConditionalFormattingRule cfRule) {
		XSSFConditionalFormattingRule xRule = ((XSSFConditionalFormattingRule) (cfRule));
	}

	@Override
	public XSSFConditionalFormattingRule getRule(int idx) {
		return null;
	}

	@Override
	public int getNumberOfRules() {
		return _cf.sizeOfCfRuleArray();
	}

	@Override
	public String toString() {
		return _cf.toString();
	}
}

