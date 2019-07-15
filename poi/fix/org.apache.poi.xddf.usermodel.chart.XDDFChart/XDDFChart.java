

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.xml.namespace.QName;
import org.apache.poi.ooxml.POIXMLDocument;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ooxml.POIXMLFactory;
import org.apache.poi.ooxml.POIXMLRelation;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.Beta;
import org.apache.poi.xddf.usermodel.XDDFShapeProperties;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.DisplayBlanks;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFChartAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFChartLegend;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDateAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFLineChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFManualLayout;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFPieChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFRadarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFScatterChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFSeriesAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFTitle;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xddf.usermodel.text.TextContainer;
import org.apache.poi.xddf.usermodel.text.XDDFTextBody;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBoolean;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTCatAx;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChartSpace;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDateAx;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTExternalData;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLineChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPieChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTRadarChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTScatterChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTSerAx;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTSurface;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTTitle;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTUnsignedInt;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTValAx;
import org.openxmlformats.schemas.drawingml.x2006.chart.ChartSpaceDocument;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraphProperties;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumn;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumns;

import static org.openxmlformats.schemas.drawingml.x2006.chart.CTBoolean.Factory.newInstance;
import static org.openxmlformats.schemas.drawingml.x2006.chart.ChartSpaceDocument.Factory.parse;


@Beta
public abstract class XDDFChart extends POIXMLDocumentPart implements TextContainer {
	private XSSFWorkbook workbook;

	private int chartIndex = 0;

	private POIXMLDocumentPart documentPart = null;

	protected List<XDDFChartAxis> axes = new ArrayList<>();

	protected final CTChartSpace chartSpace;

	protected final CTChart chart;

	protected XDDFChart() {
		super();
		chartSpace = CTChartSpace.Factory.newInstance();
		chart = chartSpace.addNewChart();
		chart.addNewPlotArea();
	}

	protected XDDFChart(PackagePart part) throws IOException, XmlException {
		super(part);
		chartSpace = parse(part.getInputStream(), POIXMLTypeLoader.DEFAULT_XML_OPTIONS).getChartSpace();
		chart = chartSpace.getChart();
	}

	@org.apache.poi.util.Internal
	public CTChartSpace getCTChartSpace() {
		return chartSpace;
	}

	@org.apache.poi.util.Internal
	public CTChart getCTChart() {
		return chart;
	}

	@org.apache.poi.util.Internal
	protected CTPlotArea getCTPlotArea() {
		return chart.getPlotArea();
	}

	public boolean isPlotOnlyVisibleCells() {
		if (chart.isSetPlotVisOnly()) {
			return chart.getPlotVisOnly().getVal();
		}else {
			return false;
		}
	}

	public void setPlotOnlyVisibleCells(boolean only) {
		if (!(chart.isSetPlotVisOnly())) {
			chart.setPlotVisOnly(newInstance());
		}
		chart.getPlotVisOnly().setVal(only);
	}

	public void setFloor(int thickness) {
		if (!(chart.isSetFloor())) {
			chart.setFloor(CTSurface.Factory.newInstance());
		}
		chart.getFloor().getThickness().setVal(thickness);
	}

	public void setBackWall(int thickness) {
		if (!(chart.isSetBackWall())) {
			chart.setBackWall(CTSurface.Factory.newInstance());
		}
		chart.getBackWall().getThickness().setVal(thickness);
	}

	public void setSideWall(int thickness) {
		if (!(chart.isSetSideWall())) {
			chart.setSideWall(CTSurface.Factory.newInstance());
		}
		chart.getSideWall().getThickness().setVal(thickness);
	}

	public void setAutoTitleDeleted(boolean deleted) {
		if (!(chart.isSetAutoTitleDeleted())) {
			chart.setAutoTitleDeleted(newInstance());
		}
		chart.getAutoTitleDeleted().setVal(deleted);
	}

