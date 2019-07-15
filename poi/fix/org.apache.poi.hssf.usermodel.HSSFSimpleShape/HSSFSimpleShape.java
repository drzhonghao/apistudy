

import java.util.List;
import org.apache.poi.ddf.AbstractEscherOptRecord;
import org.apache.poi.ddf.DefaultEscherRecordFactory;
import org.apache.poi.ddf.EscherBoolProperty;
import org.apache.poi.ddf.EscherClientDataRecord;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherOptRecord;
import org.apache.poi.ddf.EscherProperties;
import org.apache.poi.ddf.EscherRGBProperty;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.ddf.EscherShapePathProperty;
import org.apache.poi.ddf.EscherSimpleProperty;
import org.apache.poi.ddf.EscherSpRecord;
import org.apache.poi.ddf.EscherTextboxRecord;
import org.apache.poi.hssf.record.CommonObjectDataSubRecord;
import org.apache.poi.hssf.record.EndSubRecord;
import org.apache.poi.hssf.record.EscherAggregate;
import org.apache.poi.hssf.record.ObjRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.SubRecord;
import org.apache.poi.hssf.record.TextObjectRecord;
import org.apache.poi.hssf.usermodel.HSSFAnchor;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFShape;
import org.apache.poi.hssf.usermodel.HSSFShapeTypes;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.SimpleShape;


public class HSSFSimpleShape extends HSSFShape implements SimpleShape {
	public static final short OBJECT_TYPE_LINE = HSSFShapeTypes.Line;

	public static final short OBJECT_TYPE_RECTANGLE = HSSFShapeTypes.Rectangle;

	public static final short OBJECT_TYPE_OVAL = HSSFShapeTypes.Ellipse;

	public static final short OBJECT_TYPE_ARC = HSSFShapeTypes.Arc;

	public static final short OBJECT_TYPE_PICTURE = HSSFShapeTypes.PictureFrame;

	public static final short OBJECT_TYPE_COMBO_BOX = HSSFShapeTypes.HostControl;

	public static final short OBJECT_TYPE_COMMENT = HSSFShapeTypes.TextBox;

	public static final short OBJECT_TYPE_MICROSOFT_OFFICE_DRAWING = 30;

	public static final int WRAP_SQUARE = 0;

	public static final int WRAP_BY_POINTS = 1;

	public static final int WRAP_NONE = 2;

	private TextObjectRecord _textObjectRecord;

	public HSSFSimpleShape(EscherContainerRecord spContainer, ObjRecord objRecord, TextObjectRecord textObjectRecord) {
		super(spContainer, objRecord);
		this._textObjectRecord = textObjectRecord;
	}

	public HSSFSimpleShape(EscherContainerRecord spContainer, ObjRecord objRecord) {
		super(spContainer, objRecord);
	}

	public HSSFSimpleShape(HSSFShape parent, HSSFAnchor anchor) {
		super(parent, anchor);
		_textObjectRecord = createTextObjRecord();
	}

	protected TextObjectRecord getTextObjectRecord() {
		return _textObjectRecord;
	}

	protected TextObjectRecord createTextObjRecord() {
		TextObjectRecord obj = new TextObjectRecord();
		obj.setHorizontalTextAlignment(2);
		obj.setVerticalTextAlignment(2);
		obj.setTextLocked(true);
		obj.setTextOrientation(TextObjectRecord.TEXT_ORIENTATION_NONE);
		obj.setStr(new HSSFRichTextString(""));
		return obj;
	}

	@Override
	protected EscherContainerRecord createSpContainer() {
		EscherContainerRecord spContainer = new EscherContainerRecord();
		spContainer.setRecordId(EscherContainerRecord.SP_CONTAINER);
		spContainer.setOptions(((short) (15)));
		EscherSpRecord sp = new EscherSpRecord();
		sp.setRecordId(EscherSpRecord.RECORD_ID);
		sp.setFlags(((EscherSpRecord.FLAG_HAVEANCHOR) | (EscherSpRecord.FLAG_HASSHAPETYPE)));
		sp.setVersion(((short) (2)));
		EscherClientDataRecord clientData = new EscherClientDataRecord();
		clientData.setRecordId(EscherClientDataRecord.RECORD_ID);
		clientData.setOptions(((short) (0)));
		EscherOptRecord optRecord = new EscherOptRecord();
		optRecord.setEscherProperty(new EscherSimpleProperty(EscherProperties.LINESTYLE__LINEDASHING, HSSFShape.LINESTYLE_SOLID));
		optRecord.setEscherProperty(new EscherBoolProperty(EscherProperties.LINESTYLE__NOLINEDRAWDASH, 524296));
		optRecord.setEscherProperty(new EscherRGBProperty(EscherProperties.FILL__FILLCOLOR, HSSFShape.FILL__FILLCOLOR_DEFAULT));
		optRecord.setEscherProperty(new EscherRGBProperty(EscherProperties.LINESTYLE__COLOR, HSSFShape.LINESTYLE__COLOR_DEFAULT));
		optRecord.setEscherProperty(new EscherBoolProperty(EscherProperties.FILL__NOFILLHITTEST, HSSFShape.NO_FILLHITTEST_FALSE));
		optRecord.setEscherProperty(new EscherBoolProperty(EscherProperties.LINESTYLE__NOLINEDRAWDASH, 524296));
		optRecord.setEscherProperty(new EscherShapePathProperty(EscherProperties.GEOMETRY__SHAPEPATH, EscherShapePathProperty.COMPLEX));
		optRecord.setEscherProperty(new EscherBoolProperty(EscherProperties.GROUPSHAPE__PRINT, 524288));
		optRecord.setRecordId(EscherOptRecord.RECORD_ID);
		EscherTextboxRecord escherTextbox = new EscherTextboxRecord();
		escherTextbox.setRecordId(EscherTextboxRecord.RECORD_ID);
		escherTextbox.setOptions(((short) (0)));
		spContainer.addChildRecord(sp);
		spContainer.addChildRecord(optRecord);
		spContainer.addChildRecord(clientData);
		spContainer.addChildRecord(escherTextbox);
		return spContainer;
	}

