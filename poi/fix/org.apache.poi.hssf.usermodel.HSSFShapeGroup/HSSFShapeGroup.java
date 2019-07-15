

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.ddf.AbstractEscherOptRecord;
import org.apache.poi.ddf.DefaultEscherRecordFactory;
import org.apache.poi.ddf.EscherBoolProperty;
import org.apache.poi.ddf.EscherClientDataRecord;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherOptRecord;
import org.apache.poi.ddf.EscherProperties;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.ddf.EscherSpRecord;
import org.apache.poi.ddf.EscherSpgrRecord;
import org.apache.poi.hssf.record.CommonObjectDataSubRecord;
import org.apache.poi.hssf.record.EndSubRecord;
import org.apache.poi.hssf.record.EscherAggregate;
import org.apache.poi.hssf.record.GroupMarkerSubRecord;
import org.apache.poi.hssf.record.ObjRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.SubRecord;
import org.apache.poi.hssf.usermodel.HSSFAnchor;
import org.apache.poi.hssf.usermodel.HSSFChildAnchor;
import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFPicture;
import org.apache.poi.hssf.usermodel.HSSFPolygon;
import org.apache.poi.hssf.usermodel.HSSFShape;
import org.apache.poi.hssf.usermodel.HSSFShapeContainer;
import org.apache.poi.hssf.usermodel.HSSFSimpleShape;
import org.apache.poi.hssf.usermodel.HSSFTextbox;


public class HSSFShapeGroup extends HSSFShape implements HSSFShapeContainer {
	private final List<HSSFShape> shapes = new ArrayList<>();

	private EscherSpgrRecord _spgrRecord;

	public HSSFShapeGroup(EscherContainerRecord spgrContainer, ObjRecord objRecord) {
		super(spgrContainer, objRecord);
		EscherContainerRecord spContainer = spgrContainer.getChildContainers().get(0);
		_spgrRecord = ((EscherSpgrRecord) (spContainer.getChild(0)));
		for (EscherRecord ch : spContainer.getChildRecords()) {
		}
	}

	public HSSFShapeGroup(HSSFShape parent, HSSFAnchor anchor) {
		super(parent, anchor);
		_spgrRecord = ((EscherContainerRecord) (getEscherContainer().getChild(0))).getChildById(EscherSpgrRecord.RECORD_ID);
	}

	@Override
	protected EscherContainerRecord createSpContainer() {
		EscherContainerRecord spgrContainer = new EscherContainerRecord();
		EscherContainerRecord spContainer = new EscherContainerRecord();
		EscherSpgrRecord spgr = new EscherSpgrRecord();
		EscherSpRecord sp = new EscherSpRecord();
		EscherOptRecord opt = new EscherOptRecord();
		EscherRecord anchor;
		EscherClientDataRecord clientData = new EscherClientDataRecord();
		spgrContainer.setRecordId(EscherContainerRecord.SPGR_CONTAINER);
		spgrContainer.setOptions(((short) (15)));
		spContainer.setRecordId(EscherContainerRecord.SP_CONTAINER);
		spContainer.setOptions(((short) (15)));
		spgr.setRecordId(EscherSpgrRecord.RECORD_ID);
		spgr.setOptions(((short) (1)));
		spgr.setRectX1(0);
		spgr.setRectY1(0);
		spgr.setRectX2(1023);
		spgr.setRectY2(255);
		sp.setRecordId(EscherSpRecord.RECORD_ID);
		sp.setOptions(((short) (2)));
		if ((getAnchor()) instanceof HSSFClientAnchor) {
			sp.setFlags(((EscherSpRecord.FLAG_GROUP) | (EscherSpRecord.FLAG_HAVEANCHOR)));
		}else {
			sp.setFlags((((EscherSpRecord.FLAG_GROUP) | (EscherSpRecord.FLAG_HAVEANCHOR)) | (EscherSpRecord.FLAG_CHILD)));
		}
		opt.setRecordId(EscherOptRecord.RECORD_ID);
		opt.setOptions(((short) (35)));
		opt.addEscherProperty(new EscherBoolProperty(EscherProperties.PROTECTION__LOCKAGAINSTGROUPING, 262148));
		opt.addEscherProperty(new EscherBoolProperty(EscherProperties.GROUPSHAPE__PRINT, 524288));
		clientData.setRecordId(EscherClientDataRecord.RECORD_ID);
		clientData.setOptions(((short) (0)));
		spgrContainer.addChildRecord(spContainer);
		spContainer.addChildRecord(spgr);
		spContainer.addChildRecord(sp);
		spContainer.addChildRecord(opt);
		spContainer.addChildRecord(anchor);
		spContainer.addChildRecord(clientData);
		return spgrContainer;
	}

