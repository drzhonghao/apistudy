

import org.apache.poi.util.Beta;
import org.apache.poi.xddf.usermodel.XDDFShapeProperties;
import org.apache.poi.xddf.usermodel.chart.AxisCrossBetween;
import org.apache.poi.xddf.usermodel.chart.AxisCrosses;
import org.apache.poi.xddf.usermodel.chart.AxisOrientation;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.AxisTickMark;
import org.apache.poi.xddf.usermodel.chart.XDDFChartAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFTitle;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTAxPos;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTAxisUnit;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBoolean;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChartLines;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTCrossBetween;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTCrosses;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumFmt;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTScaling;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTTickLblPos;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTTickMark;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTTitle;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTUnsignedInt;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTValAx;
import org.openxmlformats.schemas.drawingml.x2006.chart.STTickLblPos;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;


@Beta
public class XDDFValueAxis extends XDDFChartAxis {
	private CTValAx ctValAx;

	public XDDFValueAxis(CTPlotArea plotArea, AxisPosition position) {
		initializeAxis(plotArea, position);
	}

	public XDDFValueAxis(CTValAx ctValAx) {
		this.ctValAx = ctValAx;
	}

	@Override
	public XDDFShapeProperties getOrAddMajorGridProperties() {
		CTChartLines majorGridlines;
		if (ctValAx.isSetMajorGridlines()) {
			majorGridlines = ctValAx.getMajorGridlines();
		}else {
			majorGridlines = ctValAx.addNewMajorGridlines();
		}
		return new XDDFShapeProperties(getOrAddLinesProperties(majorGridlines));
	}

	@Override
	public XDDFShapeProperties getOrAddMinorGridProperties() {
		CTChartLines minorGridlines;
		if (ctValAx.isSetMinorGridlines()) {
			minorGridlines = ctValAx.getMinorGridlines();
		}else {
			minorGridlines = ctValAx.addNewMinorGridlines();
		}
		return new XDDFShapeProperties(getOrAddLinesProperties(minorGridlines));
	}

	@Override
	public XDDFShapeProperties getOrAddShapeProperties() {
		CTShapeProperties properties;
		if (ctValAx.isSetSpPr()) {
			properties = ctValAx.getSpPr();
		}else {
			properties = ctValAx.addNewSpPr();
		}
		return new XDDFShapeProperties(properties);
	}

	@Override
	public void setTitle(String text) {
		if (!(ctValAx.isSetTitle())) {
			ctValAx.addNewTitle();
		}
		XDDFTitle title = new XDDFTitle(null, ctValAx.getTitle());
		title.setOverlay(false);
		title.setText(text);
	}

	@Override
	public boolean isSetMinorUnit() {
		return ctValAx.isSetMinorUnit();
	}

	@Override
	public void setMinorUnit(double minor) {
		if (Double.isNaN(minor)) {
			if (ctValAx.isSetMinorUnit()) {
				ctValAx.unsetMinorUnit();
			}
		}else {
			if (ctValAx.isSetMinorUnit()) {
				ctValAx.getMinorUnit().setVal(minor);
			}else {
				ctValAx.addNewMinorUnit().setVal(minor);
			}
		}
	}

	@Override
	public double getMinorUnit() {
		if (ctValAx.isSetMinorUnit()) {
			return ctValAx.getMinorUnit().getVal();
		}else {
			return Double.NaN;
		}
	}

	@Override
	public boolean isSetMajorUnit() {
		return ctValAx.isSetMajorUnit();
	}

	@Override
	public void setMajorUnit(double major) {
		if (Double.isNaN(major)) {
			if (ctValAx.isSetMajorUnit()) {
				ctValAx.unsetMajorUnit();
			}
		}else {
			if (ctValAx.isSetMajorUnit()) {
				ctValAx.getMajorUnit().setVal(major);
			}else {
				ctValAx.addNewMajorUnit().setVal(major);
			}
		}
	}

	@Override
	public double getMajorUnit() {
		if (ctValAx.isSetMajorUnit()) {
			return ctValAx.getMajorUnit().getVal();
		}else {
			return Double.NaN;
		}
	}

	@Override
	public void crossAxis(XDDFChartAxis axis) {
		ctValAx.getCrossAx().setVal(axis.getId());
	}

	@Override
	protected CTUnsignedInt getCTAxId() {
		return ctValAx.getAxId();
	}

	@Override
	protected CTAxPos getCTAxPos() {
		return ctValAx.getAxPos();
	}

	@Override
	public boolean hasNumberFormat() {
		return ctValAx.isSetNumFmt();
	}

	@Override
	protected CTNumFmt getCTNumFmt() {
		if (ctValAx.isSetNumFmt()) {
			return ctValAx.getNumFmt();
		}
		return ctValAx.addNewNumFmt();
	}

	@Override
	protected CTScaling getCTScaling() {
		return ctValAx.getScaling();
	}

	@Override
	protected CTCrosses getCTCrosses() {
		CTCrosses crosses = ctValAx.getCrosses();
		if (crosses == null) {
			return ctValAx.addNewCrosses();
		}else {
			return crosses;
		}
	}

	@Override
	protected CTBoolean getDelete() {
		return ctValAx.getDelete();
	}

	@Override
	protected CTTickMark getMajorCTTickMark() {
		return ctValAx.getMajorTickMark();
	}

	@Override
	protected CTTickMark getMinorCTTickMark() {
		return ctValAx.getMinorTickMark();
	}

	public AxisCrossBetween getCrossBetween() {
		return null;
	}

	public void setCrossBetween(AxisCrossBetween crossBetween) {
	}

	private void initializeAxis(CTPlotArea plotArea, AxisPosition position) {
		final long id = getNextAxId(plotArea);
		ctValAx = plotArea.addNewValAx();
		ctValAx.addNewAxId().setVal(id);
		ctValAx.addNewAxPos();
		ctValAx.addNewScaling();
		ctValAx.addNewCrossBetween();
		ctValAx.addNewCrosses();
		ctValAx.addNewCrossAx();
		ctValAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);
		ctValAx.addNewDelete();
		ctValAx.addNewMajorTickMark();
		ctValAx.addNewMinorTickMark();
		setPosition(position);
		setOrientation(AxisOrientation.MIN_MAX);
		setCrossBetween(AxisCrossBetween.MIDPOINT_CATEGORY);
		setCrosses(AxisCrosses.AUTO_ZERO);
		setVisible(true);
		setMajorTickMark(AxisTickMark.CROSS);
		setMinorTickMark(AxisTickMark.NONE);
	}
}

