

import org.apache.poi.ddf.AbstractEscherOptRecord;
import org.apache.poi.ddf.DefaultEscherRecordFactory;
import org.apache.poi.ddf.EscherBoolProperty;
import org.apache.poi.ddf.EscherClientDataRecord;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherOptRecord;
import org.apache.poi.ddf.EscherProperties;
import org.apache.poi.ddf.EscherRGBProperty;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.ddf.EscherSimpleProperty;
import org.apache.poi.ddf.EscherSpRecord;
import org.apache.poi.ddf.EscherTextboxRecord;
import org.apache.poi.hssf.record.CommonObjectDataSubRecord;
import org.apache.poi.hssf.record.EndSubRecord;
import org.apache.poi.hssf.record.EscherAggregate;
import org.apache.poi.hssf.record.ObjRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.TextObjectRecord;
import org.apache.poi.hssf.usermodel.HSSFAnchor;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFShape;
import org.apache.poi.hssf.usermodel.HSSFSimpleShape;


public class HSSFTextbox extends HSSFSimpleShape {
	public static final short OBJECT_TYPE_TEXT = 6;

	public static final short HORIZONTAL_ALIGNMENT_LEFT = 1;

	public static final short HORIZONTAL_ALIGNMENT_CENTERED = 2;

	public static final short HORIZONTAL_ALIGNMENT_RIGHT = 3;

	public static final short HORIZONTAL_ALIGNMENT_JUSTIFIED = 4;

	public static final short HORIZONTAL_ALIGNMENT_DISTRIBUTED = 7;

	public static final short VERTICAL_ALIGNMENT_TOP = 1;

	public static final short VERTICAL_ALIGNMENT_CENTER = 2;

	public static final short VERTICAL_ALIGNMENT_BOTTOM = 3;

	public static final short VERTICAL_ALIGNMENT_JUSTIFY = 4;

	public static final short VERTICAL_ALIGNMENT_DISTRIBUTED = 7;

	public HSSFTextbox(EscherContainerRecord spContainer, ObjRecord objRecord, TextObjectRecord textObjectRecord) {
		super(spContainer, objRecord, textObjectRecord);
	}

	public HSSFTextbox(HSSFShape parent, HSSFAnchor anchor) {
		super(parent, anchor);
		setHorizontalAlignment(HSSFTextbox.HORIZONTAL_ALIGNMENT_LEFT);
		setVerticalAlignment(HSSFTextbox.VERTICAL_ALIGNMENT_TOP);
		setString(new HSSFRichTextString(""));
	}

