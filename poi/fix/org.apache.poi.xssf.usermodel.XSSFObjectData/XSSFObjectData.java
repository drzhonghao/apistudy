

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.namespace.QName;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.PackageRelationshipTypes;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.ObjectData;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPictureData;
import org.apache.poi.xssf.usermodel.XSSFRelation;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFSimpleShape;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlTokenSource;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGeomGuideList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingProps;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingShapeProps;
import org.openxmlformats.schemas.drawingml.x2006.main.CTOfficeArtExtension;
import org.openxmlformats.schemas.drawingml.x2006.main.CTOfficeArtExtensionList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPoint2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPositiveSize2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPresetGeometry2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTransform2D;
import org.openxmlformats.schemas.drawingml.x2006.main.STShapeType;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTShape;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTShapeNonVisual;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTOleObject;

import static org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTShape.Factory.newInstance;


public class XSSFObjectData extends XSSFSimpleShape implements ObjectData {
	private static final POILogger LOG = POILogFactory.getLogger(XSSFObjectData.class);

	private static CTShape prototype;

	private CTOleObject oleObject;

	protected XSSFObjectData(XSSFDrawing drawing, CTShape ctShape) {
		super(drawing, ctShape);
	}

	protected static CTShape prototype() {
		final String drawNS = "http://schemas.microsoft.com/office/drawing/2010/main";
		if ((XSSFObjectData.prototype) == null) {
			CTShape shape = newInstance();
			CTShapeNonVisual nv = shape.addNewNvSpPr();
			CTNonVisualDrawingProps nvp = nv.addNewCNvPr();
			nvp.setId(1);
			nvp.setName("Shape 1");
			CTOfficeArtExtensionList extLst = nvp.addNewExtLst();
			CTOfficeArtExtension ext = extLst.addNewExt();
			ext.setUri("{63B3BB69-23CF-44E3-9099-C40C66FF867C}");
			XmlCursor cur = ext.newCursor();
			cur.toEndToken();
			cur.beginElement(new QName(drawNS, "compatExt", "a14"));
			cur.insertNamespace("a14", drawNS);
			cur.insertAttributeWithValue("spid", "_x0000_s1");
			cur.dispose();
			nv.addNewCNvSpPr();
			CTShapeProperties sp = shape.addNewSpPr();
			CTTransform2D t2d = sp.addNewXfrm();
			CTPositiveSize2D p1 = t2d.addNewExt();
			p1.setCx(0);
			p1.setCy(0);
			CTPoint2D p2 = t2d.addNewOff();
			p2.setX(0);
			p2.setY(0);
			CTPresetGeometry2D geom = sp.addNewPrstGeom();
			geom.setPrst(STShapeType.RECT);
			geom.addNewAvLst();
			XSSFObjectData.prototype = shape;
		}
		return XSSFObjectData.prototype;
	}

	@Override
	public String getOLE2ClassName() {
		return getOleObject().getProgId();
	}

	public CTOleObject getOleObject() {
		if ((oleObject) == null) {
			long shapeId = getCTShape().getNvSpPr().getCNvPr().getId();
			if ((oleObject) == null) {
				throw new POIXMLException("Ole object not found in sheet container - it's probably a control element");
			}
		}
		return oleObject;
	}

	@Override
	public byte[] getObjectData() throws IOException {
		InputStream is = getObjectPart().getInputStream();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		IOUtils.copy(is, bos);
		is.close();
		return bos.toByteArray();
	}

	public PackagePart getObjectPart() {
		if (!(getOleObject().isSetId())) {
			throw new POIXMLException("Invalid ole object found in sheet container");
		}
		POIXMLDocumentPart pdp = getSheet().getRelationById(getOleObject().getId());
		return pdp == null ? null : pdp.getPackagePart();
	}

	@Override
	public boolean hasDirectoryEntry() {
		InputStream is = null;
		try {
			is = getObjectPart().getInputStream();
			is = FileMagic.prepareToCheckMagic(is);
			return (FileMagic.valueOf(is)) == (FileMagic.OLE2);
		} catch (IOException e) {
			XSSFObjectData.LOG.log(POILogger.WARN, "can't determine if directory entry exists", e);
			return false;
		} finally {
			IOUtils.closeQuietly(is);
		}
	}

	@Override
	public DirectoryEntry getDirectory() throws IOException {
		try (InputStream is = getObjectPart().getInputStream()) {
			return new POIFSFileSystem(is).getRoot();
		}
	}

	@Override
	public String getFileName() {
		return getObjectPart().getPartName().getName();
	}

	protected XSSFSheet getSheet() {
		return ((XSSFSheet) (getDrawing().getParent()));
	}

	@Override
	public XSSFPictureData getPictureData() {
		XmlCursor cur = getOleObject().newCursor();
		try {
			if (cur.toChild(XSSFRelation.NS_SPREADSHEETML, "objectPr")) {
				String blipId = cur.getAttributeText(new QName(PackageRelationshipTypes.CORE_PROPERTIES_ECMA376_NS, "id"));
				return ((XSSFPictureData) (getSheet().getRelationById(blipId)));
			}
			return null;
		} finally {
			cur.dispose();
		}
	}

	@Override
	public String getContentType() {
		return getObjectPart().getContentType();
	}
}

