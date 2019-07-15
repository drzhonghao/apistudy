import org.apache.poi.xssf.usermodel.charts.*;


import org.apache.poi.ss.usermodel.charts.AxisCrosses;
import org.apache.poi.ss.usermodel.charts.AxisOrientation;
import org.apache.poi.ss.usermodel.charts.AxisPosition;
import org.apache.poi.ss.usermodel.charts.AxisTickMark;
import org.apache.poi.ss.usermodel.charts.ChartAxis;
import org.apache.poi.util.Internal;
import org.apache.poi.util.Removal;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTAxPos;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBoolean;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTCatAx;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChartLines;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTCrosses;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumFmt;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTScaling;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTTickMark;
import org.openxmlformats.schemas.drawingml.x2006.chart.STTickLblPos;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;

/**
 * Category axis type.
 *
 * @deprecated use {@link XDDFCategoryAxis} instead
 */
@Deprecated
@Removal(version="4.2")
public class XSSFCategoryAxis extends XSSFChartAxis {

	private CTCatAx ctCatAx;

	public XSSFCategoryAxis(XSSFChart chart, long id, AxisPosition pos) {
		super(chart);
		createAxis(id, pos);
	}

	public XSSFCategoryAxis(XSSFChart chart, CTCatAx ctCatAx) {
		super(chart);
		this.ctCatAx = ctCatAx;
	}

	@Override
	public long getId() {
		return ctCatAx.getAxId().getVal();
	}

	@Override
	@Internal
	public CTShapeProperties getLine() {
	    return ctCatAx.getSpPr();
	}

	@Override
	protected CTAxPos getCTAxPos() {
		return ctCatAx.getAxPos();
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
		return ctCatAx.getCrosses();
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

	@Override
	@Internal
	public CTChartLines getMajorGridLines() {
	    return ctCatAx.getMajorGridlines();
	}

	@Override
	public void crossAxis(ChartAxis axis) {
		ctCatAx.getCrossAx().setVal(axis.getId());
	}

	private void createAxis(long id, AxisPosition pos) {
		ctCatAx = chart.getCTChart().getPlotArea().addNewCatAx();
		ctCatAx.addNewAxId().setVal(id);
		ctCatAx.addNewAxPos();
		ctCatAx.addNewScaling();
		ctCatAx.addNewCrosses();
		ctCatAx.addNewCrossAx();
		ctCatAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);
		ctCatAx.addNewDelete();
		ctCatAx.addNewMajorTickMark();
		ctCatAx.addNewMinorTickMark();

		setPosition(pos);
		setOrientation(AxisOrientation.MIN_MAX);
		setCrosses(AxisCrosses.AUTO_ZERO);
		setVisible(true);
		setMajorTickMark(AxisTickMark.CROSS);
		setMinorTickMark(AxisTickMark.NONE);
	}

	@Override
    public boolean hasNumberFormat() {
        return ctCatAx.isSetNumFmt();
    }
}
