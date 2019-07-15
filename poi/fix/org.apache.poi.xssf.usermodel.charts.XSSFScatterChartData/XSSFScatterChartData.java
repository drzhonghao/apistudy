

import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Chart;
import org.apache.poi.ss.usermodel.charts.ChartAxis;
import org.apache.poi.ss.usermodel.charts.ChartDataSource;
import org.apache.poi.ss.usermodel.charts.ScatterChartData;
import org.apache.poi.ss.usermodel.charts.ScatterChartSeries;
import org.apache.poi.util.Removal;
import org.apache.poi.xddf.usermodel.chart.XDDFChart;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.charts.AbstractXSSFChartSeries;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTAxDataSource;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumDataSource;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTScatterChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTScatterSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTScatterStyle;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTSerTx;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTUnsignedInt;
import org.openxmlformats.schemas.drawingml.x2006.chart.STScatterStyle;


@Deprecated
@Removal(version = "4.2")
public class XSSFScatterChartData implements ScatterChartData {
	private List<XSSFScatterChartData.Series> series;

	public XSSFScatterChartData() {
		series = new ArrayList<XSSFScatterChartData.Series>();
	}

	static class Series extends AbstractXSSFChartSeries implements ScatterChartSeries {
		private int id;

		private int order;

		private ChartDataSource<?> xs;

		private ChartDataSource<? extends Number> ys;

		protected Series(int id, int order, ChartDataSource<?> xs, ChartDataSource<? extends Number> ys) {
			super();
			this.id = id;
			this.order = order;
			this.xs = xs;
			this.ys = ys;
		}

		@Override
		public ChartDataSource<?> getXValues() {
			return xs;
		}

		@Override
		public ChartDataSource<? extends Number> getYValues() {
			return ys;
		}

		protected void addToChart(CTScatterChart ctScatterChart) {
			CTScatterSer scatterSer = ctScatterChart.addNewSer();
			scatterSer.addNewIdx().setVal(this.id);
			scatterSer.addNewOrder().setVal(this.order);
			CTAxDataSource xVal = scatterSer.addNewXVal();
			CTNumDataSource yVal = scatterSer.addNewYVal();
			if (isTitleSet()) {
				scatterSer.setTx(getCTSerTx());
			}
		}
	}

	@Override
	public ScatterChartSeries addSerie(ChartDataSource<?> xs, ChartDataSource<? extends Number> ys) {
		if (!(ys.isNumeric())) {
			throw new IllegalArgumentException("Y axis data source must be numeric.");
		}
		int numOfSeries = series.size();
		XSSFScatterChartData.Series newSerie = new XSSFScatterChartData.Series(numOfSeries, numOfSeries, xs, ys);
		series.add(newSerie);
		return newSerie;
	}

	@Override
	public void fillChart(Chart chart, ChartAxis... axis) {
		if (!(chart instanceof XSSFChart)) {
			throw new IllegalArgumentException("Chart must be instance of XSSFChart");
		}
		XSSFChart xssfChart = ((XSSFChart) (chart));
		CTPlotArea plotArea = xssfChart.getCTChart().getPlotArea();
		CTScatterChart scatterChart = plotArea.addNewScatterChart();
		addStyle(scatterChart);
		for (XSSFScatterChartData.Series s : series) {
			s.addToChart(scatterChart);
		}
		for (ChartAxis ax : axis) {
			scatterChart.addNewAxId().setVal(ax.getId());
		}
	}

	@Override
	public List<? extends XSSFScatterChartData.Series> getSeries() {
		return series;
	}

	private void addStyle(CTScatterChart ctScatterChart) {
		CTScatterStyle scatterStyle = ctScatterChart.addNewScatterStyle();
		scatterStyle.setVal(STScatterStyle.LINE_MARKER);
	}
}