	@Override
	protected ObjRecord createObjRecord() {
		ObjRecord obj = new ObjRecord();
		CommonObjectDataSubRecord cmo = new CommonObjectDataSubRecord();
		cmo.setObjectType(CommonObjectDataSubRecord.OBJECT_TYPE_GROUP);
		cmo.setLocked(true);
		cmo.setPrintable(true);
		cmo.setAutofill(true);
		cmo.setAutoline(true);
		GroupMarkerSubRecord gmo = new GroupMarkerSubRecord();
		EndSubRecord end = new EndSubRecord();
		obj.addSubRecord(cmo);
		obj.addSubRecord(gmo);
		obj.addSubRecord(end);
		return obj;
	}

	@Override
	protected void afterRemove(HSSFPatriarch patriarch) {
		patriarch.getBoundAggregate().removeShapeToObjRecord(getEscherContainer().getChildContainers().get(0).getChildById(EscherClientDataRecord.RECORD_ID));
		for (int i = 0; i < (shapes.size()); i++) {
			HSSFShape shape = shapes.get(i);
			removeShape(shape);
		}
		shapes.clear();
	}

	private void onCreate(HSSFShape shape) {
		if ((getPatriarch()) != null) {
			EscherSpRecord sp;
			if (shape instanceof HSSFShapeGroup) {
			}else {
			}
			sp.setFlags(((sp.getFlags()) | (EscherSpRecord.FLAG_CHILD)));
		}
	}

	public HSSFShapeGroup createGroup(HSSFChildAnchor anchor) {
		HSSFShapeGroup group = new HSSFShapeGroup(this, anchor);
		group.setParent(this);
		group.setAnchor(anchor);
		shapes.add(group);
		onCreate(group);
		return group;
	}

	public void addShape(HSSFShape shape) {
		shapes.add(shape);
	}

	public HSSFSimpleShape createShape(HSSFChildAnchor anchor) {
		HSSFSimpleShape shape = new HSSFSimpleShape(this, anchor);
		shape.setAnchor(anchor);
		shapes.add(shape);
		onCreate(shape);
		if (shape.getAnchor().isHorizontallyFlipped()) {
		}
		if (shape.getAnchor().isVerticallyFlipped()) {
		}
		return shape;
	}

	public HSSFTextbox createTextbox(HSSFChildAnchor anchor) {
		HSSFTextbox shape = new HSSFTextbox(this, anchor);
		shape.setAnchor(anchor);
		shapes.add(shape);
		onCreate(shape);
		return shape;
	}

	public HSSFPolygon createPolygon(HSSFChildAnchor anchor) {
	}

	public HSSFPicture createPicture(HSSFChildAnchor anchor, int pictureIndex) {
		HSSFPicture shape = new HSSFPicture(this, anchor);
		shape.setAnchor(anchor);
		shape.setPictureIndex(pictureIndex);
		shapes.add(shape);
		onCreate(shape);
		if (shape.getAnchor().isHorizontallyFlipped()) {
		}
		if (shape.getAnchor().isVerticallyFlipped()) {
		}
		return shape;
	}

