

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherDgRecord;
import org.apache.poi.ddf.EscherDggRecord;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.hslf.exceptions.HSLFException;
import org.apache.poi.hslf.model.HeadersFooters;
import org.apache.poi.hslf.record.CString;
import org.apache.poi.hslf.record.ColorSchemeAtom;
import org.apache.poi.hslf.record.Document;
import org.apache.poi.hslf.record.PPDrawing;
import org.apache.poi.hslf.record.PPDrawingGroup;
import org.apache.poi.hslf.record.PositionDependentRecordContainer;
import org.apache.poi.hslf.record.Record;
import org.apache.poi.hslf.record.RecordContainer;
import org.apache.poi.hslf.record.RecordTypes;
import org.apache.poi.hslf.record.SheetContainer;
import org.apache.poi.hslf.usermodel.HSLFAutoShape;
import org.apache.poi.hslf.usermodel.HSLFBackground;
import org.apache.poi.hslf.usermodel.HSLFConnectorShape;
import org.apache.poi.hslf.usermodel.HSLFFreeformShape;
import org.apache.poi.hslf.usermodel.HSLFGroupShape;
import org.apache.poi.hslf.usermodel.HSLFMasterSheet;
import org.apache.poi.hslf.usermodel.HSLFObjectShape;
import org.apache.poi.hslf.usermodel.HSLFPictureData;
import org.apache.poi.hslf.usermodel.HSLFPictureShape;
import org.apache.poi.hslf.usermodel.HSLFPlaceholderDetails;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFShapeContainer;
import org.apache.poi.hslf.usermodel.HSLFShapeFactory;
import org.apache.poi.hslf.usermodel.HSLFSimpleShape;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTable;
import org.apache.poi.hslf.usermodel.HSLFTextBox;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.sl.draw.DrawFactory;
import org.apache.poi.sl.draw.DrawSheet;
import org.apache.poi.sl.draw.Drawable;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.sl.usermodel.Sheet;


public abstract class HSLFSheet implements HSLFShapeContainer , Sheet<HSLFShape, HSLFTextParagraph> {
	private HSLFSlideShow _slideShow;

	private HSLFBackground _background;

	private SheetContainer _container;

	private int _sheetNo;

	public HSLFSheet(SheetContainer container, int sheetNo) {
		_container = container;
		_sheetNo = sheetNo;
	}

	public abstract List<List<HSLFTextParagraph>> getTextParagraphs();

	public int _getSheetRefId() {
		return _container.getSheetId();
	}

	public int _getSheetNumber() {
		return _sheetNo;
	}

	public PPDrawing getPPDrawing() {
		return _container.getPPDrawing();
	}

	@Override
	public HSLFSlideShow getSlideShow() {
		return _slideShow;
	}

	public SheetContainer getSheetContainer() {
		return _container;
	}

	@org.apache.poi.util.Internal
	protected void setSlideShow(HSLFSlideShow ss) {
		if ((_slideShow) != null) {
			throw new HSLFException("Can't change existing slideshow reference");
		}
		_slideShow = ss;
		List<List<HSLFTextParagraph>> trs = getTextParagraphs();
		if (trs == null) {
			return;
		}
		for (List<HSLFTextParagraph> ltp : trs) {
		}
	}

	@Override
	public List<HSLFShape> getShapes() {
		PPDrawing ppdrawing = getPPDrawing();
		EscherContainerRecord dg = ppdrawing.getDgContainer();
		EscherContainerRecord spgr = null;
		for (EscherRecord rec : dg) {
			if ((rec.getRecordId()) == (EscherContainerRecord.SPGR_CONTAINER)) {
				spgr = ((EscherContainerRecord) (rec));
				break;
			}
		}
		if (spgr == null) {
			throw new IllegalStateException("spgr not found");
		}
		List<HSLFShape> shapeList = new ArrayList<>();
		boolean isFirst = true;
		for (EscherRecord r : spgr) {
			if (isFirst) {
				isFirst = false;
				continue;
			}
			EscherContainerRecord sp = ((EscherContainerRecord) (r));
			HSLFShape sh = HSLFShapeFactory.createShape(sp, null);
			if (sh instanceof HSLFSimpleShape) {
			}
			shapeList.add(sh);
		}
		return shapeList;
	}

	@Override
	public void addShape(HSLFShape shape) {
		PPDrawing ppdrawing = getPPDrawing();
		EscherContainerRecord dgContainer = ppdrawing.getDgContainer();
		EscherContainerRecord spgr = HSLFShape.getEscherChild(dgContainer, EscherContainerRecord.SPGR_CONTAINER);
		spgr.addChildRecord(shape.getSpContainer());
		shape.setShapeId(allocateShapeId());
	}

	public int allocateShapeId() {
		EscherDggRecord dgg = _slideShow.getDocumentRecord().getPPDrawingGroup().getEscherDggRecord();
		EscherDgRecord dg = _container.getPPDrawing().getEscherDgRecord();
		return dgg.allocateShapeId(dg, false);
	}

	@Override
	public boolean removeShape(HSLFShape shape) {
		PPDrawing ppdrawing = getPPDrawing();
		EscherContainerRecord dg = ppdrawing.getDgContainer();
		EscherContainerRecord spgr = dg.getChildById(EscherContainerRecord.SPGR_CONTAINER);
		if (spgr == null) {
			return false;
		}
		List<EscherRecord> lst = spgr.getChildRecords();
		boolean result = lst.remove(shape.getSpContainer());
		spgr.setChildRecords(lst);
		return result;
	}