	@Override
	protected ObjRecord createObjRecord() {
		ObjRecord obj = new ObjRecord();
		CommonObjectDataSubRecord c = new CommonObjectDataSubRecord();
		c.setObjectType(HSSFTextbox.OBJECT_TYPE_TEXT);
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
	protected EscherContainerRecord createSpContainer() {
		EscherContainerRecord spContainer = new EscherContainerRecord();
		EscherSpRecord sp = new EscherSpRecord();
		EscherOptRecord opt = new EscherOptRecord();
		EscherClientDataRecord clientData = new EscherClientDataRecord();
		EscherTextboxRecord escherTextbox = new EscherTextboxRecord();
		spContainer.setRecordId(EscherContainerRecord.SP_CONTAINER);
		spContainer.setOptions(((short) (15)));
		sp.setRecordId(EscherSpRecord.RECORD_ID);
		sp.setOptions(((short) (((EscherAggregate.ST_TEXTBOX) << 4) | 2)));
		sp.setFlags(((EscherSpRecord.FLAG_HAVEANCHOR) | (EscherSpRecord.FLAG_HASSHAPETYPE)));
		opt.setRecordId(EscherOptRecord.RECORD_ID);
		opt.addEscherProperty(new EscherSimpleProperty(EscherProperties.TEXT__TEXTID, 0));
		opt.addEscherProperty(new EscherSimpleProperty(EscherProperties.TEXT__WRAPTEXT, 0));
		opt.addEscherProperty(new EscherSimpleProperty(EscherProperties.TEXT__ANCHORTEXT, 0));
		opt.addEscherProperty(new EscherSimpleProperty(EscherProperties.GROUPSHAPE__PRINT, 524288));
		opt.addEscherProperty(new EscherSimpleProperty(EscherProperties.TEXT__TEXTLEFT, 0));
		opt.addEscherProperty(new EscherSimpleProperty(EscherProperties.TEXT__TEXTRIGHT, 0));
		opt.addEscherProperty(new EscherSimpleProperty(EscherProperties.TEXT__TEXTTOP, 0));
		opt.addEscherProperty(new EscherSimpleProperty(EscherProperties.TEXT__TEXTBOTTOM, 0));
		opt.setEscherProperty(new EscherSimpleProperty(EscherProperties.LINESTYLE__LINEDASHING, HSSFShape.LINESTYLE_SOLID));
		opt.setEscherProperty(new EscherBoolProperty(EscherProperties.LINESTYLE__NOLINEDRAWDASH, 524296));
		opt.setEscherProperty(new EscherSimpleProperty(EscherProperties.LINESTYLE__LINEWIDTH, HSSFShape.LINEWIDTH_DEFAULT));
		opt.setEscherProperty(new EscherRGBProperty(EscherProperties.FILL__FILLCOLOR, HSSFShape.FILL__FILLCOLOR_DEFAULT));
		opt.setEscherProperty(new EscherRGBProperty(EscherProperties.LINESTYLE__COLOR, HSSFShape.LINESTYLE__COLOR_DEFAULT));
		opt.setEscherProperty(new EscherBoolProperty(EscherProperties.FILL__NOFILLHITTEST, HSSFShape.NO_FILLHITTEST_FALSE));
		opt.setEscherProperty(new EscherBoolProperty(EscherProperties.GROUPSHAPE__PRINT, 524288));
		clientData.setRecordId(EscherClientDataRecord.RECORD_ID);
		clientData.setOptions(((short) (0)));
		escherTextbox.setRecordId(EscherTextboxRecord.RECORD_ID);
		escherTextbox.setOptions(((short) (0)));
		spContainer.addChildRecord(sp);
		spContainer.addChildRecord(opt);
		spContainer.addChildRecord(clientData);
		spContainer.addChildRecord(escherTextbox);
		return spContainer;
	}

	void afterInsert(HSSFPatriarch patriarch) {
		EscherAggregate agg = patriarch.getBoundAggregate();
		agg.associateShapeToObjRecord(getEscherContainer().getChildById(EscherClientDataRecord.RECORD_ID), getObjRecord());
		if ((getTextObjectRecord()) != null) {
			agg.associateShapeToObjRecord(getEscherContainer().getChildById(EscherTextboxRecord.RECORD_ID), getTextObjectRecord());
		}
	}

	public int getMarginLeft() {
		EscherSimpleProperty property = getOptRecord().lookup(EscherProperties.TEXT__TEXTLEFT);
		return property == null ? 0 : property.getPropertyValue();
	}

	public void setMarginLeft(int marginLeft) {
		setPropertyValue(new EscherSimpleProperty(EscherProperties.TEXT__TEXTLEFT, marginLeft));
	}

	public int getMarginRight() {
		EscherSimpleProperty property = getOptRecord().lookup(EscherProperties.TEXT__TEXTRIGHT);
		return property == null ? 0 : property.getPropertyValue();
	}

	public void setMarginRight(int marginRight) {
		setPropertyValue(new EscherSimpleProperty(EscherProperties.TEXT__TEXTRIGHT, marginRight));
	}

	public int getMarginTop() {
		EscherSimpleProperty property = getOptRecord().lookup(EscherProperties.TEXT__TEXTTOP);
		return property == null ? 0 : property.getPropertyValue();
	}

	public void setMarginTop(int marginTop) {
		setPropertyValue(new EscherSimpleProperty(EscherProperties.TEXT__TEXTTOP, marginTop));
	}

	public int getMarginBottom() {
		EscherSimpleProperty property = getOptRecord().lookup(EscherProperties.TEXT__TEXTBOTTOM);
		return property == null ? 0 : property.getPropertyValue();
	}

	public void setMarginBottom(int marginBottom) {
		setPropertyValue(new EscherSimpleProperty(EscherProperties.TEXT__TEXTBOTTOM, marginBottom));
	}

	public short getHorizontalAlignment() {
		return ((short) (getTextObjectRecord().getHorizontalTextAlignment()));
	}

	public void setHorizontalAlignment(short align) {
		getTextObjectRecord().setHorizontalTextAlignment(align);
	}

	public short getVerticalAlignment() {
		return ((short) (getTextObjectRecord().getVerticalTextAlignment()));
	}

	public void setVerticalAlignment(short align) {
		getTextObjectRecord().setVerticalTextAlignment(align);
	}

	@Override
	public void setShapeType(int shapeType) {
		throw new IllegalStateException(("Shape type can not be changed in " + (this.getClass().getSimpleName())));
	}

	@Override
	protected HSSFShape cloneShape() {
		TextObjectRecord txo = ((getTextObjectRecord()) == null) ? null : ((TextObjectRecord) (getTextObjectRecord().cloneViaReserialise()));
		EscherContainerRecord spContainer = new EscherContainerRecord();
		byte[] inSp = getEscherContainer().serialize();
		spContainer.fillFields(inSp, 0, new DefaultEscherRecordFactory());
		ObjRecord obj = ((ObjRecord) (getObjRecord().cloneViaReserialise()));
		return new HSSFTextbox(spContainer, obj, txo);
	}

	@Override
	protected void afterRemove(HSSFPatriarch patriarch) {
		patriarch.getBoundAggregate().removeShapeToObjRecord(getEscherContainer().getChildById(EscherClientDataRecord.RECORD_ID));
		patriarch.getBoundAggregate().removeShapeToObjRecord(getEscherContainer().getChildById(EscherTextboxRecord.RECORD_ID));
	}
}

