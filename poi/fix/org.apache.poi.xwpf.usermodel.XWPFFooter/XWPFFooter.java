

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.xml.namespace.QName;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xwpf.usermodel.BodyType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeaderFooter;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHdrFtr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTNumbering;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.FtrDocument;

import static org.openxmlformats.schemas.wordprocessingml.x2006.main.FtrDocument.Factory.parse;


public class XWPFFooter extends XWPFHeaderFooter {
	public XWPFFooter() {
		super();
	}

	public XWPFFooter(XWPFDocument doc, CTHdrFtr hdrFtr) throws IOException {
	}

	public XWPFFooter(POIXMLDocumentPart parent, PackagePart part) throws IOException {
		super(parent, part);
	}

	@Override
	protected void commit() throws IOException {
		XmlOptions xmlOptions = new XmlOptions(POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
		xmlOptions.setSaveSyntheticDocumentElement(new QName(CTNumbering.type.getName().getNamespaceURI(), "ftr"));
		PackagePart part = getPackagePart();
		OutputStream out = part.getOutputStream();
		super._getHdrFtr().save(out, xmlOptions);
		out.close();
	}

	@Override
	protected void onDocumentRead() throws IOException {
		super.onDocumentRead();
		FtrDocument ftrDocument = null;
		InputStream is = null;
		try {
			is = getPackagePart().getInputStream();
			ftrDocument = parse(is, POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
		} catch (Exception e) {
			throw new POIXMLException(e);
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	public BodyType getPartType() {
		return BodyType.FOOTER;
	}
}