	public void displayBlanksAs(DisplayBlanks as) {
		if (as == null) {
			if (chart.isSetDispBlanksAs()) {
				chart.unsetDispBlanksAs();
			}
		}else {
			if (chart.isSetDispBlanksAs()) {
			}else {
			}
		}
	}

	public Boolean getTitleOverlay() {
		if (chart.isSetTitle()) {
			CTTitle title = chart.getTitle();
			if (title.isSetOverlay()) {
				return title.getOverlay().getVal();
			}
		}
		return null;
	}

	public void setTitleOverlay(boolean overlay) {
		if (!(chart.isSetTitle())) {
			chart.addNewTitle();
		}
		new XDDFTitle(this, chart.getTitle()).setOverlay(overlay);
	}

	public void setTitleText(String text) {
		if (!(chart.isSetTitle())) {
			chart.addNewTitle();
		}
		new XDDFTitle(this, chart.getTitle()).setText(text);
	}

	public XDDFTitle getTitle() {
		if (chart.isSetTitle()) {
			return new XDDFTitle(this, chart.getTitle());
		}else {
			return null;
		}
	}

	@Beta
	public XDDFTextBody getFormattedTitle() {
		if (!(chart.isSetTitle())) {
			return null;
		}
		return new XDDFTitle(this, chart.getTitle()).getBody();
	}

	@Override
	public <R> Optional<R> findDefinedParagraphProperty(Function<CTTextParagraphProperties, Boolean> isSet, Function<CTTextParagraphProperties, R> getter) {
		return Optional.empty();
	}

	@Override
	public <R> Optional<R> findDefinedRunProperty(Function<CTTextCharacterProperties, Boolean> isSet, Function<CTTextCharacterProperties, R> getter) {
		return Optional.empty();
	}

	public XDDFShapeProperties getOrAddShapeProperties() {
		CTPlotArea plotArea = getCTPlotArea();
		CTShapeProperties properties;
		if (plotArea.isSetSpPr()) {
			properties = plotArea.getSpPr();
		}else {
			properties = plotArea.addNewSpPr();
		}
		return new XDDFShapeProperties(properties);
	}

	public void deleteShapeProperties() {
		if (getCTPlotArea().isSetSpPr()) {
			getCTPlotArea().unsetSpPr();
		}
	}

	public XDDFChartLegend getOrAddLegend() {
		return new XDDFChartLegend(chart);
	}

	public void deleteLegend() {
		if (chart.isSetLegend()) {
			chart.unsetLegend();
		}
	}

	public XDDFManualLayout getOrAddManualLayout() {
		return new XDDFManualLayout(chart.getPlotArea());
	}

	public void plot(XDDFChartData data) {
		XSSFSheet sheet = getSheet();
		for (XDDFChartData.Series series : data.getSeries()) {
			series.plot();
			fillSheet(sheet, series.getCategoryData(), series.getValuesData());
		}
	}

	public List<XDDFChartData> getChartSeries() {
		List<XDDFChartData> series = new LinkedList<>();
		CTPlotArea plotArea = getCTPlotArea();
		Map<Long, XDDFChartAxis> categories = getCategoryAxes();
		Map<Long, XDDFValueAxis> values = getValueAxes();
		for (int i = 0; i < (plotArea.sizeOfBarChartArray()); i++) {
			CTBarChart barChart = plotArea.getBarChartArray(i);
			series.add(new XDDFBarChartData(barChart, categories, values));
		}
		for (int i = 0; i < (plotArea.sizeOfLineChartArray()); i++) {
			CTLineChart lineChart = plotArea.getLineChartArray(i);
			series.add(new XDDFLineChartData(lineChart, categories, values));
		}
		for (int i = 0; i < (plotArea.sizeOfPieChartArray()); i++) {
			CTPieChart pieChart = plotArea.getPieChartArray(i);
			series.add(new XDDFPieChartData(pieChart));
		}
		for (int i = 0; i < (plotArea.sizeOfRadarChartArray()); i++) {
			CTRadarChart radarChart = plotArea.getRadarChartArray(i);
			series.add(new XDDFRadarChartData(radarChart, categories, values));
		}
		for (int i = 0; i < (plotArea.sizeOfScatterChartArray()); i++) {
			CTScatterChart scatterChart = plotArea.getScatterChartArray(i);
			series.add(new XDDFScatterChartData(scatterChart, categories, values));
		}
		return series;
	}

