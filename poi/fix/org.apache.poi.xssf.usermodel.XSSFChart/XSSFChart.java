

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLFactory;
import org.apache.poi.ooxml.POIXMLRelation;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.ss.usermodel.Chart;
import org.apache.poi.ss.usermodel.charts.AxisPosition;
import org.apache.poi.ss.usermodel.charts.ChartAxis;
import org.apache.poi.ss.usermodel.charts.ChartAxisFactory;
import org.apache.poi.ss.usermodel.charts.ChartData;
import org.apache.poi.util.Removal;
import org.apache.poi.xddf.usermodel.chart.XDDFChart;
import org.apache.poi.xssf.usermodel.XSSFGraphicFrame;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.charts.XSSFCategoryAxis;
import org.apache.poi.xssf.usermodel.charts.XSSFChartAxis;
import org.apache.poi.xssf.usermodel.charts.XSSFChartDataFactory;
import org.apache.poi.xssf.usermodel.charts.XSSFChartLegend;
import org.apache.poi.xssf.usermodel.charts.XSSFDateAxis;
import org.apache.poi.xssf.usermodel.charts.XSSFManualLayout;
import org.apache.poi.xssf.usermodel.charts.XSSFValueAxis;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBoolean;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTCatAx;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChartSpace;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDateAx;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTHeaderFooter;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLayout;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPageMargins;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPageSetup;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPrintSettings;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTStrRef;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTTitle;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTTx;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTValAx;


public final class XSSFChart extends XDDFChart implements Chart , ChartAxisFactory {
	private XSSFGraphicFrame frame;

	@Deprecated
	@Removal(version = "4.2")
	List<XSSFChartAxis> axis = new ArrayList<>();

	protected XSSFChart() {
		super();
		createChart();
	}

	protected XSSFChart(PackagePart part) throws IOException, XmlException {
		super(part);
	}

	@Override
	protected POIXMLRelation getChartRelation() {
		return null;
	}

	@Override
	protected POIXMLRelation getChartWorkbookRelation() {
		return null;
	}

	@Override
	protected POIXMLFactory getChartFactory() {
		return null;
	}

	private void createChart() {
		CTPlotArea plotArea = getCTPlotArea();
		plotArea.addNewLayout();
		chart.addNewPlotVisOnly().setVal(true);
		CTPrintSettings printSettings = chartSpace.addNewPrintSettings();
		printSettings.addNewHeaderFooter();
		CTPageMargins pageMargins = printSettings.addNewPageMargins();
		pageMargins.setB(0.75);
		pageMargins.setL(0.7);
		pageMargins.setR(0.7);
		pageMargins.setT(0.75);
		pageMargins.setHeader(0.3);
		pageMargins.setFooter(0.3);
		printSettings.addNewPageSetup();
	}

