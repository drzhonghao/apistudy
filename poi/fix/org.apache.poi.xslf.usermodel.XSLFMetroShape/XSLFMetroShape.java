

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.sl.usermodel.Shape;
import org.apache.poi.util.Internal;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.openxmlformats.schemas.presentationml.x2006.main.CTGroupShape;

import static org.openxmlformats.schemas.presentationml.x2006.main.CTGroupShape.Factory.parse;


@Internal
public class XSLFMetroShape {
	public static Shape<?, ?> parseShape(byte[] metroBytes) throws IOException, InvalidFormatException, XmlException {
		PackagePartName shapePN = PackagingURIHelper.createPartName("/drs/shapexml.xml");
		OPCPackage pkg = null;
		try {
			pkg = OPCPackage.open(new ByteArrayInputStream(metroBytes));
			PackagePart shapePart = pkg.getPart(shapePN);
			CTGroupShape gs = parse(shapePart.getInputStream(), POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
		} finally {
			if (pkg != null) {
				pkg.close();
			}
		}
		return null;
	}
}

