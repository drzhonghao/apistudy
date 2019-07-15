

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.POIDocument;
import org.apache.poi.ddf.AbstractEscherOptRecord;
import org.apache.poi.ddf.EscherComplexProperty;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherDgRecord;
import org.apache.poi.ddf.EscherOptRecord;
import org.apache.poi.ddf.EscherProperty;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.ddf.EscherSpgrRecord;
import org.apache.poi.hssf.record.AbstractEscherHolderRecord;
import org.apache.poi.hssf.record.CommonObjectDataSubRecord;
import org.apache.poi.hssf.record.EmbeddedObjectRefSubRecord;
import org.apache.poi.hssf.record.EndSubRecord;
import org.apache.poi.hssf.record.EscherAggregate;
import org.apache.poi.hssf.record.FtCfSubRecord;
import org.apache.poi.hssf.record.FtPioGrbitSubRecord;
import org.apache.poi.hssf.record.NoteRecord;
import org.apache.poi.hssf.record.ObjRecord;
import org.apache.poi.hssf.usermodel.HSSFAnchor;
import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import org.apache.poi.hssf.usermodel.HSSFCombobox;
import org.apache.poi.hssf.usermodel.HSSFComment;
import org.apache.poi.hssf.usermodel.HSSFObjectData;
import org.apache.poi.hssf.usermodel.HSSFPicture;
import org.apache.poi.hssf.usermodel.HSSFPictureData;
import org.apache.poi.hssf.usermodel.HSSFPolygon;
import org.apache.poi.hssf.usermodel.HSSFShape;
import org.apache.poi.hssf.usermodel.HSSFShapeContainer;
import org.apache.poi.hssf.usermodel.HSSFShapeFactory;
import org.apache.poi.hssf.usermodel.HSSFShapeGroup;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFSimpleShape;
import org.apache.poi.hssf.usermodel.HSSFTextbox;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.StringUtil;


public final class HSSFPatriarch implements HSSFShapeContainer , Drawing<HSSFShape> {
	private final List<HSSFShape> _shapes = new ArrayList<>();

	private final EscherSpgrRecord _spgrRecord;

	private final EscherContainerRecord _mainSpgrContainer;

	private EscherAggregate _boundAggregate;

	private final HSSFSheet _sheet;

	HSSFPatriarch(HSSFSheet sheet, EscherAggregate boundAggregate) {
		_sheet = sheet;
		_boundAggregate = boundAggregate;
		_mainSpgrContainer = _boundAggregate.getEscherContainer().getChildContainers().get(0);
		EscherContainerRecord spContainer = ((EscherContainerRecord) (_boundAggregate.getEscherContainer().getChildContainers().get(0).getChild(0)));
		_spgrRecord = spContainer.getChildById(EscherSpgrRecord.RECORD_ID);
		buildShapeTree();
	}

	static HSSFPatriarch createPatriarch(HSSFPatriarch patriarch, HSSFSheet sheet) {
		HSSFPatriarch newPatriarch = new HSSFPatriarch(sheet, new EscherAggregate(true));
		newPatriarch.afterCreate();
		for (HSSFShape shape : patriarch.getChildren()) {
			HSSFShape newShape;
			if (shape instanceof HSSFShapeGroup) {
			}else {
			}
			newShape = null;
			newPatriarch.onCreate(newShape);
			newShape = null;
			newPatriarch.addShape(newShape);
		}
		return newPatriarch;
	}

	protected void preSerialize() {
		Map<Integer, NoteRecord> tailRecords = _boundAggregate.getTailRecords();
		Set<String> coordinates = new HashSet<>(tailRecords.size());
		for (NoteRecord rec : tailRecords.values()) {
			String noteRef = new CellReference(rec.getRow(), rec.getColumn(), true, true).formatAsString();
			if (coordinates.contains(noteRef)) {
				throw new IllegalStateException(("found multiple cell comments for cell " + noteRef));
			}else {
				coordinates.add(noteRef);
			}
		}
	}

	@Override
	public boolean removeShape(HSSFShape shape) {
		return false;
	}

	void afterCreate() {
		_boundAggregate.setMainSpRecordId(newShapeId());
	}

