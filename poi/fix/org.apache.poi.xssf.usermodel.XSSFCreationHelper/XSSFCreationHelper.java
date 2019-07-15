

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.Internal;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.IndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFHyperlink;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColor;

import static org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColor.Factory.newInstance;


public class XSSFCreationHelper implements CreationHelper {
	private final XSSFWorkbook workbook;

	@Internal
	public XSSFCreationHelper(XSSFWorkbook wb) {
		workbook = wb;
	}

	@Override
	public XSSFRichTextString createRichTextString(String text) {
		XSSFRichTextString rt = new XSSFRichTextString(text);
		return rt;
	}

	@Override
	public XSSFDataFormat createDataFormat() {
		return workbook.createDataFormat();
	}

	@Override
	public XSSFColor createExtendedColor() {
		return XSSFColor.from(newInstance(), workbook.getStylesSource().getIndexedColors());
	}

	@Override
	public XSSFHyperlink createHyperlink(HyperlinkType type) {
		return null;
	}

	@Override
	public XSSFFormulaEvaluator createFormulaEvaluator() {
		return new XSSFFormulaEvaluator(workbook);
	}

	@Override
	public XSSFClientAnchor createClientAnchor() {
		return new XSSFClientAnchor();
	}

	@Override
	public AreaReference createAreaReference(String reference) {
		return new AreaReference(reference, workbook.getSpreadsheetVersion());
	}

	@Override
	public AreaReference createAreaReference(CellReference topLeft, CellReference bottomRight) {
		return new AreaReference(topLeft, bottomRight, workbook.getSpreadsheetVersion());
	}
}

