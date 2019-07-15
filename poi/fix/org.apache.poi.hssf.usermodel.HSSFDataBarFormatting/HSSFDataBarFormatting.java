

import org.apache.poi.hssf.record.CFRule12Record;
import org.apache.poi.hssf.record.common.ExtendedColor;
import org.apache.poi.hssf.usermodel.HSSFConditionalFormattingThreshold;
import org.apache.poi.hssf.usermodel.HSSFExtendedColor;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.DataBarFormatting;


public final class HSSFDataBarFormatting implements DataBarFormatting {
	private final HSSFSheet sheet;

	private final CFRule12Record cfRule12Record;

	private final org.apache.poi.hssf.record.cf.DataBarFormatting databarFormatting;

	protected HSSFDataBarFormatting(CFRule12Record cfRule12Record, HSSFSheet sheet) {
		this.sheet = sheet;
		this.cfRule12Record = cfRule12Record;
		this.databarFormatting = this.cfRule12Record.getDataBarFormatting();
	}

	public boolean isLeftToRight() {
		return !(databarFormatting.isReversed());
	}

	public void setLeftToRight(boolean ltr) {
		databarFormatting.setReversed((!ltr));
	}

	public int getWidthMin() {
		return databarFormatting.getPercentMin();
	}

	public void setWidthMin(int width) {
		databarFormatting.setPercentMin(((byte) (width)));
	}

	public int getWidthMax() {
		return databarFormatting.getPercentMax();
	}

	public void setWidthMax(int width) {
		databarFormatting.setPercentMax(((byte) (width)));
	}

	public HSSFExtendedColor getColor() {
		return new HSSFExtendedColor(databarFormatting.getColor());
	}

	public void setColor(Color color) {
		HSSFExtendedColor hcolor = ((HSSFExtendedColor) (color));
	}

	public HSSFConditionalFormattingThreshold getMinThreshold() {
		return null;
	}

	public HSSFConditionalFormattingThreshold getMaxThreshold() {
		return null;
	}

	public boolean isIconOnly() {
		return databarFormatting.isIconOnly();
	}

	public void setIconOnly(boolean only) {
		databarFormatting.setIconOnly(only);
	}

	public HSSFConditionalFormattingThreshold createThreshold() {
		return null;
	}
}