	private Map<Long, XDDFChartAxis> getCategoryAxes() {
		CTPlotArea plotArea = getCTPlotArea();
		int sizeOfArray = plotArea.sizeOfCatAxArray();
		Map<Long, XDDFChartAxis> axes = new HashMap<>(sizeOfArray);
		for (int i = 0; i < sizeOfArray; i++) {
			CTCatAx category = plotArea.getCatAxArray(i);
			axes.put(category.getAxId().getVal(), new XDDFCategoryAxis(category));
		}
		return axes;
	}

	private Map<Long, XDDFValueAxis> getValueAxes() {
		CTPlotArea plotArea = getCTPlotArea();
		int sizeOfArray = plotArea.sizeOfValAxArray();
		Map<Long, XDDFValueAxis> axes = new HashMap<>(sizeOfArray);
		for (int i = 0; i < sizeOfArray; i++) {
			CTValAx values = plotArea.getValAxArray(i);
			axes.put(values.getAxId().getVal(), new XDDFValueAxis(values));
		}
		return axes;
	}

	public XDDFValueAxis createValueAxis(AxisPosition pos) {
		XDDFValueAxis valueAxis = new XDDFValueAxis(chart.getPlotArea(), pos);
		if ((axes.size()) == 1) {
			XDDFChartAxis axis = axes.get(0);
			axis.crossAxis(valueAxis);
			valueAxis.crossAxis(axis);
		}
		axes.add(valueAxis);
		return valueAxis;
	}

	public XDDFCategoryAxis createCategoryAxis(AxisPosition pos) {
		XDDFCategoryAxis categoryAxis = new XDDFCategoryAxis(chart.getPlotArea(), pos);
		if ((axes.size()) == 1) {
			XDDFChartAxis axis = axes.get(0);
			axis.crossAxis(categoryAxis);
			categoryAxis.crossAxis(axis);
		}
		axes.add(categoryAxis);
		return categoryAxis;
	}

	public XDDFDateAxis createDateAxis(AxisPosition pos) {
		XDDFDateAxis dateAxis = new XDDFDateAxis(chart.getPlotArea(), pos);
		if ((axes.size()) == 1) {
			XDDFChartAxis axis = axes.get(0);
			axis.crossAxis(dateAxis);
			dateAxis.crossAxis(axis);
		}
		axes.add(dateAxis);
		return dateAxis;
	}

	public XDDFChartData createData(ChartTypes type, XDDFChartAxis category, XDDFValueAxis values) {
		Map<Long, XDDFChartAxis> categories = Collections.singletonMap(category.getId(), category);
		Map<Long, XDDFValueAxis> mapValues = Collections.singletonMap(values.getId(), values);
		final CTPlotArea plotArea = getCTPlotArea();
		switch (type) {
			case BAR :
				return new XDDFBarChartData(plotArea.addNewBarChart(), categories, mapValues);
			case LINE :
				return new XDDFLineChartData(plotArea.addNewLineChart(), categories, mapValues);
			case PIE :
				return new XDDFPieChartData(plotArea.addNewPieChart());
			case RADAR :
				return new XDDFRadarChartData(plotArea.addNewRadarChart(), categories, mapValues);
			case SCATTER :
				return new XDDFScatterChartData(plotArea.addNewScatterChart(), categories, mapValues);
			default :
				return null;
		}
	}

	public List<? extends XDDFChartAxis> getAxes() {
		if ((axes.isEmpty()) && (hasAxes())) {
			parseAxes();
		}
		return axes;
	}

