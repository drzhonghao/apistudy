

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.poi.hpsf.ClassID;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ooxml.POIXMLRelation;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.poifs.filesystem.Ole10Native;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.sl.usermodel.ObjectMetaData;
import org.apache.poi.sl.usermodel.ObjectShape;
import org.apache.poi.xslf.usermodel.XSLFFactory;
import org.apache.poi.xslf.usermodel.XSLFGraphicFrame;
import org.apache.poi.xslf.usermodel.XSLFObjectData;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFRelation;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSheet;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlTokenSource;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlip;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlipFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGraphicalObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGraphicalObjectData;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingProps;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualGraphicFrameProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualPictureProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPoint2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPositiveSize2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPresetGeometry2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTRelativeRect;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTStretchInfoProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTransform2D;
import org.openxmlformats.schemas.drawingml.x2006.main.STShapeType;
import org.openxmlformats.schemas.presentationml.x2006.main.CTApplicationNonVisualDrawingProps;
import org.openxmlformats.schemas.presentationml.x2006.main.CTGraphicalObjectFrame;
import org.openxmlformats.schemas.presentationml.x2006.main.CTGraphicalObjectFrameNonVisual;
import org.openxmlformats.schemas.presentationml.x2006.main.CTGroupShape;
import org.openxmlformats.schemas.presentationml.x2006.main.CTOleObject;
import org.openxmlformats.schemas.presentationml.x2006.main.CTPicture;
import org.openxmlformats.schemas.presentationml.x2006.main.CTPictureNonVisual;
import org.w3c.dom.Node;

import static org.openxmlformats.schemas.presentationml.x2006.main.CTGroupShape.Factory.newInstance;
import static org.openxmlformats.schemas.presentationml.x2006.main.CTPicture.Factory.parse;


public class XSLFObjectShape extends XSLFGraphicFrame implements ObjectShape<XSLFShape, XSLFTextParagraph> {
	static final String OLE_URI = "http://schemas.openxmlformats.org/presentationml/2006/ole";

	private CTOleObject _oleObject;

	private XSLFPictureData _data;

	@org.apache.poi.util.Internal
	public CTOleObject getCTOleObject() {
		return _oleObject;
	}

	@Override
	public XSLFObjectData getObjectData() {
		String oleRel = getCTOleObject().getId();
		return getSheet().getRelationPartById(oleRel).getDocumentPart();
	}

	@Override
	public String getProgId() {
		return (_oleObject) == null ? null : _oleObject.getProgId();
	}

	@Override
	public String getFullName() {
		return (_oleObject) == null ? null : _oleObject.getName();
	}

	@Override
	public XSLFPictureData getPictureData() {
		if ((_data) == null) {
			String blipId = getBlipId();
			if (blipId == null) {
				return null;
			}
			PackagePart p = getSheet().getPackagePart();
			PackageRelationship rel = p.getRelationship(blipId);
			if (rel != null) {
				try {
					PackagePart imgPart = p.getRelatedPart(rel);
					_data = new XSLFPictureData(imgPart);
				} catch (Exception e) {
					throw new POIXMLException(e);
				}
			}
		}
		return _data;
	}

	protected CTBlip getBlip() {
		return getBlipFill().getBlip();
	}

	protected String getBlipId() {
		String id = getBlip().getEmbed();
		if (id.isEmpty()) {
			return null;
		}
		return id;
	}

	protected CTBlipFillProperties getBlipFill() {
		String xquery = "declare namespace p='http://schemas.openxmlformats.org/presentationml/2006/main' " + ".//p:blipFill";
		XmlObject xo = selectProperty(XmlObject.class, xquery);
		try {
			xo = parse(xo.getDomNode());
		} catch (XmlException xe) {
			return null;
		}
		return ((CTPicture) (xo)).getBlipFill();
	}

