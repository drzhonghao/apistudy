

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.util.Beta;
import org.apache.poi.xddf.usermodel.XDDFShapeProperties;
import org.apache.poi.xddf.usermodel.chart.BarDirection;
import org.apache.poi.xddf.usermodel.chart.BarGrouping;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFChartAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTAxDataSource;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarDir;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBoolean;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDLbls;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTGapAmount;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumDataSource;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTSerTx;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTUnsignedInt;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;


@Beta
public class XDDFBarChartData extends XDDFChartData {
	private CTBarChart chart;

	public XDDFBarChartData(CTBarChart chart, Map<Long, XDDFChartAxis> categories, Map<Long, XDDFValueAxis> values) {
		this.chart = chart;
		if ((chart.getBarDir()) == null) {
		}
		for (CTBarSer series : chart.getSerList()) {
			this.series.add(new XDDFBarChartData.Series(series, series.getCat(), series.getVal()));
		}
		defineAxes(categories, values);
	}

	private void defineAxes(Map<Long, XDDFChartAxis> categories, Map<Long, XDDFValueAxis> values) {
		if ((chart.sizeOfAxIdArray()) == 0) {
			for (Long id : categories.keySet()) {
				chart.addNewAxId().setVal(id);
			}
			for (Long id : values.keySet()) {
				chart.addNewAxId().setVal(id);
			}
		}
		defineAxes(chart.getAxIdArray(), categories, values);
	}

	@Override
	public void setVaryColors(boolean varyColors) {
		if (chart.isSetVaryColors()) {
			chart.getVaryColors().setVal(varyColors);
		}else {
			chart.addNewVaryColors().setVal(varyColors);
		}
	}

	public BarDirection getBarDirection() {
		return null;
	}

	public void setBarDirection(BarDirection direction) {
	}

	public BarGrouping getBarGrouping() {
		if (chart.isSetGrouping()) {
		}else {
			return BarGrouping.STANDARD;
		}
		return null;
	}

	public void setBarGrouping(BarGrouping grouping) {
		if (chart.isSetGrouping()) {
		}else {
		}
	}

	public int getGapWidth() {
		if (chart.isSetGapWidth()) {
			return chart.getGapWidth().getVal();
		}else {
			return 0;
		}
	}

	public void setGapWidth(int width) {
		if (chart.isSetGapWidth()) {
			chart.getGapWidth().setVal(width);
		}else {
			chart.addNewGapWidth().setVal(width);
		}
	}

	@Override
	public XDDFChartData.Series addSeries(XDDFDataSource<?> category, XDDFNumericalDataSource<? extends Number> values) {
		final int index = this.series.size();
		final CTBarSer ctSer = this.chart.addNewSer();
		ctSer.addNewTx();
		ctSer.addNewCat();
		ctSer.addNewVal();
		ctSer.addNewIdx().setVal(index);
		ctSer.addNewOrder().setVal(index);
		final XDDFBarChartData.Series added = new XDDFBarChartData.Series(ctSer, category, values);
		this.series.add(added);
		return added;
	}

	public class Series extends XDDFChartData.Series {
		private CTBarSer series;

		protected Series(CTBarSer series, XDDFDataSource<?> category, XDDFNumericalDataSource<? extends Number> values) {
			super(category, values);
			this.series = series;
		}

		protected Series(CTBarSer series, CTAxDataSource category, CTNumDataSource values) {
			super(XDDFDataSourcesFactory.fromDataSource(category), XDDFDataSourcesFactory.fromDataSource(values));
			this.series = series;
		}

		@Override
		protected CTSerTx getSeriesText() {
			if (series.isSetTx()) {
				return series.getTx();
			}else {
				return series.addNewTx();
			}
		}

		@Override
		public void setShowLeaderLines(boolean showLeaderLines) {
			if (!(series.isSetDLbls())) {
				series.addNewDLbls();
			}
			if (series.getDLbls().isSetShowLeaderLines()) {
				series.getDLbls().getShowLeaderLines().setVal(showLeaderLines);
			}else {
				series.getDLbls().addNewShowLeaderLines().setVal(showLeaderLines);
			}
		}

		@Override
		public XDDFShapeProperties getShapeProperties() {
			if (series.isSetSpPr()) {
				return new XDDFShapeProperties(series.getSpPr());
			}else {
				return null;
			}
		}

		@Override
		public void setShapeProperties(XDDFShapeProperties properties) {
			if (properties == null) {
				if (series.isSetSpPr()) {
					series.unsetSpPr();
				}
			}else {
				if (series.isSetSpPr()) {
					series.setSpPr(properties.getXmlObject());
				}else {
					series.addNewSpPr().set(properties.getXmlObject());
				}
			}
		}

		@Override
		protected CTAxDataSource getAxDS() {
			return series.getCat();
		}

		@Override
		protected CTNumDataSource getNumDS() {
			return series.getVal();
		}
	}
}

