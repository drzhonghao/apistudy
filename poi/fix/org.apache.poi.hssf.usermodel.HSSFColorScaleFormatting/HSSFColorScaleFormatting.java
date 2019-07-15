

import org.apache.poi.hssf.record.CFRule12Record;
import org.apache.poi.hssf.record.cf.ColorGradientFormatting;
import org.apache.poi.hssf.record.cf.ColorGradientThreshold;
import org.apache.poi.hssf.record.cf.Threshold;
import org.apache.poi.hssf.record.common.ExtendedColor;
import org.apache.poi.hssf.usermodel.HSSFConditionalFormattingThreshold;
import org.apache.poi.hssf.usermodel.HSSFExtendedColor;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.ColorScaleFormatting;
import org.apache.poi.ss.usermodel.ConditionalFormattingThreshold;


public final class HSSFColorScaleFormatting implements ColorScaleFormatting {
	private final HSSFSheet sheet;

	private final CFRule12Record cfRule12Record;

	private final ColorGradientFormatting colorFormatting;

	protected HSSFColorScaleFormatting(CFRule12Record cfRule12Record, HSSFSheet sheet) {
		this.sheet = sheet;
		this.cfRule12Record = cfRule12Record;
		this.colorFormatting = this.cfRule12Record.getColorGradientFormatting();
	}

	public int getNumControlPoints() {
		return colorFormatting.getNumControlPoints();
	}

	public void setNumControlPoints(int num) {
		colorFormatting.setNumControlPoints(num);
	}

	public HSSFExtendedColor[] getColors() {
		ExtendedColor[] colors = colorFormatting.getColors();
		HSSFExtendedColor[] hcolors = new HSSFExtendedColor[colors.length];
		for (int i = 0; i < (colors.length); i++) {
			hcolors[i] = new HSSFExtendedColor(colors[i]);
		}
		return hcolors;
	}

	public void setColors(Color[] colors) {
		ExtendedColor[] cr = new ExtendedColor[colors.length];
		for (int i = 0; i < (colors.length); i++) {
		}
		colorFormatting.setColors(cr);
	}

	public HSSFConditionalFormattingThreshold[] getThresholds() {
		Threshold[] t = colorFormatting.getThresholds();
		HSSFConditionalFormattingThreshold[] ht = new HSSFConditionalFormattingThreshold[t.length];
		for (int i = 0; i < (t.length); i++) {
		}
		return ht;
	}

	public void setThresholds(ConditionalFormattingThreshold[] thresholds) {
		ColorGradientThreshold[] t = new ColorGradientThreshold[thresholds.length];
		for (int i = 0; i < (t.length); i++) {
			HSSFConditionalFormattingThreshold hssfT = ((HSSFConditionalFormattingThreshold) (thresholds[i]));
		}
		colorFormatting.setThresholds(t);
	}

	public HSSFConditionalFormattingThreshold createThreshold() {
		return null;
	}
}

