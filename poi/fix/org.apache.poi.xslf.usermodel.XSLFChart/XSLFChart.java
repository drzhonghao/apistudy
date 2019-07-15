

import java.io.IOException;
import org.apache.poi.ooxml.POIXMLFactory;
import org.apache.poi.ooxml.POIXMLRelation;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.util.Beta;
import org.apache.poi.xddf.usermodel.chart.XDDFChart;
import org.apache.poi.xslf.usermodel.XSLFFactory;
import org.apache.poi.xslf.usermodel.XSLFRelation;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.xmlbeans.XmlException;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTTitle;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTTx;


@Beta
public final class XSLFChart extends XDDFChart {
	protected XSLFChart() {
		super();
	}

	protected XSLFChart(PackagePart part) throws IOException, XmlException {
		super(part);
	}

	@Override
	protected POIXMLRelation getChartRelation() {
		return XSLFRelation.CHART;
	}

	@Override
	protected POIXMLRelation getChartWorkbookRelation() {
		return XSLFRelation.WORKBOOK;
	}

	@Override
	protected POIXMLFactory getChartFactory() {
		return XSLFFactory.getInstance();
	}

	public XSLFTextShape getTitleShape() {
		if (!(chart.isSetTitle())) {
			chart.addNewTitle();
		}
		final CTTitle title = chart.getTitle();
		if (((title.getTx()) != null) && (title.getTx().isSetRich())) {
		}else {
		}
		return null;
	}
}

