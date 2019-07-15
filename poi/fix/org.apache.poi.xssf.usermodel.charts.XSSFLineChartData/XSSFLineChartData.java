

import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Chart;
import org.apache.poi.ss.usermodel.charts.ChartAxis;
import org.apache.poi.ss.usermodel.charts.ChartDataSource;
import org.apache.poi.ss.usermodel.charts.LineChartData;
import org.apache.poi.ss.usermodel.charts.LineChartSeries;
import org.apache.poi.util.Removal;
import org.apache.poi.xddf.usermodel.chart.XDDFChart;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.charts.AbstractXSSFChartSeries;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTAxDataSource;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBoolean;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLineChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLineSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTMarker;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTMarkerStyle;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumDataSource;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTSerTx;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTUnsignedInt;
import org.openxmlformats.schemas.drawingml.x2006.chart.STMarkerStyle;


@Deprecated
@Removal(version = "4.2")
public class XSSFLineChartData implements LineChartData {
	private List<XSSFLineChartData.Series> series;

	public XSSFLineChartData() {
		series = new ArrayList<XSSFLineChartData.Series>();
	}

	static class Series extends AbstractXSSFChartSeries implements LineChartSeries {
		private int id;

		private int order;

		private ChartDataSource<?> categories;

		private ChartDataSource<? extends Number> values;

		protected Series(int id, int order, ChartDataSource<?> categories, ChartDataSource<? extends Number> values) {
			this.id = id;
			this.order = order;
			this.categories = categories;
			this.values = values;
		}

		@Override
		public ChartDataSource<?> getCategoryAxisData() {
			return categories;
		}

		@Override
		public ChartDataSource<? extends Number> getValues() {
			return values;
		}

		protected void addToChart(CTLineChart ctLineChart) {
			CTLineSer ctLineSer = ctLineChart.addNewSer();
			ctLineSer.addNewIdx().setVal(id);
			ctLineSer.addNewOrder().setVal(order);
			ctLineSer.addNewMarker().addNewSymbol().setVal(STMarkerStyle.NONE);
			CTAxDataSource catDS = ctLineSer.addNewCat();
			CTNumDataSource valueDS = ctLineSer.addNewVal();
			if (isTitleSet()) {
				ctLineSer.setTx(getCTSerTx());
			}
		}
	}

	@Override
	public LineChartSeries addSeries(ChartDataSource<?> categoryAxisData, ChartDataSource<? extends Number> values) {
		if (!(values.isNumeric())) {
			throw new IllegalArgumentException("Value data source must be numeric.");
		}
		int numOfSeries = series.size();
		XSSFLineChartData.Series newSeries = new XSSFLineChartData.Series(numOfSeries, numOfSeries, categoryAxisData, values);
		series.add(newSeries);
		return newSeries;
	}

	@Override
	public List<? extends LineChartSeries> getSeries() {
		return series;
	}

	@Override
	public void fillChart(Chart chart, ChartAxis... axis) {
		if (!(chart instanceof XSSFChart)) {
			throw new IllegalArgumentException("Chart must be instance of XSSFChart");
		}
		XSSFChart xssfChart = ((XSSFChart) (chart));
		CTPlotArea plotArea = xssfChart.getCTChart().getPlotArea();
		CTLineChart lineChart = plotArea.addNewLineChart();
		lineChart.addNewVaryColors().setVal(false);
		for (XSSFLineChartData.Series s : series) {
			s.addToChart(lineChart);
		}
		for (ChartAxis ax : axis) {
			lineChart.addNewAxId().setVal(ax.getId());
		}
	}
}