	public HSSFShapeGroup createGroup(HSSFClientAnchor anchor) {
		HSSFShapeGroup group = new HSSFShapeGroup(null, anchor);
		addShape(group);
		onCreate(group);
		return group;
	}

	public HSSFSimpleShape createSimpleShape(HSSFClientAnchor anchor) {
		HSSFSimpleShape shape = new HSSFSimpleShape(null, anchor);
		addShape(shape);
		onCreate(shape);
		return shape;
	}

	public HSSFPicture createPicture(HSSFClientAnchor anchor, int pictureIndex) {
		HSSFPicture shape = new HSSFPicture(null, anchor);
		shape.setPictureIndex(pictureIndex);
		addShape(shape);
		onCreate(shape);
		return shape;
	}

	@Override
	public HSSFPicture createPicture(ClientAnchor anchor, int pictureIndex) {
		return createPicture(((HSSFClientAnchor) (anchor)), pictureIndex);
	}

	@Override
	public HSSFObjectData createObjectData(ClientAnchor anchor, int storageId, int pictureIndex) {
		ObjRecord obj = new ObjRecord();
		CommonObjectDataSubRecord ftCmo = new CommonObjectDataSubRecord();
		ftCmo.setObjectType(CommonObjectDataSubRecord.OBJECT_TYPE_PICTURE);
		ftCmo.setLocked(true);
		ftCmo.setPrintable(true);
		ftCmo.setAutofill(true);
		ftCmo.setAutoline(true);
		ftCmo.setReserved1(0);
		ftCmo.setReserved2(0);
		ftCmo.setReserved3(0);
		obj.addSubRecord(ftCmo);
		FtCfSubRecord ftCf = new FtCfSubRecord();
		HSSFPictureData pictData = getSheet().getWorkbook().getAllPictures().get((pictureIndex - 1));
		switch (pictData.getFormat()) {
			case Workbook.PICTURE_TYPE_WMF :
			case Workbook.PICTURE_TYPE_EMF :
				ftCf.setFlags(FtCfSubRecord.METAFILE_BIT);
				break;
			case Workbook.PICTURE_TYPE_DIB :
			case Workbook.PICTURE_TYPE_PNG :
			case Workbook.PICTURE_TYPE_JPEG :
			case Workbook.PICTURE_TYPE_PICT :
				ftCf.setFlags(FtCfSubRecord.BITMAP_BIT);
				break;
			default :
				throw new IllegalStateException(("Invalid picture type: " + (pictData.getFormat())));
		}
		obj.addSubRecord(ftCf);
		FtPioGrbitSubRecord ftPioGrbit = new FtPioGrbitSubRecord();
		ftPioGrbit.setFlagByBit(FtPioGrbitSubRecord.AUTO_PICT_BIT, true);
		obj.addSubRecord(ftPioGrbit);
		EmbeddedObjectRefSubRecord ftPictFmla = new EmbeddedObjectRefSubRecord();
		ftPictFmla.setUnknownFormulaData(new byte[]{ 2, 0, 0, 0, 0 });
		ftPictFmla.setOleClassname("Paket");
		ftPictFmla.setStorageId(storageId);
		obj.addSubRecord(ftPictFmla);
		obj.addSubRecord(new EndSubRecord());
		String entryName = "MBD" + (HexDump.toHex(storageId));
		DirectoryEntry oleRoot;
		try {
			DirectoryNode dn = _sheet.getWorkbook().getDirectory();
			if (dn == null) {
				throw new FileNotFoundException();
			}
			oleRoot = ((DirectoryEntry) (dn.getEntry(entryName)));
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("trying to add ole shape without actually adding data first - use HSSFWorkbook.addOlePackage first", e);
		}
		HSSFPicture shape = new HSSFPicture(null, ((HSSFClientAnchor) (anchor)));
		shape.setPictureIndex(pictureIndex);
		return null;
	}

	public HSSFPolygon createPolygon(HSSFClientAnchor anchor) {
		return null;
	}

	public HSSFTextbox createTextbox(HSSFClientAnchor anchor) {
		HSSFTextbox shape = new HSSFTextbox(null, anchor);
		addShape(shape);
		onCreate(shape);
		return shape;
	}

