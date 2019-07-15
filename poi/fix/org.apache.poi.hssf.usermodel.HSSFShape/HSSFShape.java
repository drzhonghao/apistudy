

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ddf.AbstractEscherOptRecord;
import org.apache.poi.ddf.EscherBoolProperty;
import org.apache.poi.ddf.EscherChildAnchorRecord;
import org.apache.poi.ddf.EscherClientAnchorRecord;
import org.apache.poi.ddf.EscherComplexProperty;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherOptRecord;
import org.apache.poi.ddf.EscherProperties;
import org.apache.poi.ddf.EscherProperty;
import org.apache.poi.ddf.EscherRGBProperty;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.ddf.EscherSimpleProperty;
import org.apache.poi.ddf.EscherSpRecord;
import org.apache.poi.hssf.record.CommonObjectDataSubRecord;
import org.apache.poi.hssf.record.ObjRecord;
import org.apache.poi.hssf.record.SubRecord;
import org.apache.poi.hssf.usermodel.HSSFAnchor;
import org.apache.poi.hssf.usermodel.HSSFChildAnchor;
import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.ss.usermodel.Shape;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.StringUtil;


public abstract class HSSFShape implements Shape {
	private static final POILogger LOG = POILogFactory.getLogger(HSSFShape.class);

	public static final int LINEWIDTH_ONE_PT = 12700;

	public static final int LINEWIDTH_DEFAULT = 9525;

	public static final int LINESTYLE__COLOR_DEFAULT = 134217792;

	public static final int FILL__FILLCOLOR_DEFAULT = 134217737;

	public static final boolean NO_FILL_DEFAULT = true;

	public static final int LINESTYLE_SOLID = 0;

	public static final int LINESTYLE_DASHSYS = 1;

	public static final int LINESTYLE_DOTSYS = 2;

	public static final int LINESTYLE_DASHDOTSYS = 3;

	public static final int LINESTYLE_DASHDOTDOTSYS = 4;

	public static final int LINESTYLE_DOTGEL = 5;

	public static final int LINESTYLE_DASHGEL = 6;

	public static final int LINESTYLE_LONGDASHGEL = 7;

	public static final int LINESTYLE_DASHDOTGEL = 8;

	public static final int LINESTYLE_LONGDASHDOTGEL = 9;

	public static final int LINESTYLE_LONGDASHDOTDOTGEL = 10;

	public static final int LINESTYLE_NONE = -1;

	public static final int LINESTYLE_DEFAULT = HSSFShape.LINESTYLE_NONE;

	private HSSFShape parent;

	HSSFAnchor anchor;

	private HSSFPatriarch _patriarch;

	private final EscherContainerRecord _escherContainer;

	private final ObjRecord _objRecord;

	private final EscherOptRecord _optRecord;

	public static final int NO_FILLHITTEST_TRUE = 1114112;

	public static final int NO_FILLHITTEST_FALSE = 65536;

	public HSSFShape(EscherContainerRecord spContainer, ObjRecord objRecord) {
		this._escherContainer = spContainer;
		this._objRecord = objRecord;
		this._optRecord = spContainer.getChildById(EscherOptRecord.RECORD_ID);
		this.anchor = HSSFAnchor.createAnchorFromEscher(spContainer);
	}

	public HSSFShape(HSSFShape parent, HSSFAnchor anchor) {
		this.parent = parent;
		this.anchor = anchor;
		this._escherContainer = createSpContainer();
		_optRecord = _escherContainer.getChildById(EscherOptRecord.RECORD_ID);
		_objRecord = createObjRecord();
	}

	protected abstract EscherContainerRecord createSpContainer();

	protected abstract ObjRecord createObjRecord();

	protected abstract void afterRemove(HSSFPatriarch patriarch);

	void setShapeId(int shapeId) {
		EscherSpRecord spRecord = _escherContainer.getChildById(EscherSpRecord.RECORD_ID);
		spRecord.setShapeId(shapeId);
		CommonObjectDataSubRecord cod = ((CommonObjectDataSubRecord) (_objRecord.getSubRecords().get(0)));
		cod.setObjectId(((short) (shapeId % 1024)));
	}

	int getShapeId() {
		return ((EscherSpRecord) (_escherContainer.getChildById(EscherSpRecord.RECORD_ID))).getShapeId();
	}

	abstract void afterInsert(HSSFPatriarch patriarch);

