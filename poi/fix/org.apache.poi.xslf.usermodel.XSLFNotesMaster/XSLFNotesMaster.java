

import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.sl.usermodel.MasterSheet;
import org.apache.poi.util.Beta;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSheet;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.openxmlformats.schemas.drawingml.x2006.main.CTColorMapping;
import org.openxmlformats.schemas.presentationml.x2006.main.CTNotesMaster;
import org.openxmlformats.schemas.presentationml.x2006.main.NotesMasterDocument;

import static org.openxmlformats.schemas.presentationml.x2006.main.NotesMasterDocument.Factory.parse;


@Beta
public class XSLFNotesMaster extends XSLFSheet implements MasterSheet<XSLFShape, XSLFTextParagraph> {
	private CTNotesMaster _slide;

	XSLFNotesMaster() {
		super();
		_slide = XSLFNotesMaster.prototype();
	}

	protected XSLFNotesMaster(PackagePart part) throws IOException, XmlException {
		super(part);
		NotesMasterDocument doc = parse(getPackagePart().getInputStream(), POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
		_slide = doc.getNotesMaster();
	}

	private static CTNotesMaster prototype() {
		InputStream is = XSLFNotesMaster.class.getResourceAsStream("notesMaster.xml");
		if (is == null) {
			throw new POIXMLException("Missing resource 'notesMaster.xml'");
		}
		try {
			try {
				NotesMasterDocument doc = parse(is, POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
				return doc.getNotesMaster();
			} finally {
				is.close();
			}
		} catch (Exception e) {
			throw new POIXMLException("Can't initialize NotesMaster", e);
		}
	}

	@Override
	public CTNotesMaster getXmlObject() {
		return _slide;
	}

	@Override
	protected String getRootElementName() {
		return "notesMaster";
	}

	@Override
	public MasterSheet<XSLFShape, XSLFTextParagraph> getMasterSheet() {
		return null;
	}

	boolean isSupportTheme() {
		return true;
	}

	CTColorMapping getColorMapping() {
		return _slide.getClrMap();
	}
}

