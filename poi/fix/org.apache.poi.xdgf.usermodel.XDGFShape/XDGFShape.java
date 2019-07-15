

import com.microsoft.schemas.office.visio.x2012.main.ShapeSheetType;
import com.microsoft.schemas.office.visio.x2012.main.ShapesType;
import com.microsoft.schemas.office.visio.x2012.main.SheetType;
import com.microsoft.schemas.office.visio.x2012.main.TextType;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.xdgf.exceptions.XDGFException;
import org.apache.poi.xdgf.usermodel.XDGFBaseContents;
import org.apache.poi.xdgf.usermodel.XDGFCell;
import org.apache.poi.xdgf.usermodel.XDGFDocument;
import org.apache.poi.xdgf.usermodel.XDGFMaster;
import org.apache.poi.xdgf.usermodel.XDGFMasterContents;
import org.apache.poi.xdgf.usermodel.XDGFPageContents;
import org.apache.poi.xdgf.usermodel.XDGFSheet;
import org.apache.poi.xdgf.usermodel.XDGFStyleSheet;
import org.apache.poi.xdgf.usermodel.XDGFText;
import org.apache.poi.xdgf.usermodel.section.CombinedIterable;
import org.apache.poi.xdgf.usermodel.section.GeometrySection;
import org.apache.poi.xdgf.usermodel.section.XDGFSection;
import org.apache.poi.xdgf.usermodel.shape.ShapeVisitor;
import org.apache.poi.xdgf.usermodel.shape.exceptions.StopVisitingThisBranch;


public class XDGFShape extends XDGFSheet {
	XDGFBaseContents _parentPage;

	XDGFShape _parent;

	XDGFMaster _master;

	XDGFShape _masterShape;

	XDGFText _text;

	List<XDGFShape> _shapes;

	Double _pinX;

	Double _pinY;

	Double _width;

	Double _height;

	Double _locPinX;

	Double _locPinY;

	Double _beginX;

	Double _beginY;

	Double _endX;

	Double _endY;

	Double _angle;

	Double _rotationXAngle;

	Double _rotationYAngle;

	Double _rotationZAngle;

	Boolean _flipX;

	Boolean _flipY;

	Double _txtPinX;

	Double _txtPinY;

	Double _txtLocPinX;

	Double _txtLocPinY;

	Double _txtAngle;

	Double _txtWidth;

	Double _txtHeight;

	public XDGFShape(ShapeSheetType shapeSheet, XDGFBaseContents parentPage, XDGFDocument document) {
		this(null, shapeSheet, parentPage, document);
	}

	public XDGFShape(XDGFShape parent, ShapeSheetType shapeSheet, XDGFBaseContents parentPage, XDGFDocument document) {
		super(shapeSheet, document);
		_parent = parent;
		_parentPage = parentPage;
		TextType text = shapeSheet.getText();
		if (text != null) {
		}
		if (shapeSheet.isSetShapes()) {
			_shapes = new ArrayList<>();
			for (ShapeSheetType shape : shapeSheet.getShapes().getShapeArray()) {
				_shapes.add(new XDGFShape(this, shape, parentPage, document));
			}
		}
		readProperties();
	}

	@Override
	public String toString() {
		if ((_parentPage) instanceof XDGFMasterContents) {
			return (((_parentPage) + ": <Shape ID=\"") + (getID())) + "\">";
		}else {
			return ("<Shape ID=\"" + (getID())) + "\">";
		}
	}