	public List<HSSFShape> getChildren() {
		return Collections.unmodifiableList(shapes);
	}

	public void setCoordinates(int x1, int y1, int x2, int y2) {
		_spgrRecord.setRectX1(x1);
		_spgrRecord.setRectX2(x2);
		_spgrRecord.setRectY1(y1);
		_spgrRecord.setRectY2(y2);
	}

	public void clear() {
		ArrayList<HSSFShape> copy = new ArrayList<>(shapes);
		for (HSSFShape shape : copy) {
			removeShape(shape);
		}
	}

	public int getX1() {
		return _spgrRecord.getRectX1();
	}

	public int getY1() {
		return _spgrRecord.getRectY1();
	}

	public int getX2() {
		return _spgrRecord.getRectX2();
	}

	public int getY2() {
		return _spgrRecord.getRectY2();
	}

	public int countOfAllChildren() {
		int count = shapes.size();
		for (Iterator<HSSFShape> iterator = shapes.iterator(); iterator.hasNext();) {
			HSSFShape shape = iterator.next();
			count += shape.countOfAllChildren();
		}
		return count;
	}

	void afterInsert(HSSFPatriarch patriarch) {
		EscherAggregate agg = patriarch.getBoundAggregate();
		EscherContainerRecord containerRecord = getEscherContainer().getChildById(EscherContainerRecord.SP_CONTAINER);
		agg.associateShapeToObjRecord(containerRecord.getChildById(EscherClientDataRecord.RECORD_ID), getObjRecord());
	}

	void setShapeId(int shapeId) {
		EscherContainerRecord containerRecord = getEscherContainer().getChildById(EscherContainerRecord.SP_CONTAINER);
		EscherSpRecord spRecord = containerRecord.getChildById(EscherSpRecord.RECORD_ID);
		spRecord.setShapeId(shapeId);
		CommonObjectDataSubRecord cod = ((CommonObjectDataSubRecord) (getObjRecord().getSubRecords().get(0)));
		cod.setObjectId(((short) (shapeId % 1024)));
	}

	int getShapeId() {
		EscherContainerRecord containerRecord = getEscherContainer().getChildById(EscherContainerRecord.SP_CONTAINER);
		return ((EscherSpRecord) (containerRecord.getChildById(EscherSpRecord.RECORD_ID))).getShapeId();
	}

	@Override
	protected HSSFShape cloneShape() {
		throw new IllegalStateException("Use method cloneShape(HSSFPatriarch patriarch)");
	}

	protected HSSFShape cloneShape(HSSFPatriarch patriarch) {
		EscherContainerRecord spgrContainer = new EscherContainerRecord();
		spgrContainer.setRecordId(EscherContainerRecord.SPGR_CONTAINER);
		spgrContainer.setOptions(((short) (15)));
		EscherContainerRecord spContainer = new EscherContainerRecord();
		EscherContainerRecord cont = getEscherContainer().getChildById(EscherContainerRecord.SP_CONTAINER);
		byte[] inSp = cont.serialize();
		spContainer.fillFields(inSp, 0, new DefaultEscherRecordFactory());
		spgrContainer.addChildRecord(spContainer);
		ObjRecord obj = null;
		if (null != (getObjRecord())) {
			obj = ((ObjRecord) (getObjRecord().cloneViaReserialise()));
		}
		HSSFShapeGroup group = new HSSFShapeGroup(spgrContainer, obj);
		group.setPatriarch(patriarch);
		for (HSSFShape shape : getChildren()) {
			HSSFShape newShape;
			if (shape instanceof HSSFShapeGroup) {
				newShape = ((HSSFShapeGroup) (shape)).cloneShape(patriarch);
			}else {
			}
			group.addShape(newShape);
			group.onCreate(newShape);
		}
		return group;
	}

	public boolean removeShape(HSSFShape shape) {
	}

	@Override
	public Iterator<HSSFShape> iterator() {
		return shapes.iterator();
	}
}