	public HSSFComment createComment(HSSFAnchor anchor) {
		HSSFComment shape = new HSSFComment(null, anchor);
		addShape(shape);
		onCreate(shape);
		return shape;
	}

	HSSFSimpleShape createComboBox(HSSFAnchor anchor) {
		HSSFCombobox shape = new HSSFCombobox(null, anchor);
		addShape(shape);
		onCreate(shape);
		return shape;
	}

	@Override
	public HSSFComment createCellComment(ClientAnchor anchor) {
		return createComment(((HSSFAnchor) (anchor)));
	}

	@Override
	public List<HSSFShape> getChildren() {
		return Collections.unmodifiableList(_shapes);
	}

	@Override
	@org.apache.poi.util.Internal
	public void addShape(HSSFShape shape) {
		_shapes.add(shape);
	}

	private void onCreate(HSSFShape shape) {
		EscherContainerRecord spgrContainer = _boundAggregate.getEscherContainer().getChildContainers().get(0);
		int shapeId = newShapeId();
		setFlipFlags(shape);
	}

	public int countOfAllChildren() {
		int count = _shapes.size();
		for (HSSFShape shape : _shapes) {
			count += shape.countOfAllChildren();
		}
		return count;
	}

	@Override
	public void setCoordinates(int x1, int y1, int x2, int y2) {
		_spgrRecord.setRectY1(y1);
		_spgrRecord.setRectY2(y2);
		_spgrRecord.setRectX1(x1);
		_spgrRecord.setRectX2(x2);
	}

	@Override
	public void clear() {
		ArrayList<HSSFShape> copy = new ArrayList<>(_shapes);
		for (HSSFShape shape : copy) {
			removeShape(shape);
		}
	}

	int newShapeId() {
		EscherDgRecord dg = _boundAggregate.getEscherContainer().getChildById(EscherDgRecord.RECORD_ID);
		return 0;
	}

	public boolean containsChart() {
		EscherOptRecord optRecord = ((EscherOptRecord) (_boundAggregate.findFirstWithId(EscherOptRecord.RECORD_ID)));
		if (optRecord == null) {
			return false;
		}
		for (EscherProperty prop : optRecord.getEscherProperties()) {
			if (((prop.getPropertyNumber()) == 896) && (prop.isComplex())) {
				EscherComplexProperty cp = ((EscherComplexProperty) (prop));
				String str = StringUtil.getFromUnicodeLE(cp.getComplexData());
				if (str.equals("Chart 1\u0000")) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public int getX1() {
		return _spgrRecord.getRectX1();
	}

	@Override
	public int getY1() {
		return _spgrRecord.getRectY1();
	}

	@Override
	public int getX2() {
		return _spgrRecord.getRectX2();
	}

	@Override
	public int getY2() {
		return _spgrRecord.getRectY2();
	}

	@org.apache.poi.util.Internal
	public EscherAggregate getBoundAggregate() {
		return _boundAggregate;
	}

	@Override
	public HSSFClientAnchor createAnchor(int dx1, int dy1, int dx2, int dy2, int col1, int row1, int col2, int row2) {
		return new HSSFClientAnchor(dx1, dy1, dx2, dy2, ((short) (col1)), row1, ((short) (col2)), row2);
	}

	void buildShapeTree() {
		EscherContainerRecord dgContainer = _boundAggregate.getEscherContainer();
		if (dgContainer == null) {
			return;
		}
		EscherContainerRecord spgrConrainer = dgContainer.getChildContainers().get(0);
		List<EscherContainerRecord> spgrChildren = spgrConrainer.getChildContainers();
		for (int i = 0; i < (spgrChildren.size()); i++) {
			EscherContainerRecord spContainer = spgrChildren.get(i);
			if (i != 0) {
				HSSFShapeFactory.createShapeTree(spContainer, _boundAggregate, this, _sheet.getWorkbook().getDirectory());
			}
		}
	}

	private void setFlipFlags(HSSFShape shape) {
		if (shape.getAnchor().isHorizontallyFlipped()) {
		}
		if (shape.getAnchor().isVerticallyFlipped()) {
		}
	}

	@Override
	public Iterator<HSSFShape> iterator() {
		return _shapes.iterator();
	}

	protected HSSFSheet getSheet() {
		return _sheet;
	}
}