	protected void readProperties() {
		_pinX = XDGFCell.maybeGetDouble(_cells, "PinX");
		_pinY = XDGFCell.maybeGetDouble(_cells, "PinY");
		_width = XDGFCell.maybeGetDouble(_cells, "Width");
		_height = XDGFCell.maybeGetDouble(_cells, "Height");
		_locPinX = XDGFCell.maybeGetDouble(_cells, "LocPinX");
		_locPinY = XDGFCell.maybeGetDouble(_cells, "LocPinY");
		_beginX = XDGFCell.maybeGetDouble(_cells, "BeginX");
		_beginY = XDGFCell.maybeGetDouble(_cells, "BeginY");
		_endX = XDGFCell.maybeGetDouble(_cells, "EndX");
		_endY = XDGFCell.maybeGetDouble(_cells, "EndY");
		_angle = XDGFCell.maybeGetDouble(_cells, "Angle");
		_rotationXAngle = XDGFCell.maybeGetDouble(_cells, "RotationXAngle");
		_rotationYAngle = XDGFCell.maybeGetDouble(_cells, "RotationYAngle");
		_rotationZAngle = XDGFCell.maybeGetDouble(_cells, "RotationZAngle");
		_flipX = XDGFCell.maybeGetBoolean(_cells, "FlipX");
		_flipY = XDGFCell.maybeGetBoolean(_cells, "FlipY");
		_txtPinX = XDGFCell.maybeGetDouble(_cells, "TxtPinX");
		_txtPinY = XDGFCell.maybeGetDouble(_cells, "TxtPinY");
		_txtLocPinX = XDGFCell.maybeGetDouble(_cells, "TxtLocPinX");
		_txtLocPinY = XDGFCell.maybeGetDouble(_cells, "TxtLocPinY");
		_txtWidth = XDGFCell.maybeGetDouble(_cells, "TxtWidth");
		_txtHeight = XDGFCell.maybeGetDouble(_cells, "TxtHeight");
		_txtAngle = XDGFCell.maybeGetDouble(_cells, "TxtAngle");
	}

	protected void setupMaster(XDGFPageContents pageContents, XDGFMasterContents master) {
		ShapeSheetType obj = getXmlObject();
		if (obj.isSetMaster()) {
			_master = pageContents.getMasterById(obj.getMaster());
			if ((_master) == null) {
				throw XDGFException.error(("refers to non-existant master " + (obj.getMaster())), this);
			}
		}else
			if (obj.isSetMasterShape()) {
				if ((_masterShape) == null) {
					throw XDGFException.error(("refers to non-existant master shape " + (obj.getMasterShape())), this);
				}
			}

		setupSectionMasters();
		if ((_shapes) != null) {
			for (XDGFShape shape : _shapes) {
				shape.setupMaster(pageContents, ((_master) == null ? master : _master.getContent()));
			}
		}
	}

	protected void setupSectionMasters() {
		if ((_masterShape) == null) {
			return;
		}
		try {
			for (Map.Entry<String, XDGFSection> section : _sections.entrySet()) {
				XDGFSection master = _masterShape.getSection(section.getKey());
				if (master != null) {
					section.getValue().setupMaster(master);
				}
			}
			for (Map.Entry<Long, GeometrySection> section : _geometry.entrySet()) {
				GeometrySection master = _masterShape.getGeometryByIdx(section.getKey());
				if (master != null) {
					section.getValue().setupMaster(master);
				}
			}
		} catch (POIXMLException e) {
			throw XDGFException.wrap(this.toString(), e);
		}
	}

	public ShapeSheetType getXmlObject() {
		return ((ShapeSheetType) (_sheet));
	}

	public long getID() {
		return getXmlObject().getID();
	}

	public String getType() {
		return getXmlObject().getType();
	}

	public String getTextAsString() {
		XDGFText text = getText();
		if (text == null) {
			return "";
		}
		return text.getTextContent();
	}

	public boolean hasText() {
		return ((_text) != null) || (((_masterShape) != null) && ((_masterShape._text) != null));
	}

	@Override
	public XDGFCell getCell(String cellName) {
		XDGFCell _cell = super.getCell(cellName);
		if ((_cell == null) && ((_masterShape) != null)) {
			_cell = _masterShape.getCell(cellName);
		}
		return _cell;
	}

	public GeometrySection getGeometryByIdx(long idx) {
		return _geometry.get(idx);
	}

	public List<XDGFShape> getShapes() {
		return _shapes;
	}

	public String getName() {
		String name = getXmlObject().getName();
		if (name == null) {
			return "";
		}
		return name;
	}

	public String getShapeType() {
		String type = getXmlObject().getType();
		if (type == null) {
			return "";
		}
		return type;
	}

	public String getSymbolName() {
		if ((_master) == null) {
			return "";
		}
		String name = _master.getName();
		if (name == null) {
			return "";
		}
		return name;
	}

	public XDGFShape getMasterShape() {
		return _masterShape;
	}

	public XDGFShape getParentShape() {
		return _parent;
	}

	public XDGFShape getTopmostParentShape() {
		XDGFShape top = null;
		if ((_parent) != null) {
			top = _parent.getTopmostParentShape();
			if (top == null) {
				top = _parent;
			}
		}
		return top;
	}

	public boolean hasMaster() {
		return (_master) != null;
	}

