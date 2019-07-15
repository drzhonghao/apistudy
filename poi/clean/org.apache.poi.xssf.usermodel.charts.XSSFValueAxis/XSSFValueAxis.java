import org.apache.poi.xssf.usermodel.charts.*;


import org.apache.poi.ss.usermodel.charts.AxisCrossBetween;
import org.apache.poi.ss.usermodel.charts.AxisCrosses;
import org.apache.poi.ss.usermodel.charts.AxisOrientation;
import org.apache.poi.ss.usermodel.charts.AxisPosition;
import org.apache.poi.ss.usermodel.charts.AxisTickMark;
import org.apache.poi.ss.usermodel.charts.ChartAxis;
import org.apache.poi.ss.usermodel.charts.ValueAxis;
import org.apache.poi.util.Internal;
import org.apache.poi.util.Removal;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTAxPos;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBoolean;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChartLines;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTCrosses;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumFmt;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTScaling;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTTickMark;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTValAx;
import org.openxmlformats.schemas.drawingml.x2006.chart.STCrossBetween;
import org.openxmlformats.schemas.drawingml.x2006.chart.STTickLblPos;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;

/**
 * Value axis type.
 *
 * @deprecated use {@link XDDFValueAxis} instead
 */
@Deprecated
@Removal(version="4.2")
public class XSSFValueAxis extends XSSFChartAxis implements ValueAxis {

	private CTValAx ctValAx;

	public XSSFValueAxis(XSSFChart chart, long id, AxisPosition pos) {
		super(chart);
		createAxis(id, pos);
	}

	public XSSFValueAxis(XSSFChart chart, CTValAx ctValAx) {
		super(chart);
		this.ctValAx = ctValAx;
	}

	@Override
	public long getId() {
		return ctValAx.getAxId().getVal();
	}

	@Override
	@Internal
    public CTShapeProperties getLine() {
        return ctValAx.getSpPr();
    }

	@Override
	public void setCrossBetween(AxisCrossBetween crossBetween) {
		ctValAx.getCrossBetween().setVal(fromCrossBetween(crossBetween));
	}

	@Override
	public AxisCrossBetween getCrossBetween() {
		return toCrossBetween(ctValAx.getCrossBetween().getVal());
	}

	@Override
	protected CTAxPos getCTAxPos() {
		return ctValAx.getAxPos();
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
		return ctValAx.getCrosses();
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

	@Override
	@Internal
    public CTChartLines getMajorGridLines() {
        return ctValAx.getMajorGridlines();
    }

    @Override
	public void crossAxis(ChartAxis axis) {
		ctValAx.getCrossAx().setVal(axis.getId());
	}

	private void createAxis(long id, AxisPosition pos) {
		ctValAx = chart.getCTChart().getPlotArea().addNewValAx();
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

		setPosition(pos);
		setOrientation(AxisOrientation.MIN_MAX);
		setCrossBetween(AxisCrossBetween.MIDPOINT_CATEGORY);
		setCrosses(AxisCrosses.AUTO_ZERO);
		setVisible(true);
		setMajorTickMark(AxisTickMark.CROSS);
		setMinorTickMark(AxisTickMark.NONE);
	}

	private static STCrossBetween.Enum fromCrossBetween(AxisCrossBetween crossBetween) {
		switch (crossBetween) {
			case BETWEEN: return STCrossBetween.BETWEEN;
			case MIDPOINT_CATEGORY: return STCrossBetween.MID_CAT;
			default:
				throw new IllegalArgumentException();
		}
	}

	private static AxisCrossBetween toCrossBetween(STCrossBetween.Enum ctCrossBetween) {
		switch (ctCrossBetween.intValue()) {
			case STCrossBetween.INT_BETWEEN: return AxisCrossBetween.BETWEEN;
			case STCrossBetween.INT_MID_CAT: return AxisCrossBetween.MIDPOINT_CATEGORY;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
    public boolean hasNumberFormat() {
        return ctValAx.isSetNumFmt();
    }
}