	private boolean hasAxes() {
		CTPlotArea ctPlotArea = chart.getPlotArea();
		int totalAxisCount = (((ctPlotArea.sizeOfValAxArray()) + (ctPlotArea.sizeOfCatAxArray())) + (ctPlotArea.sizeOfDateAxArray())) + (ctPlotArea.sizeOfSerAxArray());
		return totalAxisCount > 0;
	}

	private void parseAxes() {
		for (CTCatAx catAx : chart.getPlotArea().getCatAxArray()) {
			axes.add(new XDDFCategoryAxis(catAx));
		}
		for (CTDateAx dateAx : chart.getPlotArea().getDateAxArray()) {
			axes.add(new XDDFDateAxis(dateAx));
		}
		for (CTSerAx serAx : chart.getPlotArea().getSerAxArray()) {
			axes.add(new XDDFSeriesAxis(serAx));
		}
		for (CTValAx valAx : chart.getPlotArea().getValAxArray()) {
			axes.add(new XDDFValueAxis(valAx));
		}
	}

	public void setValueRange(int axisIndex, Double minimum, Double maximum, Double majorUnit, Double minorUnit) {
		XDDFChartAxis axis = getAxes().get(axisIndex);
		if (axis == null) {
			return;
		}
		if (minimum != null) {
			axis.setMinimum(minimum);
		}
		if (maximum != null) {
			axis.setMaximum(maximum);
		}
		if (majorUnit != null) {
			axis.setMajorUnit(majorUnit);
		}
		if (minorUnit != null) {
			axis.setMinorUnit(minorUnit);
		}
	}

	public PackageRelationship createRelationshipInChart(POIXMLRelation chartRelation, POIXMLFactory chartFactory, int chartIndex) {
		documentPart = createRelationship(chartRelation, chartFactory, chartIndex, true).getDocumentPart();
		return this.addRelation(null, chartRelation, documentPart).getRelationship();
	}

	private PackagePart createWorksheetPart(POIXMLRelation chartRelation, POIXMLRelation chartWorkbookRelation, POIXMLFactory chartFactory) throws InvalidFormatException {
		PackageRelationship xlsx = createRelationshipInChart(chartWorkbookRelation, chartFactory, chartIndex);
		this.setExternalId(xlsx.getId());
		return getTargetPart(xlsx);
	}

	public void saveWorkbook(XSSFWorkbook workbook) throws IOException, InvalidFormatException {
		PackagePart worksheetPart = getWorksheetPart();
		if (worksheetPart == null) {
			POIXMLRelation chartRelation = getChartRelation();
			POIXMLRelation chartWorkbookRelation = getChartWorkbookRelation();
			POIXMLFactory chartFactory = getChartFactory();
			if (((chartRelation != null) && (chartWorkbookRelation != null)) && (chartFactory != null)) {
				worksheetPart = createWorksheetPart(chartRelation, chartWorkbookRelation, chartFactory);
			}else {
				throw new InvalidFormatException("unable to determine chart relations");
			}
		}
		try (OutputStream xlsOut = worksheetPart.getOutputStream()) {
			setWorksheetPartCommitted();
			workbook.write(xlsOut);
		}
	}

	protected abstract POIXMLRelation getChartRelation();

	protected abstract POIXMLRelation getChartWorkbookRelation();

	protected abstract POIXMLFactory getChartFactory();

	protected void fillSheet(XSSFSheet sheet, XDDFDataSource<?> categoryData, XDDFNumericalDataSource<?> valuesData) {
		int numOfPoints = categoryData.getPointCount();
		for (int i = 0; i < numOfPoints; i++) {
			XSSFRow row = this.getRow(sheet, (i + 1));
			this.getCell(row, categoryData.getColIndex()).setCellValue(categoryData.getPointAt(i).toString());
			this.getCell(row, valuesData.getColIndex()).setCellValue(valuesData.getPointAt(i).doubleValue());
		}
	}