	@Override
	public OutputStream updateObjectData(final ObjectMetaData.Application application, final ObjectMetaData metaData) throws IOException {
		final ObjectMetaData md = (application != null) ? application.getMetaData() : metaData;
		if ((md == null) || ((md.getClassID()) == null)) {
			throw new IllegalArgumentException("either application and/or metaData needs to be set.");
		}
		final XSLFSheet sheet = getSheet();
		final POIXMLDocumentPart.RelationPart rp;
		if (_oleObject.isSetId()) {
			rp = sheet.getRelationPartById(_oleObject.getId());
		}else {
			try {
				final XSLFRelation descriptor = XSLFRelation.OLE_OBJECT;
				final OPCPackage pack = sheet.getPackagePart().getPackage();
				int nextIdx = pack.getUnusedPartIndex(descriptor.getDefaultFileName());
				rp = sheet.createRelationship(descriptor, XSLFFactory.getInstance(), nextIdx, false);
				_oleObject.setId(rp.getRelationship().getId());
			} catch (InvalidFormatException e) {
				throw new IOException("Unable to add new ole embedding", e);
			}
		}
		_oleObject.setProgId(md.getProgId());
		_oleObject.setName(md.getObjectName());
		return new XSLFObjectShape.XSLFObjectOutputStream(rp.getDocumentPart().getPackagePart(), md);
	}

	private static class XSLFObjectOutputStream extends ByteArrayOutputStream {
		final PackagePart objectPart;

		final ObjectMetaData metaData;

		private XSLFObjectOutputStream(final PackagePart objectPart, final ObjectMetaData metaData) {
			super(100000);
			this.objectPart = objectPart;
			this.metaData = metaData;
		}

		public void close() throws IOException {
			objectPart.clear();
			try (final OutputStream os = objectPart.getOutputStream()) {
				final ByteArrayInputStream bis = new ByteArrayInputStream(this.buf, 0, size());
				final FileMagic fm = FileMagic.valueOf(this.buf);
				if (fm == (FileMagic.OLE2)) {
					try (final POIFSFileSystem poifs = new POIFSFileSystem(bis)) {
						poifs.getRoot().setStorageClsid(metaData.getClassID());
						poifs.writeFilesystem(os);
					}
				}else
					if ((metaData.getOleEntry()) == null) {
						os.write(this.buf, 0, size());
					}else {
						try (final POIFSFileSystem poifs = new POIFSFileSystem()) {
							final ClassID clsId = metaData.getClassID();
							if (clsId != null) {
								poifs.getRoot().setStorageClsid(clsId);
							}
							poifs.createDocument(bis, metaData.getOleEntry());
							Ole10Native.createOleMarkerEntry(poifs);
							poifs.writeFilesystem(os);
						}
					}

			}
		}
	}

	static CTGraphicalObjectFrame prototype(int shapeId, String picRel) {
		CTGraphicalObjectFrame frame = CTGraphicalObjectFrame.Factory.newInstance();
		CTGraphicalObjectFrameNonVisual nvGr = frame.addNewNvGraphicFramePr();
		CTNonVisualDrawingProps cnv = nvGr.addNewCNvPr();
		cnv.setName(("Object " + shapeId));
		cnv.setId(shapeId);
		nvGr.addNewCNvGraphicFramePr();
		nvGr.addNewNvPr();
		frame.addNewXfrm();
		CTGraphicalObjectData gr = frame.addNewGraphic().addNewGraphicData();
		gr.setUri(XSLFObjectShape.OLE_URI);
		XmlCursor grCur = gr.newCursor();
		grCur.toEndToken();
		CTGroupShape grpShp = newInstance();
		CTPicture pic = grpShp.addNewPic();
		CTPictureNonVisual nvPicPr = pic.addNewNvPicPr();
		CTNonVisualDrawingProps cNvPr = nvPicPr.addNewCNvPr();
		cNvPr.setName("");
		cNvPr.setId(0);
		nvPicPr.addNewCNvPicPr();
		nvPicPr.addNewNvPr();
		CTBlipFillProperties blip = pic.addNewBlipFill();
		blip.addNewBlip().setEmbed(picRel);
		blip.addNewStretch().addNewFillRect();
		CTShapeProperties spPr = pic.addNewSpPr();
		CTTransform2D xfrm = spPr.addNewXfrm();
		CTPoint2D off = xfrm.addNewOff();
		off.setX(1270000);
		off.setY(1270000);
		CTPositiveSize2D xext = xfrm.addNewExt();
		xext.setCx(1270000);
		xext.setCy(1270000);
		spPr.addNewPrstGeom().setPrst(STShapeType.RECT);
		XmlCursor picCur = grpShp.newCursor();
		picCur.toStartDoc();
		picCur.moveXmlContents(grCur);
		picCur.dispose();
		grCur.dispose();
		return frame;
	}
}

