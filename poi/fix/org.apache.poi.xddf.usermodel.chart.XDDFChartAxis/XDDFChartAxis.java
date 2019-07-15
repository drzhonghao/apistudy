

import org.apache.poi.util.Beta;
import org.apache.poi.xddf.usermodel.HasShapeProperties;
import org.apache.poi.xddf.usermodel.XDDFShapeProperties;
import org.apache.poi.xddf.usermodel.chart.AxisCrosses;
import org.apache.poi.xddf.usermodel.chart.AxisOrientation;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.AxisTickMark;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTAxPos;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBoolean;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChartLines;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTCrosses;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDouble;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLogBase;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumFmt;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTScaling;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTTickMark;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTUnsignedInt;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;


@Beta
public abstract class XDDFChartAxis implements HasShapeProperties {
	protected abstract CTUnsignedInt getCTAxId();

	protected abstract CTAxPos getCTAxPos();

	protected abstract CTNumFmt getCTNumFmt();

	protected abstract CTScaling getCTScaling();

	protected abstract CTCrosses getCTCrosses();

	protected abstract CTBoolean getDelete();

	protected abstract CTTickMark getMajorCTTickMark();

	protected abstract CTTickMark getMinorCTTickMark();

	public abstract XDDFShapeProperties getOrAddMajorGridProperties();

	public abstract XDDFShapeProperties getOrAddMinorGridProperties();

	public abstract void setTitle(String text);

	public abstract boolean isSetMinorUnit();

	public abstract void setMinorUnit(double minor);

	public abstract double getMinorUnit();

	public abstract boolean isSetMajorUnit();

	public abstract void setMajorUnit(double major);

	public abstract double getMajorUnit();

	public long getId() {
		return getCTAxId().getVal();
	}

	public AxisPosition getPosition() {
		return null;
	}

	public void setPosition(AxisPosition position) {
	}

	public abstract boolean hasNumberFormat();

	public void setNumberFormat(String format) {
		getCTNumFmt().setFormatCode(format);
		getCTNumFmt().setSourceLinked(true);
	}

	public String getNumberFormat() {
		return getCTNumFmt().getFormatCode();
	}

	public boolean isSetLogBase() {
		return getCTScaling().isSetLogBase();
	}

	private static final double MIN_LOG_BASE = 2.0;

	private static final double MAX_LOG_BASE = 1000.0;

	public void setLogBase(double logBase) {
		if ((logBase < (XDDFChartAxis.MIN_LOG_BASE)) || ((XDDFChartAxis.MAX_LOG_BASE) < logBase)) {
			throw new IllegalArgumentException(("Axis log base must be between 2 and 1000 (inclusive), got: " + logBase));
		}
		CTScaling scaling = getCTScaling();
		if (scaling.isSetLogBase()) {
			scaling.getLogBase().setVal(logBase);
		}else {
			scaling.addNewLogBase().setVal(logBase);
		}
	}

	public double getLogBase() {
		CTScaling scaling = getCTScaling();
		if (scaling.isSetLogBase()) {
			return scaling.getLogBase().getVal();
		}
		return Double.NaN;
	}

	public boolean isSetMinimum() {
		return getCTScaling().isSetMin();
	}

	public void setMinimum(double min) {
		CTScaling scaling = getCTScaling();
		if (Double.isNaN(min)) {
			if (scaling.isSetMin()) {
				scaling.unsetMin();
			}
		}else {
			if (scaling.isSetMin()) {
				scaling.getMin().setVal(min);
			}else {
				scaling.addNewMin().setVal(min);
			}
		}
	}

	public double getMinimum() {
		CTScaling scaling = getCTScaling();
		if (scaling.isSetMin()) {
			return scaling.getMin().getVal();
		}else {
			return Double.NaN;
		}
	}

	public boolean isSetMaximum() {
		return getCTScaling().isSetMax();
	}

	public void setMaximum(double max) {
		CTScaling scaling = getCTScaling();
		if (Double.isNaN(max)) {
			if (scaling.isSetMax()) {
				scaling.unsetMax();
			}
		}else {
			if (scaling.isSetMax()) {
				scaling.getMax().setVal(max);
			}else {
				scaling.addNewMax().setVal(max);
			}
		}
	}

	public double getMaximum() {
		CTScaling scaling = getCTScaling();
		if (scaling.isSetMax()) {
			return scaling.getMax().getVal();
		}else {
			return Double.NaN;
		}
	}

	public AxisOrientation getOrientation() {
		return null;
	}

	public void setOrientation(AxisOrientation orientation) {
		CTScaling scaling = getCTScaling();
		if (scaling.isSetOrientation()) {
		}else {
		}
	}

	public AxisCrosses getCrosses() {
		return null;
	}

	public void setCrosses(AxisCrosses crosses) {
	}

	public abstract void crossAxis(XDDFChartAxis axis);

	public boolean isVisible() {
		return !(getDelete().getVal());
	}

	public void setVisible(boolean value) {
		getDelete().setVal((!value));
	}

	public AxisTickMark getMajorTickMark() {
		return null;
	}

	public void setMajorTickMark(AxisTickMark tickMark) {
	}

	public AxisTickMark getMinorTickMark() {
		return null;
	}

	public void setMinorTickMark(AxisTickMark tickMark) {
	}

	protected CTShapeProperties getOrAddLinesProperties(CTChartLines gridlines) {
		CTShapeProperties properties;
		if (gridlines.isSetSpPr()) {
			properties = gridlines.getSpPr();
		}else {
			properties = gridlines.addNewSpPr();
		}
		return properties;
	}

	protected long getNextAxId(CTPlotArea plotArea) {
		long totalAxisCount = (((plotArea.sizeOfValAxArray()) + (plotArea.sizeOfCatAxArray())) + (plotArea.sizeOfDateAxArray())) + (plotArea.sizeOfSerAxArray());
		return totalAxisCount;
	}
}