	public boolean hasMasterShape() {
		return (_masterShape) != null;
	}

	public boolean hasParent() {
		return (_parent) != null;
	}

	public boolean hasShapes() {
		return (_shapes) != null;
	}

	public boolean isTopmost() {
		return (_parent) == null;
	}

	public boolean isShape1D() {
		return (getBeginX()) != null;
	}

	public boolean isDeleted() {
		return getXmlObject().isSetDel() ? getXmlObject().getDel() : false;
	}

	public XDGFText getText() {
		if (((_text) == null) && ((_masterShape) != null)) {
			return _masterShape.getText();
		}
		return _text;
	}

	public Double getPinX() {
		if (((_pinX) == null) && ((_masterShape) != null)) {
			return _masterShape.getPinX();
		}
		if ((_pinX) == null) {
			throw XDGFException.error("PinX not set!", this);
		}
		return _pinX;
	}

	public Double getPinY() {
		if (((_pinY) == null) && ((_masterShape) != null)) {
			return _masterShape.getPinY();
		}
		if ((_pinY) == null) {
			throw XDGFException.error("PinY not specified!", this);
		}
		return _pinY;
	}

	public Double getWidth() {
		if (((_width) == null) && ((_masterShape) != null)) {
			return _masterShape.getWidth();
		}
		if ((_width) == null) {
			throw XDGFException.error("Width not specified!", this);
		}
		return _width;
	}

	public Double getHeight() {
		if (((_height) == null) && ((_masterShape) != null)) {
			return _masterShape.getHeight();
		}
		if ((_height) == null) {
			throw XDGFException.error("Height not specified!", this);
		}
		return _height;
	}

	public Double getLocPinX() {
		if (((_locPinX) == null) && ((_masterShape) != null)) {
			return _masterShape.getLocPinX();
		}
		if ((_locPinX) == null) {
			throw XDGFException.error("LocPinX not specified!", this);
		}
		return _locPinX;
	}

	public Double getLocPinY() {
		if (((_locPinY) == null) && ((_masterShape) != null)) {
			return _masterShape.getLocPinY();
		}
		if ((_locPinY) == null) {
			throw XDGFException.error("LocPinY not specified!", this);
		}
		return _locPinY;
	}

	public Double getBeginX() {
		if (((_beginX) == null) && ((_masterShape) != null)) {
			return _masterShape.getBeginX();
		}
		return _beginX;
	}

	public Double getBeginY() {
		if (((_beginY) == null) && ((_masterShape) != null)) {
			return _masterShape.getBeginY();
		}
		return _beginY;
	}

	public Double getEndX() {
		if (((_endX) == null) && ((_masterShape) != null)) {
			return _masterShape.getEndX();
		}
		return _endX;
	}

	public Double getEndY() {
		if (((_endY) == null) && ((_masterShape) != null)) {
			return _masterShape.getEndY();
		}
		return _endY;
	}

	public Double getAngle() {
		if (((_angle) == null) && ((_masterShape) != null)) {
			return _masterShape.getAngle();
		}
		return _angle;
	}

	public Boolean getFlipX() {
		if (((_flipX) == null) && ((_masterShape) != null)) {
			return _masterShape.getFlipX();
		}
		return _flipX;
	}

	public Boolean getFlipY() {
		if (((_flipY) == null) && ((_masterShape) != null)) {
			return _masterShape.getFlipY();
		}
		return _flipY;
	}

	public Double getTxtPinX() {
		if ((((_txtPinX) == null) && ((_masterShape) != null)) && ((_masterShape._txtPinX) != null)) {
			return _masterShape._txtPinX;
		}
		if ((_txtPinX) == null) {
			return (getWidth()) * 0.5;
		}
		return _txtPinX;
	}

	public Double getTxtPinY() {
		if ((((_txtLocPinY) == null) && ((_masterShape) != null)) && ((_masterShape._txtLocPinY) != null)) {
			return _masterShape._txtLocPinY;
		}
		if ((_txtPinY) == null) {
			return (getHeight()) * 0.5;
		}
		return _txtPinY;
	}

	public Double getTxtLocPinX() {
		if ((((_txtLocPinX) == null) && ((_masterShape) != null)) && ((_masterShape._txtLocPinX) != null)) {
			return _masterShape._txtLocPinX;
		}
		if ((_txtLocPinX) == null) {
			return (getTxtWidth()) * 0.5;
		}
		return _txtLocPinX;
	}

