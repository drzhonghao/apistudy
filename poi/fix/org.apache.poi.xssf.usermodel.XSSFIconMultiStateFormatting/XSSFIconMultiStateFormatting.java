

import org.apache.poi.ss.usermodel.ConditionalFormattingThreshold;
import org.apache.poi.ss.usermodel.IconMultiStateFormatting;
import org.apache.poi.xssf.usermodel.XSSFConditionalFormattingThreshold;
import org.apache.xmlbeans.StringEnumAbstractBase;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCfvo;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTIconSet;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STIconSetType;

import static org.apache.poi.ss.usermodel.IconMultiStateFormatting.IconSet.byName;


public class XSSFIconMultiStateFormatting implements IconMultiStateFormatting {
	CTIconSet _iconset;

	XSSFIconMultiStateFormatting(CTIconSet iconset) {
		_iconset = iconset;
	}

	public IconMultiStateFormatting.IconSet getIconSet() {
		String set = _iconset.getIconSet().toString();
		return byName(set);
	}

	public void setIconSet(IconMultiStateFormatting.IconSet set) {
		STIconSetType.Enum xIconSet = STIconSetType.Enum.forString(set.name);
		_iconset.setIconSet(xIconSet);
	}

	public boolean isIconOnly() {
		if (_iconset.isSetShowValue())
			return !(_iconset.getShowValue());

		return false;
	}

	public void setIconOnly(boolean only) {
		_iconset.setShowValue((!only));
	}

	public boolean isReversed() {
		if (_iconset.isSetReverse())
			return _iconset.getReverse();

		return false;
	}

	public void setReversed(boolean reversed) {
		_iconset.setReverse(reversed);
	}

	public XSSFConditionalFormattingThreshold[] getThresholds() {
		CTCfvo[] cfvos = _iconset.getCfvoArray();
		XSSFConditionalFormattingThreshold[] t = new XSSFConditionalFormattingThreshold[cfvos.length];
		for (int i = 0; i < (cfvos.length); i++) {
		}
		return t;
	}

	public void setThresholds(ConditionalFormattingThreshold[] thresholds) {
		CTCfvo[] cfvos = new CTCfvo[thresholds.length];
		for (int i = 0; i < (thresholds.length); i++) {
		}
		_iconset.setCfvoArray(cfvos);
	}

	public XSSFConditionalFormattingThreshold createThreshold() {
		return null;
	}
}