	public void onCreate() {
	}

	@Override
	public abstract HSLFMasterSheet getMasterSheet();

	public ColorSchemeAtom getColorScheme() {
		return _container.getColorScheme();
	}

	@Override
	public HSLFBackground getBackground() {
		if ((_background) == null) {
			PPDrawing ppdrawing = getPPDrawing();
			EscherContainerRecord dg = ppdrawing.getDgContainer();
			EscherContainerRecord spContainer = dg.getChildById(EscherContainerRecord.SP_CONTAINER);
		}
		return _background;
	}

	@Override
	public void draw(Graphics2D graphics) {
		DrawFactory drawFact = DrawFactory.getInstance(graphics);
		Drawable draw = drawFact.getDrawable(this);
		draw.draw(graphics);
	}

	protected void onAddTextShape(HSLFTextShape shape) {
	}

	public HSLFTextShape getPlaceholderByTextType(int type) {
		for (HSLFShape shape : getShapes()) {
			if (shape instanceof HSLFTextShape) {
				HSLFTextShape tx = ((HSLFTextShape) (shape));
				if ((tx.getRunType()) == type) {
					return tx;
				}
			}
		}
		return null;
	}

	public HSLFSimpleShape getPlaceholder(Placeholder type) {
		for (HSLFShape shape : getShapes()) {
			if (shape instanceof HSLFSimpleShape) {
				HSLFSimpleShape ss = ((HSLFSimpleShape) (shape));
				if (type == (ss.getPlaceholder())) {
					return ss;
				}
			}
		}
		return null;
	}

	public String getProgrammableTag() {
		String tag = null;
		RecordContainer progTags = ((RecordContainer) (getSheetContainer().findFirstOfType(RecordTypes.ProgTags.typeID)));
		if (progTags != null) {
			RecordContainer progBinaryTag = ((RecordContainer) (progTags.findFirstOfType(RecordTypes.ProgBinaryTag.typeID)));
			if (progBinaryTag != null) {
				CString binaryTag = ((CString) (progBinaryTag.findFirstOfType(RecordTypes.CString.typeID)));
				if (binaryTag != null) {
					tag = binaryTag.getText();
				}
			}
		}
		return tag;
	}

	@Override
	public Iterator<HSLFShape> iterator() {
		return getShapes().iterator();
	}

	@Override
	public boolean getFollowMasterGraphics() {
		return false;
	}

	@Override
	public HSLFTextBox createTextBox() {
		HSLFTextBox s = new HSLFTextBox();
		s.setHorizontalCentered(true);
		s.setAnchor(new Rectangle2D.Double(0, 0, 100, 100));
		addShape(s);
		return s;
	}

	@Override
	public HSLFAutoShape createAutoShape() {
		HSLFAutoShape s = new HSLFAutoShape(ShapeType.RECT);
		s.setHorizontalCentered(true);
		s.setAnchor(new Rectangle2D.Double(0, 0, 100, 100));
		addShape(s);
		return s;
	}

	@Override
	public HSLFFreeformShape createFreeform() {
		HSLFFreeformShape s = new HSLFFreeformShape();
		s.setHorizontalCentered(true);
		s.setAnchor(new Rectangle2D.Double(0, 0, 100, 100));
		addShape(s);
		return s;
	}

	@Override
	public HSLFConnectorShape createConnector() {
		HSLFConnectorShape s = new HSLFConnectorShape();
		s.setAnchor(new Rectangle2D.Double(0, 0, 100, 100));
		addShape(s);
		return s;
	}

	@Override
	public HSLFGroupShape createGroup() {
		HSLFGroupShape s = new HSLFGroupShape();
		s.setAnchor(new Rectangle2D.Double(0, 0, 100, 100));
		addShape(s);
		return s;
	}

	@Override
	public HSLFPictureShape createPicture(PictureData pictureData) {
		if (!(pictureData instanceof HSLFPictureData)) {
			throw new IllegalArgumentException("pictureData needs to be of type HSLFPictureData");
		}
		HSLFPictureShape s = new HSLFPictureShape(((HSLFPictureData) (pictureData)));
		s.setAnchor(new Rectangle2D.Double(0, 0, 100, 100));
		addShape(s);
		return s;
	}

	@Override
	public HSLFTable createTable(int numRows, int numCols) {
		if ((numRows < 1) || (numCols < 1)) {
			throw new IllegalArgumentException("numRows and numCols must be greater than 0");
		}
		return null;
	}

	@Override
	public HSLFObjectShape createOleShape(PictureData pictureData) {
		if (!(pictureData instanceof HSLFPictureData)) {
			throw new IllegalArgumentException("pictureData needs to be of type HSLFPictureData");
		}
		HSLFObjectShape s = new HSLFObjectShape(((HSLFPictureData) (pictureData)));
		s.setAnchor(new Rectangle2D.Double(0, 0, 100, 100));
		addShape(s);
		return s;
	}

	public HeadersFooters getHeadersFooters() {
		return null;
	}

	@Override
	public HSLFPlaceholderDetails getPlaceholderDetails(Placeholder placeholder) {
		final HSLFSimpleShape ph = getPlaceholder(placeholder);
		return null;
	}
}

