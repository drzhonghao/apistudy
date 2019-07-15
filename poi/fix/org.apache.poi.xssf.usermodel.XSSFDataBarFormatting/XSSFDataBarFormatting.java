

import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.DataBarFormatting;
import org.apache.poi.xssf.usermodel.IndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFConditionalFormattingThreshold;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColor;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDataBar;


public class XSSFDataBarFormatting implements DataBarFormatting {
	IndexedColorMap _colorMap;

	CTDataBar _databar;

	XSSFDataBarFormatting(CTDataBar databar, IndexedColorMap colorMap) {
		_databar = databar;
		_colorMap = colorMap;
	}

	public boolean isIconOnly() {
		if (_databar.isSetShowValue())
			return !(_databar.getShowValue());

		return false;
	}

	public void setIconOnly(boolean only) {
		_databar.setShowValue((!only));
	}

	public boolean isLeftToRight() {
		return true;
	}

	public void setLeftToRight(boolean ltr) {
	}

	public int getWidthMin() {
		return 0;
	}

	public void setWidthMin(int width) {
	}

	public int getWidthMax() {
		return 100;
	}

	public void setWidthMax(int width) {
	}

	public XSSFColor getColor() {
		return XSSFColor.from(_databar.getColor(), _colorMap);
	}

	public void setColor(Color color) {
		_databar.setColor(((XSSFColor) (color)).getCTColor());
	}

	public XSSFConditionalFormattingThreshold getMinThreshold() {
		return null;
	}

	public XSSFConditionalFormattingThreshold getMaxThreshold() {
		return null;
	}

	public XSSFConditionalFormattingThreshold createThreshold() {
		return null;
	}
}