	@Override
	protected ObjRecord createObjRecord() {
		ObjRecord obj = new ObjRecord();
		CommonObjectDataSubRecord c = new CommonObjectDataSubRecord();
		c.setLocked(true);
		c.setPrintable(true);
		c.setAutofill(true);
		c.setAutoline(true);
		EndSubRecord e = new EndSubRecord();
		obj.addSubRecord(c);
		obj.addSubRecord(e);
		return obj;
	}

	@Override
	protected void afterRemove(HSSFPatriarch patriarch) {
		patriarch.getBoundAggregate().removeShapeToObjRecord(getEscherContainer().getChildById(EscherClientDataRecord.RECORD_ID));
		if (null != (getEscherContainer().getChildById(EscherTextboxRecord.RECORD_ID))) {
			patriarch.getBoundAggregate().removeShapeToObjRecord(getEscherContainer().getChildById(EscherTextboxRecord.RECORD_ID));
		}
	}

	public HSSFRichTextString getString() {
		return _textObjectRecord.getStr();
	}

	public void setString(RichTextString string) {
		if (((getShapeType()) == 0) || ((getShapeType()) == (HSSFSimpleShape.OBJECT_TYPE_LINE))) {
			throw new IllegalStateException(("Cannot set text for shape type: " + (getShapeType())));
		}
		HSSFRichTextString rtr = ((HSSFRichTextString) (string));
		if ((rtr.numFormattingRuns()) == 0)
			rtr.applyFont(((short) (0)));

		TextObjectRecord txo = getOrCreateTextObjRecord();
		txo.setStr(rtr);
		if ((string.getString()) != null) {
			setPropertyValue(new EscherSimpleProperty(EscherProperties.TEXT__TEXTID, string.getString().hashCode()));
		}
	}

	void afterInsert(HSSFPatriarch patriarch) {
		EscherAggregate agg = patriarch.getBoundAggregate();
		agg.associateShapeToObjRecord(getEscherContainer().getChildById(EscherClientDataRecord.RECORD_ID), getObjRecord());
		if (null != (getTextObjectRecord())) {
			agg.associateShapeToObjRecord(getEscherContainer().getChildById(EscherTextboxRecord.RECORD_ID), getTextObjectRecord());
		}
	}

	@Override
	protected HSSFShape cloneShape() {
		TextObjectRecord txo = null;
		EscherContainerRecord spContainer = new EscherContainerRecord();
		byte[] inSp = getEscherContainer().serialize();
		spContainer.fillFields(inSp, 0, new DefaultEscherRecordFactory());
		ObjRecord obj = ((ObjRecord) (getObjRecord().cloneViaReserialise()));
		if ((((getTextObjectRecord()) != null) && ((getString()) != null)) && (null != (getString().getString()))) {
			txo = ((TextObjectRecord) (getTextObjectRecord().cloneViaReserialise()));
		}
		return new HSSFSimpleShape(spContainer, obj, txo);
	}

	public int getShapeType() {
		EscherSpRecord spRecord = getEscherContainer().getChildById(EscherSpRecord.RECORD_ID);
		return spRecord.getShapeType();
	}

	public int getWrapText() {
		EscherSimpleProperty property = getOptRecord().lookup(EscherProperties.TEXT__WRAPTEXT);
		return null == property ? HSSFSimpleShape.WRAP_SQUARE : property.getPropertyValue();
	}

	public void setWrapText(int value) {
		setPropertyValue(new EscherSimpleProperty(EscherProperties.TEXT__WRAPTEXT, false, false, value));
	}

	public void setShapeType(int value) {
		CommonObjectDataSubRecord cod = ((CommonObjectDataSubRecord) (getObjRecord().getSubRecords().get(0)));
		cod.setObjectType(HSSFSimpleShape.OBJECT_TYPE_MICROSOFT_OFFICE_DRAWING);
		EscherSpRecord spRecord = getEscherContainer().getChildById(EscherSpRecord.RECORD_ID);
		spRecord.setShapeType(((short) (value)));
	}

	private TextObjectRecord getOrCreateTextObjRecord() {
		if ((getTextObjectRecord()) == null) {
			_textObjectRecord = createTextObjRecord();
		}
		EscherTextboxRecord escherTextbox = getEscherContainer().getChildById(EscherTextboxRecord.RECORD_ID);
		if (null == escherTextbox) {
			escherTextbox = new EscherTextboxRecord();
			escherTextbox.setRecordId(EscherTextboxRecord.RECORD_ID);
			escherTextbox.setOptions(((short) (0)));
			getEscherContainer().addChildRecord(escherTextbox);
			getPatriarch().getBoundAggregate().associateShapeToObjRecord(escherTextbox, _textObjectRecord);
		}
		return _textObjectRecord;
	}

	@Override
	public int getShapeId() {
	}
}