	public Double getTxtLocPinY() {
		if ((((_txtLocPinY) == null) && ((_masterShape) != null)) && ((_masterShape._txtLocPinY) != null)) {
			return _masterShape._txtLocPinY;
		}
		if ((_txtLocPinY) == null) {
			return (getTxtHeight()) * 0.5;
		}
		return _txtLocPinY;
	}

	public Double getTxtAngle() {
		if (((_txtAngle) == null) && ((_masterShape) != null)) {
			return _masterShape.getTxtAngle();
		}
		return _txtAngle;
	}

	public Double getTxtWidth() {
		if ((((_txtWidth) == null) && ((_masterShape) != null)) && ((_masterShape._txtWidth) != null)) {
			return _masterShape._txtWidth;
		}
		if ((_txtWidth) == null) {
			return getWidth();
		}
		return _txtWidth;
	}

	public Double getTxtHeight() {
		if ((((_txtHeight) == null) && ((_masterShape) != null)) && ((_masterShape._txtHeight) != null)) {
			return _masterShape._txtHeight;
		}
		if ((_txtHeight) == null) {
			return getHeight();
		}
		return _txtHeight;
	}

	@Override
	public Integer getLineCap() {
		Integer lineCap = super.getLineCap();
		if (lineCap != null) {
			return lineCap;
		}
		if ((_masterShape) != null) {
			return _masterShape.getLineCap();
		}
		return _document.getDefaultLineStyle().getLineCap();
	}

	@Override
	public Color getLineColor() {
		Color lineColor = super.getLineColor();
		if (lineColor != null) {
			return lineColor;
		}
		if ((_masterShape) != null) {
			return _masterShape.getLineColor();
		}
		return _document.getDefaultLineStyle().getLineColor();
	}

	@Override
	public Integer getLinePattern() {
		Integer linePattern = super.getLinePattern();
		if (linePattern != null) {
			return linePattern;
		}
		if ((_masterShape) != null) {
			return _masterShape.getLinePattern();
		}
		return _document.getDefaultLineStyle().getLinePattern();
	}

	@Override
	public Double getLineWeight() {
		Double lineWeight = super.getLineWeight();
		if (lineWeight != null) {
			return lineWeight;
		}
		if ((_masterShape) != null) {
			return _masterShape.getLineWeight();
		}
		return _document.getDefaultLineStyle().getLineWeight();
	}

	@Override
	public Color getFontColor() {
		Color fontColor = super.getFontColor();
		if (fontColor != null) {
			return fontColor;
		}
		if ((_masterShape) != null) {
			return _masterShape.getFontColor();
		}
		return _document.getDefaultTextStyle().getFontColor();
	}

	@Override
	public Double getFontSize() {
		Double fontSize = super.getFontSize();
		if (fontSize != null) {
			return fontSize;
		}
		if ((_masterShape) != null) {
			return _masterShape.getFontSize();
		}
		return _document.getDefaultTextStyle().getFontSize();
	}

