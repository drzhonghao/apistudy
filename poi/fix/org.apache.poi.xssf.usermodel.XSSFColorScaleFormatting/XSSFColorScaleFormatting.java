

import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.ColorScaleFormatting;
import org.apache.poi.ss.usermodel.ConditionalFormattingThreshold;
import org.apache.poi.xssf.usermodel.IndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFConditionalFormattingThreshold;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCfvo;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColor;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColorScale;


public class XSSFColorScaleFormatting implements ColorScaleFormatting {
	private CTColorScale _scale;

	private IndexedColorMap _indexedColorMap;

	XSSFColorScaleFormatting(CTColorScale scale, IndexedColorMap colorMap) {
		_scale = scale;
		_indexedColorMap = colorMap;
	}

	public int getNumControlPoints() {
		return _scale.sizeOfCfvoArray();
	}

	public void setNumControlPoints(int num) {
		while (num < (_scale.sizeOfCfvoArray())) {
			_scale.removeCfvo(((_scale.sizeOfCfvoArray()) - 1));
			_scale.removeColor(((_scale.sizeOfColorArray()) - 1));
		} 
		while (num > (_scale.sizeOfCfvoArray())) {
			_scale.addNewCfvo();
			_scale.addNewColor();
		} 
	}

	public XSSFColor[] getColors() {
		CTColor[] ctcols = _scale.getColorArray();
		XSSFColor[] c = new XSSFColor[ctcols.length];
		for (int i = 0; i < (ctcols.length); i++) {
			c[i] = XSSFColor.from(ctcols[i], _indexedColorMap);
		}
		return c;
	}

	public void setColors(Color[] colors) {
		CTColor[] ctcols = new CTColor[colors.length];
		for (int i = 0; i < (colors.length); i++) {
			ctcols[i] = ((XSSFColor) (colors[i])).getCTColor();
		}
		_scale.setColorArray(ctcols);
	}

	public XSSFConditionalFormattingThreshold[] getThresholds() {
		CTCfvo[] cfvos = _scale.getCfvoArray();
		XSSFConditionalFormattingThreshold[] t = new XSSFConditionalFormattingThreshold[cfvos.length];
		for (int i = 0; i < (cfvos.length); i++) {
		}
		return t;
	}

	public void setThresholds(ConditionalFormattingThreshold[] thresholds) {
		CTCfvo[] cfvos = new CTCfvo[thresholds.length];
		for (int i = 0; i < (thresholds.length); i++) {
		}
		_scale.setCfvoArray(cfvos);
	}

	public XSSFColor createColor() {
		return XSSFColor.from(_scale.addNewColor(), _indexedColorMap);
	}

	public XSSFConditionalFormattingThreshold createThreshold() {
		return null;
	}
}

