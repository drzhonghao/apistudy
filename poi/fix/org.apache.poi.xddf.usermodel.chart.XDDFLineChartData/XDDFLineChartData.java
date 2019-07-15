

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.util.Beta;
import org.apache.poi.xddf.usermodel.XDDFShapeProperties;
import org.apache.poi.xddf.usermodel.chart.Grouping;
import org.apache.poi.xddf.usermodel.chart.MarkerStyle;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFChartAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTAxDataSource;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBoolean;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDLbls;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLineChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLineSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTMarker;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTMarkerSize;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumDataSource;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTSerTx;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTUnsignedInt;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;


@Beta
public class XDDFLineChartData extends XDDFChartData {
	private CTLineChart chart;

	public XDDFLineChartData(CTLineChart chart, Map<Long, XDDFChartAxis> categories, Map<Long, XDDFValueAxis> values) {
		this.chart = chart;
		for (CTLineSer series : chart.getSerList()) {
			this.series.add(new XDDFLineChartData.Series(series, series.getCat(), series.getVal()));
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

	public Grouping getGrouping() {
		return null;
	}

	public void setGrouping(Grouping grouping) {
	}

	@Override
	public XDDFChartData.Series addSeries(XDDFDataSource<?> category, XDDFNumericalDataSource<? extends Number> values) {
		final int index = this.series.size();
		final CTLineSer ctSer = this.chart.addNewSer();
		ctSer.addNewCat();
		ctSer.addNewVal();
		ctSer.addNewIdx().setVal(index);
		ctSer.addNewOrder().setVal(index);
		final XDDFLineChartData.Series added = new XDDFLineChartData.Series(ctSer, category, values);
		this.series.add(added);
		return added;
	}

	public class Series extends XDDFChartData.Series {
		private CTLineSer series;

		protected Series(CTLineSer series, XDDFDataSource<?> category, XDDFNumericalDataSource<? extends Number> values) {
			super(category, values);
			this.series = series;
		}

		protected Series(CTLineSer series, CTAxDataSource category, CTNumDataSource values) {
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

		public Boolean getSmooth() {
			if (series.isSetSmooth()) {
				return series.getSmooth().getVal();
			}else {
				return null;
			}
		}

		public void setSmooth(Boolean smooth) {
			if (smooth == null) {
				if (series.isSetSmooth()) {
					series.unsetSmooth();
				}
			}else {
				if (series.isSetSmooth()) {
					series.getSmooth().setVal(smooth);
				}else {
					series.addNewSmooth().setVal(smooth);
				}
			}
		}

		public void setMarkerSize(short size) {
			if ((size < 2) || (72 < size)) {
				throw new IllegalArgumentException("Minimum inclusive: 2; Maximum inclusive: 72");
			}
			CTMarker marker = getMarker();
			if (marker.isSetSize()) {
				marker.getSize().setVal(size);
			}else {
				marker.addNewSize().setVal(size);
			}
		}

		public void setMarkerStyle(MarkerStyle style) {
			CTMarker marker = getMarker();
			if (marker.isSetSymbol()) {
			}else {
			}
		}

		private CTMarker getMarker() {
			if (series.isSetMarker()) {
				return series.getMarker();
			}else {
				return series.addNewMarker();
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

