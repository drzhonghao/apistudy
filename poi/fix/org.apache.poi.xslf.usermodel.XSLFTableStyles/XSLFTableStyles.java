

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.util.Beta;
import org.apache.poi.xslf.usermodel.XSLFTableStyle;
import org.apache.xmlbeans.XmlException;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTableStyle;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTableStyleList;
import org.openxmlformats.schemas.drawingml.x2006.main.TblStyleLstDocument;

import static org.openxmlformats.schemas.drawingml.x2006.main.TblStyleLstDocument.Factory.parse;


@Beta
public class XSLFTableStyles extends POIXMLDocumentPart implements Iterable<XSLFTableStyle> {
	private CTTableStyleList _tblStyleLst;

	private List<XSLFTableStyle> _styles;

	public XSLFTableStyles() {
		super();
	}

	public XSLFTableStyles(PackagePart part) throws IOException, XmlException {
		super(part);
		InputStream is = getPackagePart().getInputStream();
		TblStyleLstDocument styleDoc = parse(is);
		is.close();
		_tblStyleLst = styleDoc.getTblStyleLst();
		List<CTTableStyle> tblStyles = _tblStyleLst.getTblStyleList();
		_styles = new ArrayList<>(tblStyles.size());
		for (CTTableStyle c : tblStyles) {
		}
	}

	public CTTableStyleList getXmlObject() {
		return _tblStyleLst;
	}

	public Iterator<XSLFTableStyle> iterator() {
		return _styles.iterator();
	}

	public List<XSLFTableStyle> getStyles() {
		return Collections.unmodifiableList(_styles);
	}
}

