

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Locale;
import org.apache.poi.extractor.POITextExtractor;
import org.apache.poi.ooxml.extractor.POIXMLTextExtractor;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.ss.extractor.ExcelExtractor;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.xssf.binary.XSSFBCommentsTable;
import org.apache.poi.xssf.binary.XSSFBHyperlinksTable;
import org.apache.poi.xssf.binary.XSSFBParser;
import org.apache.poi.xssf.binary.XSSFBSharedStringsTable;
import org.apache.poi.xssf.binary.XSSFBSheetHandler;
import org.apache.poi.xssf.binary.XSSFBStylesTable;
import org.apache.poi.xssf.eventusermodel.XSSFBReader;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.extractor.XSSFEventBasedExcelExtractor;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.usermodel.XSSFRelation;
import org.apache.xmlbeans.XmlException;
import org.xml.sax.SAXException;


public class XSSFBEventBasedExcelExtractor extends XSSFEventBasedExcelExtractor implements ExcelExtractor {
	private static final POILogger LOGGER = POILogFactory.getLogger(XSSFBEventBasedExcelExtractor.class);

	public static final XSSFRelation[] SUPPORTED_TYPES = new XSSFRelation[]{ XSSFRelation.XLSB_BINARY_WORKBOOK };

	private boolean handleHyperlinksInCells;

	public XSSFBEventBasedExcelExtractor(String path) throws IOException, OpenXML4JException, XmlException {
		super(path);
	}

	public XSSFBEventBasedExcelExtractor(OPCPackage container) throws IOException, OpenXML4JException, XmlException {
		super(container);
	}

	public static void main(String[] args) throws Exception {
		if ((args.length) < 1) {
			System.err.println("Use:");
			System.err.println("  XSSFBEventBasedExcelExtractor <filename.xlsb>");
			System.exit(1);
		}
		POIXMLTextExtractor extractor = new XSSFBEventBasedExcelExtractor(args[0]);
		System.out.println(extractor.getText());
		extractor.close();
	}

	public void setHandleHyperlinksInCells(boolean handleHyperlinksInCells) {
		this.handleHyperlinksInCells = handleHyperlinksInCells;
	}

	@Override
	public void setFormulasNotResults(boolean formulasNotResults) {
		throw new IllegalArgumentException("Not currently supported");
	}

	public void processSheet(XSSFSheetXMLHandler.SheetContentsHandler sheetContentsExtractor, XSSFBStylesTable styles, XSSFBCommentsTable comments, SharedStrings strings, InputStream sheetInputStream) throws IOException {
		DataFormatter formatter;
		if ((getLocale()) == null) {
			formatter = new DataFormatter();
		}else {
			formatter = new DataFormatter(getLocale());
		}
		XSSFBSheetHandler xssfbSheetHandler = new XSSFBSheetHandler(sheetInputStream, styles, comments, strings, sheetContentsExtractor, formatter, getFormulasNotResults());
		xssfbSheetHandler.parse();
	}

	public String getText() {
		try {
			XSSFBSharedStringsTable strings = new XSSFBSharedStringsTable(getPackage());
			XSSFBReader xssfbReader = new XSSFBReader(getPackage());
			XSSFBStylesTable styles = xssfbReader.getXSSFBStylesTable();
			XSSFBReader.SheetIterator iter = ((XSSFBReader.SheetIterator) (xssfbReader.getSheetsData()));
			StringBuilder text = new StringBuilder(64);
			XSSFBHyperlinksTable hyperlinksTable = null;
			while (iter.hasNext()) {
				InputStream stream = iter.next();
				if (getIncludeSheetNames()) {
					text.append(iter.getSheetName());
					text.append('\n');
				}
				if (handleHyperlinksInCells) {
					hyperlinksTable = new XSSFBHyperlinksTable(iter.getSheetPart());
				}
				XSSFBCommentsTable comments = (getIncludeCellComments()) ? iter.getXSSFBSheetComments() : null;
				if (getIncludeHeadersFooters()) {
				}
				if (getIncludeTextBoxes()) {
				}
				if (getIncludeHeadersFooters()) {
				}
				stream.close();
			} 
			return text.toString();
		} catch (IOException | OpenXML4JException | SAXException e) {
			XSSFBEventBasedExcelExtractor.LOGGER.log(POILogger.WARN, e);
			return null;
		}
	}
}