	private XSSFRow getRow(XSSFSheet sheet, int index) {
		if ((sheet.getRow(index)) != null) {
			return sheet.getRow(index);
		}else {
			return sheet.createRow(index);
		}
	}

	private XSSFCell getCell(XSSFRow row, int index) {
		if ((row.getCell(index)) != null) {
			return row.getCell(index);
		}else {
			return row.createCell(index);
		}
	}

	public void importContent(XDDFChart other) {
		this.chart.set(other.chart);
	}

	@Override
	protected void commit() throws IOException {
		XmlOptions xmlOptions = new XmlOptions(POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
		xmlOptions.setSaveSyntheticDocumentElement(new QName(CTChartSpace.type.getName().getNamespaceURI(), "chartSpace", "c"));
		if ((workbook) != null) {
			try {
				saveWorkbook(workbook);
			} catch (InvalidFormatException e) {
				throw new POIXMLException(e);
			}
		}
		PackagePart part = getPackagePart();
		try (OutputStream out = part.getOutputStream()) {
			chartSpace.save(out, xmlOptions);
		}
	}

	public CellReference setSheetTitle(String title, int column) {
		XSSFSheet sheet = getSheet();
		XSSFRow row = this.getRow(sheet, 0);
		XSSFCell cell = this.getCell(row, column);
		cell.setCellValue(title);
		this.updateSheetTable(sheet.getTables().get(0).getCTTable(), title, column);
		return new CellReference(sheet.getSheetName(), 0, column, true, true);
	}

	private void updateSheetTable(CTTable ctTable, String title, int index) {
		CTTableColumns tableColumnList = ctTable.getTableColumns();
		CTTableColumn column = null;
		for (int i = 0; (tableColumnList.getCount()) < index; i++) {
			column = tableColumnList.addNewTableColumn();
			column.setId(i);
		}
		column = tableColumnList.getTableColumnArray(index);
		column.setName(title);
	}

	public String formatRange(CellRangeAddress range) {
		final XSSFSheet sheet = getSheet();
		return sheet == null ? null : range.formatAsString(sheet.getSheetName(), true);
	}

	private XSSFSheet getSheet() {
		XSSFSheet sheet = null;
		try {
			sheet = getWorkbook().getSheetAt(0);
		} catch (InvalidFormatException ife) {
		} catch (IOException ioe) {
		}
		return sheet;
	}

	private PackagePart getWorksheetPart() throws InvalidFormatException {
		for (POIXMLDocumentPart.RelationPart part : getRelationParts()) {
			if (POIXMLDocument.PACK_OBJECT_REL_TYPE.equals(part.getRelationship().getRelationshipType())) {
				return getTargetPart(part.getRelationship());
			}
		}
		return null;
	}

	private void setWorksheetPartCommitted() throws InvalidFormatException {
		for (POIXMLDocumentPart.RelationPart part : getRelationParts()) {
			if (POIXMLDocument.PACK_OBJECT_REL_TYPE.equals(part.getRelationship().getRelationshipType())) {
				part.getDocumentPart().setCommited(true);
				break;
			}
		}
	}

	public XSSFWorkbook getWorkbook() throws IOException, InvalidFormatException {
		if ((workbook) == null) {
			try {
				PackagePart worksheetPart = getWorksheetPart();
				if (worksheetPart == null) {
					workbook = new XSSFWorkbook();
					workbook.createSheet();
				}else {
					workbook = new XSSFWorkbook(worksheetPart.getInputStream());
				}
			} catch (NotOfficeXmlFileException e) {
				workbook = new XSSFWorkbook();
				workbook.createSheet();
			}
		}
		return workbook;
	}

	public void setWorkbook(XSSFWorkbook workbook) {
		this.workbook = workbook;
	}

	public void setExternalId(String id) {
		getCTChartSpace().addNewExternalData().setId(id);
	}

	protected int getChartIndex() {
		return chartIndex;
	}

	public void setChartIndex(int chartIndex) {
		this.chartIndex = chartIndex;
	}
}

