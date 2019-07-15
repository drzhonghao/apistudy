

import com.microsoft.schemas.office.visio.x2012.main.ConnectType;
import com.microsoft.schemas.office.visio.x2012.main.ConnectsType;
import com.microsoft.schemas.office.visio.x2012.main.PageContentsType;
import com.microsoft.schemas.office.visio.x2012.main.ShapeSheetType;
import com.microsoft.schemas.office.visio.x2012.main.ShapesType;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.xdgf.exceptions.XDGFException;
import org.apache.poi.xdgf.usermodel.XDGFConnection;
import org.apache.poi.xdgf.usermodel.XDGFDocument;
import org.apache.poi.xdgf.usermodel.XDGFShape;
import org.apache.poi.xdgf.usermodel.shape.ShapeRenderer;
import org.apache.poi.xdgf.usermodel.shape.ShapeVisitor;
import org.apache.poi.xdgf.usermodel.shape.exceptions.StopVisiting;
import org.apache.poi.xdgf.xml.XDGFXMLDocumentPart;


public class XDGFBaseContents extends XDGFXMLDocumentPart {
	protected PageContentsType _pageContents;

	protected List<XDGFShape> _toplevelShapes = new ArrayList<>();

	protected Map<Long, XDGFShape> _shapes = new HashMap<>();

	protected List<XDGFConnection> _connections = new ArrayList<>();

	public XDGFBaseContents(PackagePart part, XDGFDocument document) {
		super(part, document);
	}

	@org.apache.poi.util.Internal
	public PageContentsType getXmlObject() {
		return _pageContents;
	}

	@Override
	protected void onDocumentRead() {
		if (_pageContents.isSetShapes()) {
			for (ShapeSheetType shapeSheet : _pageContents.getShapes().getShapeArray()) {
			}
		}
		if (_pageContents.isSetConnects()) {
			for (ConnectType connect : _pageContents.getConnects().getConnectArray()) {
				XDGFShape from = _shapes.get(connect.getFromSheet());
				XDGFShape to = _shapes.get(connect.getToSheet());
				if (from == null)
					throw new POIXMLException((((this) + "; Connect; Invalid from id: ") + (connect.getFromSheet())));

				if (to == null)
					throw new POIXMLException((((this) + "; Connect; Invalid to id: ") + (connect.getToSheet())));

				_connections.add(new XDGFConnection(connect, from, to));
			}
		}
	}

	protected void addToShapeIndex(XDGFShape shape) {
		_shapes.put(shape.getID(), shape);
		List<XDGFShape> shapes = shape.getShapes();
		if (shapes == null)
			return;

		for (XDGFShape subshape : shapes)
			addToShapeIndex(subshape);

	}

	public void draw(Graphics2D graphics) {
		visitShapes(new ShapeRenderer(graphics));
	}

	public XDGFShape getShapeById(long id) {
		return _shapes.get(id);
	}

	public Map<Long, XDGFShape> getShapesMap() {
		return Collections.unmodifiableMap(_shapes);
	}

	public Collection<XDGFShape> getShapes() {
		return _shapes.values();
	}

	public List<XDGFShape> getTopLevelShapes() {
		return Collections.unmodifiableList(_toplevelShapes);
	}

	public List<XDGFConnection> getConnections() {
		return Collections.unmodifiableList(_connections);
	}

	@Override
	public String toString() {
		return getPackagePart().getPartName().toString();
	}

	public void visitShapes(ShapeVisitor visitor) {
		try {
			for (XDGFShape shape : _toplevelShapes) {
				shape.visitShapes(visitor, new AffineTransform(), 0);
			}
		} catch (StopVisiting e) {
		} catch (POIXMLException e) {
			throw XDGFException.wrap(this, e);
		}
	}
}

