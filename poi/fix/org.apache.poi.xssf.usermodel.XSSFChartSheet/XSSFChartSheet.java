

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.xml.namespace.QName;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTChartsheet;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDrawing;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTLegacyDrawing;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.ChartsheetDocument;

import static org.openxmlformats.schemas.spreadsheetml.x2006.main.ChartsheetDocument.Factory.parse;


public class XSSFChartSheet extends XSSFSheet {
	private static final byte[] BLANK_WORKSHEET = XSSFChartSheet.blankWorksheet();

	protected CTChartsheet chartsheet;

	protected XSSFChartSheet(PackagePart part) {
		super(part);
	}

	protected void read(InputStream is) throws IOException {
		super.read(new ByteArrayInputStream(XSSFChartSheet.BLANK_WORKSHEET));
		try {
			chartsheet = parse(is, POIXMLTypeLoader.DEFAULT_XML_OPTIONS).getChartsheet();
		} catch (XmlException e) {
			throw new POIXMLException(e);
		}
	}

	public CTChartsheet getCTChartsheet() {
		return chartsheet;
	}

	@Override
	protected CTDrawing getCTDrawing() {
		return chartsheet.getDrawing();
	}

	@Override
	protected CTLegacyDrawing getCTLegacyDrawing() {
		return chartsheet.getLegacyDrawing();
	}

	@Override
	protected void write(OutputStream out) throws IOException {
		XmlOptions xmlOptions = new XmlOptions(POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
		xmlOptions.setSaveSyntheticDocumentElement(new QName(CTChartsheet.type.getName().getNamespaceURI(), "chartsheet"));
		chartsheet.save(out, xmlOptions);
	}

	private static byte[] blankWorksheet() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		return out.toByteArray();
	}
}

