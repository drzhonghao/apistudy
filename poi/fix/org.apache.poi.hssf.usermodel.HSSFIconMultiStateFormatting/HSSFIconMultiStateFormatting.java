

import org.apache.poi.hssf.record.CFRule12Record;
import org.apache.poi.hssf.record.cf.Threshold;
import org.apache.poi.hssf.usermodel.HSSFConditionalFormattingThreshold;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.usermodel.ConditionalFormattingThreshold;
import org.apache.poi.ss.usermodel.IconMultiStateFormatting;


public final class HSSFIconMultiStateFormatting implements IconMultiStateFormatting {
	private final HSSFSheet sheet;

	private final CFRule12Record cfRule12Record;

	private final org.apache.poi.hssf.record.cf.IconMultiStateFormatting iconFormatting;

	protected HSSFIconMultiStateFormatting(CFRule12Record cfRule12Record, HSSFSheet sheet) {
		this.sheet = sheet;
		this.cfRule12Record = cfRule12Record;
		this.iconFormatting = this.cfRule12Record.getMultiStateFormatting();
	}

	public IconMultiStateFormatting.IconSet getIconSet() {
		return iconFormatting.getIconSet();
	}

	public void setIconSet(IconMultiStateFormatting.IconSet set) {
		iconFormatting.setIconSet(set);
	}

	public boolean isIconOnly() {
		return iconFormatting.isIconOnly();
	}

	public void setIconOnly(boolean only) {
		iconFormatting.setIconOnly(only);
	}

	public boolean isReversed() {
		return iconFormatting.isReversed();
	}

	public void setReversed(boolean reversed) {
		iconFormatting.setReversed(reversed);
	}

	public HSSFConditionalFormattingThreshold[] getThresholds() {
		Threshold[] t = iconFormatting.getThresholds();
		HSSFConditionalFormattingThreshold[] ht = new HSSFConditionalFormattingThreshold[t.length];
		for (int i = 0; i < (t.length); i++) {
		}
		return ht;
	}

	public void setThresholds(ConditionalFormattingThreshold[] thresholds) {
		Threshold[] t = new Threshold[thresholds.length];
		for (int i = 0; i < (t.length); i++) {
		}
		iconFormatting.setThresholds(t);
	}

	public HSSFConditionalFormattingThreshold createThreshold() {
		return null;
	}
}