	protected EscherContainerRecord getEscherContainer() {
		return _escherContainer;
	}

	protected ObjRecord getObjRecord() {
		return _objRecord;
	}

	public EscherOptRecord getOptRecord() {
		return _optRecord;
	}

	@Override
	public HSSFShape getParent() {
		return parent;
	}

	@Override
	public HSSFAnchor getAnchor() {
		return anchor;
	}

	public void setAnchor(HSSFAnchor anchor) {
		int i = 0;
		int recordId = -1;
		if ((parent) == null) {
			if (anchor instanceof HSSFChildAnchor)
				throw new IllegalArgumentException("Must use client anchors for shapes directly attached to sheet.");

			EscherClientAnchorRecord anch = _escherContainer.getChildById(EscherClientAnchorRecord.RECORD_ID);
			if (null != anch) {
				for (i = 0; i < (_escherContainer.getChildRecords().size()); i++) {
					if ((_escherContainer.getChild(i).getRecordId()) == (EscherClientAnchorRecord.RECORD_ID)) {
						if (i != ((_escherContainer.getChildRecords().size()) - 1)) {
							recordId = _escherContainer.getChild((i + 1)).getRecordId();
						}
					}
				}
				_escherContainer.removeChildRecord(anch);
			}
		}else {
			if (anchor instanceof HSSFClientAnchor)
				throw new IllegalArgumentException("Must use child anchors for shapes attached to groups.");

			EscherChildAnchorRecord anch = _escherContainer.getChildById(EscherChildAnchorRecord.RECORD_ID);
			if (null != anch) {
				for (i = 0; i < (_escherContainer.getChildRecords().size()); i++) {
					if ((_escherContainer.getChild(i).getRecordId()) == (EscherChildAnchorRecord.RECORD_ID)) {
						if (i != ((_escherContainer.getChildRecords().size()) - 1)) {
							recordId = _escherContainer.getChild((i + 1)).getRecordId();
						}
					}
				}
				_escherContainer.removeChildRecord(anch);
			}
		}
		if ((-1) == recordId) {
		}else {
		}
		this.anchor = anchor;
	}

	public int getLineStyleColor() {
		EscherRGBProperty rgbProperty = _optRecord.lookup(EscherProperties.LINESTYLE__COLOR);
		return rgbProperty == null ? HSSFShape.LINESTYLE__COLOR_DEFAULT : rgbProperty.getRgbColor();
	}

	public void setLineStyleColor(int lineStyleColor) {
		setPropertyValue(new EscherRGBProperty(EscherProperties.LINESTYLE__COLOR, lineStyleColor));
	}

	@Override
	public void setLineStyleColor(int red, int green, int blue) {
		int lineStyleColor = ((blue << 16) | (green << 8)) | red;
		setPropertyValue(new EscherRGBProperty(EscherProperties.LINESTYLE__COLOR, lineStyleColor));
	}

	public int getFillColor() {
		EscherRGBProperty rgbProperty = _optRecord.lookup(EscherProperties.FILL__FILLCOLOR);
		return rgbProperty == null ? HSSFShape.FILL__FILLCOLOR_DEFAULT : rgbProperty.getRgbColor();
	}

	public void setFillColor(int fillColor) {
		setPropertyValue(new EscherRGBProperty(EscherProperties.FILL__FILLCOLOR, fillColor));
	}

	@Override
	public void setFillColor(int red, int green, int blue) {
		int fillColor = ((blue << 16) | (green << 8)) | red;
		setPropertyValue(new EscherRGBProperty(EscherProperties.FILL__FILLCOLOR, fillColor));
	}

	public int getLineWidth() {
		EscherSimpleProperty property = _optRecord.lookup(EscherProperties.LINESTYLE__LINEWIDTH);
		return property == null ? HSSFShape.LINEWIDTH_DEFAULT : property.getPropertyValue();
	}

	public void setLineWidth(int lineWidth) {
		setPropertyValue(new EscherSimpleProperty(EscherProperties.LINESTYLE__LINEWIDTH, lineWidth));
	}

	public int getLineStyle() {
		EscherSimpleProperty property = _optRecord.lookup(EscherProperties.LINESTYLE__LINEDASHING);
		if (null == property) {
			return HSSFShape.LINESTYLE_DEFAULT;
		}
		return property.getPropertyValue();
	}