	@Override
	protected void commit() throws IOException {
		XmlOptions xmlOptions = new XmlOptions(POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
		xmlOptions.setSaveSyntheticDocumentElement(new QName(CTChartSpace.type.getName().getNamespaceURI(), "chartSpace", "c"));
		PackagePart part = getPackagePart();
		try (OutputStream out = part.getOutputStream()) {
			chartSpace.save(out, xmlOptions);
		}
	}

	public XSSFGraphicFrame getGraphicFrame() {
		return frame;
	}

	protected void setGraphicFrame(XSSFGraphicFrame frame) {
		this.frame = frame;
	}

	@Override
	@Deprecated
	@Removal(version = "4.2")
	public XSSFChartDataFactory getChartDataFactory() {
		return XSSFChartDataFactory.getInstance();
	}

	@Override
	@Deprecated
	@Removal(version = "4.2")
	public XSSFChart getChartAxisFactory() {
		return this;
	}

	@Override
	@Deprecated
	@Removal(version = "4.2")
	public void plot(ChartData data, ChartAxis... chartAxis) {
		data.fillChart(this, chartAxis);
	}

	@Override
	@Deprecated
	@Removal(version = "4.2")
	public XSSFValueAxis createValueAxis(AxisPosition pos) {
		long id = (axis.size()) + 1;
		if ((axis.size()) == 1) {
			ChartAxis ax = axis.get(0);
		}
		return null;
	}

	@Override
	@Deprecated
	@Removal(version = "4.2")
	public XSSFCategoryAxis createCategoryAxis(AxisPosition pos) {
		long id = (axis.size()) + 1;
		if ((axis.size()) == 1) {
			ChartAxis ax = axis.get(0);
		}
		return null;
	}

	@Override
	@Deprecated
	@Removal(version = "4.2")
	public XSSFDateAxis createDateAxis(AxisPosition pos) {
		long id = (axis.size()) + 1;
		if ((axis.size()) == 1) {
			ChartAxis ax = axis.get(0);
		}
		return null;
	}

	@Override
	@Deprecated
	@Removal(version = "4.2")
	public List<? extends XSSFChartAxis> getAxis() {
		if ((axis.isEmpty()) && (hasAxis())) {
			parseAxis();
		}
		return axis;
	}

	@Override
	@Deprecated
	@Removal(version = "4.2")
	public XSSFManualLayout getManualLayout() {
		return null;
	}

	public XSSFRichTextString getTitleText() {
		if (!(chart.isSetTitle())) {
			return null;
		}
		CTTitle title = chart.getTitle();
		StringBuilder text = new StringBuilder(64);
		return new XSSFRichTextString(text.toString());
	}

	public String getTitleFormula() {
		if (!(chart.isSetTitle())) {
			return null;
		}
		CTTitle title = chart.getTitle();
		if (!(title.isSetTx())) {
			return null;
		}
		CTTx tx = title.getTx();
		if (!(tx.isSetStrRef())) {
			return null;
		}
		return tx.getStrRef().getF();
	}

	public void setTitleFormula(String formula) {
		CTTitle ctTitle;
		if (chart.isSetTitle()) {
			ctTitle = chart.getTitle();
		}else {
			ctTitle = chart.addNewTitle();
		}
		CTTx tx;
		if (ctTitle.isSetTx()) {
			tx = ctTitle.getTx();
		}else {
			tx = ctTitle.addNewTx();
		}
		if (tx.isSetRich()) {
			tx.unsetRich();
		}
		CTStrRef strRef;
		if (tx.isSetStrRef()) {
			strRef = tx.getStrRef();
		}else {
			strRef = tx.addNewStrRef();
		}
		strRef.setF(formula);
	}

	@Override
	@Deprecated
	@Removal(version = "4.2")
	public XSSFChartLegend getOrCreateLegend() {
		return null;
	}

	@Deprecated
	@Removal(version = "4.2")
	private boolean hasAxis() {
		CTPlotArea ctPlotArea = chart.getPlotArea();
		int totalAxisCount = (((ctPlotArea.sizeOfValAxArray()) + (ctPlotArea.sizeOfCatAxArray())) + (ctPlotArea.sizeOfDateAxArray())) + (ctPlotArea.sizeOfSerAxArray());
		return totalAxisCount > 0;
	}

	@Deprecated
	@Removal(version = "4.2")
	private void parseAxis() {
		parseCategoryAxis();
		parseDateAxis();
		parseValueAxis();
	}

	@Deprecated
	@Removal(version = "4.2")
	private void parseCategoryAxis() {
		for (CTCatAx catAx : chart.getPlotArea().getCatAxArray()) {
		}
	}

	@Deprecated
	@Removal(version = "4.2")
	private void parseDateAxis() {
		for (CTDateAx dateAx : chart.getPlotArea().getDateAxArray()) {
		}
	}

	@Deprecated
	@Removal(version = "4.2")
	private void parseValueAxis() {
		for (CTValAx valAx : chart.getPlotArea().getValAxArray()) {
		}
	}
}

