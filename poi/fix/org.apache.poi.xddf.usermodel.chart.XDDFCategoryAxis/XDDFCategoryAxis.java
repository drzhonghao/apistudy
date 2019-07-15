

import org.apache.poi.util.Beta;
import org.apache.poi.xddf.usermodel.XDDFShapeProperties;
import org.apache.poi.xddf.usermodel.chart.AxisCrosses;
import org.apache.poi.xddf.usermodel.chart.AxisLabelAlignment;
import org.apache.poi.xddf.usermodel.chart.AxisOrientation;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.AxisTickMark;
import org.apache.poi.xddf.usermodel.chart.XDDFChartAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFTitle;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTAxPos;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBoolean;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTCatAx;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChartLines;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTCrosses;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumFmt;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTScaling;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTTickLblPos;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTTickMark;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTTitle;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTUnsignedInt;
import org.openxmlformats.schemas.drawingml.x2006.chart.STTickLblPos;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;


@Beta
public class XDDFCategoryAxis extends XDDFChartAxis {
	private CTCatAx ctCatAx;

	public XDDFCategoryAxis(CTPlotArea plotArea, AxisPosition position) {
		initializeAxis(plotArea, position);
	}

	public XDDFCategoryAxis(CTCatAx ctCatAx) {
		this.ctCatAx = ctCatAx;
	}

	@Override
	public XDDFShapeProperties getOrAddMajorGridProperties() {
		CTChartLines majorGridlines;
		if (ctCatAx.isSetMajorGridlines()) {
			majorGridlines = ctCatAx.getMajorGridlines();
		}else {
			majorGridlines = ctCatAx.addNewMajorGridlines();
		}
		return new XDDFShapeProperties(getOrAddLinesProperties(majorGridlines));
	}

	@Override
	public XDDFShapeProperties getOrAddMinorGridProperties() {
		CTChartLines minorGridlines;
		if (ctCatAx.isSetMinorGridlines()) {
			minorGridlines = ctCatAx.getMinorGridlines();
		}else {
			minorGridlines = ctCatAx.addNewMinorGridlines();
		}
		return new XDDFShapeProperties(getOrAddLinesProperties(minorGridlines));
	}

	@Override
	public XDDFShapeProperties getOrAddShapeProperties() {
		CTShapeProperties properties;
		if (ctCatAx.isSetSpPr()) {
			properties = ctCatAx.getSpPr();
		}else {
			properties = ctCatAx.addNewSpPr();
		}
		return new XDDFShapeProperties(properties);
	}

	@Override
	public void setTitle(String text) {
		if (!(ctCatAx.isSetTitle())) {
			ctCatAx.addNewTitle();
		}
		XDDFTitle title = new XDDFTitle(null, ctCatAx.getTitle());
		title.setOverlay(false);
		title.setText(text);
	}

	@Override
	public boolean isSetMinorUnit() {
		return false;
	}

	@Override
	public void setMinorUnit(double minor) {
	}

	@Override
	public double getMinorUnit() {
		return Double.NaN;
	}

	@Override
	public boolean isSetMajorUnit() {
		return false;
	}

	@Override
	public void setMajorUnit(double major) {
	}

	@Override
	public double getMajorUnit() {
		return Double.NaN;
	}

	@Override
	public void crossAxis(XDDFChartAxis axis) {
		ctCatAx.getCrossAx().setVal(axis.getId());
	}

	@Override
	protected CTUnsignedInt getCTAxId() {
		return ctCatAx.getAxId();
	}

	@Override
	protected CTAxPos getCTAxPos() {
		return ctCatAx.getAxPos();
	}

	@Override
	public boolean hasNumberFormat() {
		return ctCatAx.isSetNumFmt();
	}

	@Override
	protected CTNumFmt getCTNumFmt() {
		if (ctCatAx.isSetNumFmt()) {
			return ctCatAx.getNumFmt();
		}
		return ctCatAx.addNewNumFmt();
	}

	@Override
	protected CTScaling getCTScaling() {
		return ctCatAx.getScaling();
	}

	@Override
	protected CTCrosses getCTCrosses() {
		CTCrosses crosses = ctCatAx.getCrosses();
		if (crosses == null) {
			return ctCatAx.addNewCrosses();
		}else {
			return crosses;
		}
	}

	@Override
	protected CTBoolean getDelete() {
		return ctCatAx.getDelete();
	}

	@Override
	protected CTTickMark getMajorCTTickMark() {
		return ctCatAx.getMajorTickMark();
	}

	@Override
	protected CTTickMark getMinorCTTickMark() {
		return ctCatAx.getMinorTickMark();
	}

	public AxisLabelAlignment getLabelAlignment() {
		return null;
	}

	public void setLabelAlignment(AxisLabelAlignment labelAlignment) {
	}

	private void initializeAxis(CTPlotArea plotArea, AxisPosition position) {
		final long id = getNextAxId(plotArea);
		ctCatAx = plotArea.addNewCatAx();
		ctCatAx.addNewAxId().setVal(id);
		ctCatAx.addNewAxPos();
		ctCatAx.addNewScaling();
		ctCatAx.addNewCrosses();
		ctCatAx.addNewCrossAx();
		ctCatAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);
		ctCatAx.addNewDelete();
		ctCatAx.addNewMajorTickMark();
		ctCatAx.addNewMinorTickMark();
		setPosition(position);
		setOrientation(AxisOrientation.MIN_MAX);
		setCrosses(AxisCrosses.AUTO_ZERO);
		setVisible(true);
		setMajorTickMark(AxisTickMark.CROSS);
		setMinorTickMark(AxisTickMark.NONE);
	}
}