	public void setLineStyle(int lineStyle) {
		setPropertyValue(new EscherSimpleProperty(EscherProperties.LINESTYLE__LINEDASHING, lineStyle));
		if ((getLineStyle()) != (HSSFShape.LINESTYLE_SOLID)) {
			setPropertyValue(new EscherSimpleProperty(EscherProperties.LINESTYLE__LINEENDCAPSTYLE, 0));
			if ((getLineStyle()) == (HSSFShape.LINESTYLE_NONE)) {
				setPropertyValue(new EscherBoolProperty(EscherProperties.LINESTYLE__NOLINEDRAWDASH, 524288));
			}else {
				setPropertyValue(new EscherBoolProperty(EscherProperties.LINESTYLE__NOLINEDRAWDASH, 524296));
			}
		}
	}

	@Override
	public boolean isNoFill() {
		EscherBoolProperty property = _optRecord.lookup(EscherProperties.FILL__NOFILLHITTEST);
		return property == null ? HSSFShape.NO_FILL_DEFAULT : (property.getPropertyValue()) == (HSSFShape.NO_FILLHITTEST_TRUE);
	}

	@Override
	public void setNoFill(boolean noFill) {
		setPropertyValue(new EscherBoolProperty(EscherProperties.FILL__NOFILLHITTEST, (noFill ? HSSFShape.NO_FILLHITTEST_TRUE : HSSFShape.NO_FILLHITTEST_FALSE)));
	}

	protected void setPropertyValue(EscherProperty property) {
		_optRecord.setEscherProperty(property);
	}

	public void setFlipVertical(boolean value) {
		EscherSpRecord sp = getEscherContainer().getChildById(EscherSpRecord.RECORD_ID);
		if (value) {
			sp.setFlags(((sp.getFlags()) | (EscherSpRecord.FLAG_FLIPVERT)));
		}else {
			sp.setFlags(((sp.getFlags()) & ((Integer.MAX_VALUE) - (EscherSpRecord.FLAG_FLIPVERT))));
		}
	}

	public void setFlipHorizontal(boolean value) {
		EscherSpRecord sp = getEscherContainer().getChildById(EscherSpRecord.RECORD_ID);
		if (value) {
			sp.setFlags(((sp.getFlags()) | (EscherSpRecord.FLAG_FLIPHORIZ)));
		}else {
			sp.setFlags(((sp.getFlags()) & ((Integer.MAX_VALUE) - (EscherSpRecord.FLAG_FLIPHORIZ))));
		}
	}

	public boolean isFlipVertical() {
		EscherSpRecord sp = getEscherContainer().getChildById(EscherSpRecord.RECORD_ID);
		return ((sp.getFlags()) & (EscherSpRecord.FLAG_FLIPVERT)) != 0;
	}

	public boolean isFlipHorizontal() {
		EscherSpRecord sp = getEscherContainer().getChildById(EscherSpRecord.RECORD_ID);
		return ((sp.getFlags()) & (EscherSpRecord.FLAG_FLIPHORIZ)) != 0;
	}

	public int getRotationDegree() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		EscherSimpleProperty property = getOptRecord().lookup(EscherProperties.TRANSFORM__ROTATION);
		if (null == property) {
			return 0;
		}
		try {
			LittleEndian.putInt(property.getPropertyValue(), bos);
			return LittleEndian.getShort(bos.toByteArray(), 2);
		} catch (IOException e) {
			HSSFShape.LOG.log(POILogger.ERROR, "can't determine rotation degree", e);
			return 0;
		}
	}

	public void setRotationDegree(short value) {
		setPropertyValue(new EscherSimpleProperty(EscherProperties.TRANSFORM__ROTATION, (value << 16)));
	}

	public int countOfAllChildren() {
		return 1;
	}

	protected abstract HSSFShape cloneShape();

	protected void setPatriarch(HSSFPatriarch _patriarch) {
		this._patriarch = _patriarch;
	}

	public HSSFPatriarch getPatriarch() {
		return _patriarch;
	}

	protected void setParent(HSSFShape parent) {
		this.parent = parent;
	}

	public String getShapeName() {
		EscherOptRecord eor = getOptRecord();
		if (eor == null) {
			return null;
		}
		EscherProperty ep = eor.lookup(EscherProperties.GROUPSHAPE__SHAPENAME);
		if (ep instanceof EscherComplexProperty) {
			return StringUtil.getFromUnicodeLE(((EscherComplexProperty) (ep)).getComplexData());
		}
		return null;
	}
}

