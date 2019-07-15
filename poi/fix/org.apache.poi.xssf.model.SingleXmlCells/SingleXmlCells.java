

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Vector;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.helpers.XSSFSingleXmlCell;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSingleXmlCell;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSingleXmlCells;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.SingleXmlCellsDocument;

import static org.openxmlformats.schemas.spreadsheetml.x2006.main.SingleXmlCellsDocument.Factory.newInstance;
import static org.openxmlformats.schemas.spreadsheetml.x2006.main.SingleXmlCellsDocument.Factory.parse;


public class SingleXmlCells extends POIXMLDocumentPart {
	private CTSingleXmlCells singleXMLCells;

	public SingleXmlCells() {
		super();
		singleXMLCells = CTSingleXmlCells.Factory.newInstance();
	}

	public SingleXmlCells(PackagePart part) throws IOException {
		super(part);
		readFrom(part.getInputStream());
	}

	public void readFrom(InputStream is) throws IOException {
		try {
			SingleXmlCellsDocument doc = parse(is, POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
			singleXMLCells = doc.getSingleXmlCells();
		} catch (XmlException e) {
			throw new IOException(e.getLocalizedMessage());
		}
	}

	public XSSFSheet getXSSFSheet() {
		return ((XSSFSheet) (getParent()));
	}

	protected void writeTo(OutputStream out) throws IOException {
		SingleXmlCellsDocument doc = newInstance();
		doc.setSingleXmlCells(singleXMLCells);
		doc.save(out, POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
	}

	@Override
	protected void commit() throws IOException {
		PackagePart part = getPackagePart();
		OutputStream out = part.getOutputStream();
		writeTo(out);
		out.close();
	}

	public CTSingleXmlCells getCTSingleXMLCells() {
		return singleXMLCells;
	}

	public List<XSSFSingleXmlCell> getAllSimpleXmlCell() {
		List<XSSFSingleXmlCell> list = new Vector<>();
		for (CTSingleXmlCell singleXmlCell : singleXMLCells.getSingleXmlCellArray()) {
		}
		return list;
	}
}