	public Stroke getStroke() {
		float lineWeight = getLineWeight().floatValue();
		int cap;
		int join = BasicStroke.JOIN_MITER;
		float miterlimit = 10.0F;
		switch (getLineCap()) {
			case 0 :
				cap = BasicStroke.CAP_ROUND;
				break;
			case 1 :
				cap = BasicStroke.CAP_SQUARE;
				break;
			case 2 :
				cap = BasicStroke.CAP_BUTT;
				break;
			default :
				throw new POIXMLException("Invalid line cap specified");
		}
		float[] dash = null;
		switch (getLinePattern()) {
			case 0 :
				break;
			case 1 :
				break;
			case 2 :
				dash = new float[]{ 5, 3 };
				break;
			case 3 :
				dash = new float[]{ 1, 4 };
				break;
			case 4 :
				dash = new float[]{ 6, 3, 1, 3 };
				break;
			case 5 :
				dash = new float[]{ 6, 3, 1, 3, 1, 3 };
				break;
			case 6 :
				dash = new float[]{ 1, 3, 6, 3, 6, 3 };
				break;
			case 7 :
				dash = new float[]{ 15, 3, 6, 3 };
				break;
			case 8 :
				dash = new float[]{ 6, 3, 6, 3 };
				break;
			case 9 :
				dash = new float[]{ 3, 2 };
				break;
			case 10 :
				dash = new float[]{ 1, 2 };
				break;
			case 11 :
				dash = new float[]{ 3, 2, 1, 2 };
				break;
			case 12 :
				dash = new float[]{ 3, 2, 1, 2, 1 };
				break;
			case 13 :
				dash = new float[]{ 1, 2, 3, 2, 3, 2 };
				break;
			case 14 :
				dash = new float[]{ 3, 2, 7, 2 };
				break;
			case 15 :
				dash = new float[]{ 7, 2, 3, 2, 3, 2 };
				break;
			case 16 :
				dash = new float[]{ 12, 6 };
				break;
			case 17 :
				dash = new float[]{ 1, 6 };
				break;
			case 18 :
				dash = new float[]{ 1, 6, 12, 6 };
				break;
			case 19 :
				dash = new float[]{ 1, 6, 1, 6, 12, 6 };
				break;
			case 20 :
				dash = new float[]{ 1, 6, 12, 6, 12, 6 };
				break;
			case 21 :
				dash = new float[]{ 30, 6, 12, 6 };
				break;
			case 22 :
				dash = new float[]{ 30, 6, 12, 6, 12, 6 };
				break;
			case 23 :
				dash = new float[]{ 1 };
				break;
			case 254 :
				throw new POIXMLException("Unsupported line pattern value");
			default :
				throw new POIXMLException("Invalid line pattern value");
		}
		if (dash != null) {
			for (int i = 0; i < (dash.length); i++) {
				dash[i] *= lineWeight;
			}
		}
		return new BasicStroke(lineWeight, cap, join, miterlimit, dash, 0);
	}

	public Iterable<GeometrySection> getGeometrySections() {
		return new CombinedIterable<>(_geometry, ((_masterShape) != null ? _masterShape._geometry : null));
	}

	public Rectangle2D.Double getBounds() {
		return new Rectangle2D.Double(0, 0, getWidth(), getHeight());
	}

	public Path2D.Double getBoundsAsPath() {
		Double w = getWidth();
		Double h = getHeight();
		Path2D.Double bounds = new Path2D.Double();
		bounds.moveTo(0, 0);
		bounds.lineTo(w, 0);
		bounds.lineTo(w, h);
		bounds.lineTo(0, h);
		bounds.lineTo(0, 0);
		return bounds;
	}

	public Path2D.Double getPath() {
		for (GeometrySection geoSection : getGeometrySections()) {
			if (geoSection.getNoShow()) {
				continue;
			}
		}
		return null;
	}

	public boolean hasGeometry() {
		for (GeometrySection geoSection : getGeometrySections()) {
			if (!(geoSection.getNoShow())) {
				return true;
			}
		}
		return false;
	}

	protected AffineTransform getParentTransform() {
		AffineTransform tr = new AffineTransform();
		Double locX = getLocPinX();
		Double locY = getLocPinY();
		Boolean flipX = getFlipX();
		Boolean flipY = getFlipY();
		Double angle = getAngle();
		tr.translate((-locX), (-locY));
		tr.translate(getPinX(), getPinY());
		if ((angle != null) && ((Math.abs(angle)) > 0.001)) {
			tr.rotate(angle, locX, locY);
		}
		if ((flipX != null) && flipX) {
			tr.scale((-1), 1);
			tr.translate((-(getWidth())), 0);
		}
		if ((flipY != null) && flipY) {
			tr.scale(1, (-1));
			tr.translate(0, (-(getHeight())));
		}
		return tr;
	}

	public void visitShapes(ShapeVisitor visitor, AffineTransform tr, int level) {
		tr = ((AffineTransform) (tr.clone()));
		tr.concatenate(getParentTransform());
		try {
			if ((_shapes) != null) {
				for (XDGFShape shape : _shapes) {
					shape.visitShapes(visitor, tr, (level + 1));
				}
			}
		} catch (StopVisitingThisBranch e) {
		} catch (POIXMLException e) {
			throw XDGFException.wrap(this.toString(), e);
		}
	}

	public void visitShapes(ShapeVisitor visitor, int level) {
		try {
			if ((_shapes) != null) {
				for (XDGFShape shape : _shapes) {
					shape.visitShapes(visitor, (level + 1));
				}
			}
		} catch (StopVisitingThisBranch e) {
		} catch (POIXMLException e) {
			throw XDGFException.wrap(this.toString(), e);
		}
	}
}

