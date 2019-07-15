

import org.apache.poi.ddf.AbstractEscherOptRecord;
import org.apache.poi.ddf.EscherArrayProperty;
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
import org.apache.poi.hssf.record.CommonObjectDataSubRecord;
import org.apache.poi.hssf.record.EndSubRecord;
import org.apache.poi.hssf.record.EscherAggregate;
import org.apache.poi.hssf.record.ObjRecord;
import org.apache.poi.hssf.record.TextObjectRecord;
import org.apache.poi.hssf.usermodel.HSSFAnchor;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFShape;
import org.apache.poi.hssf.usermodel.HSSFSimpleShape;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


public class HSSFPolygon extends HSSFSimpleShape {
	private static POILogger logger = POILogFactory.getLogger(HSSFPolygon.class);

	public static final short OBJECT_TYPE_MICROSOFT_OFFICE_DRAWING = 30;

	public HSSFPolygon(EscherContainerRecord spContainer, ObjRecord objRecord, TextObjectRecord _textObjectRecord) {
		super(spContainer, objRecord, _textObjectRecord);
	}

	public HSSFPolygon(EscherContainerRecord spContainer, ObjRecord objRecord) {
		super(spContainer, objRecord);
	}

	HSSFPolygon(HSSFShape parent, HSSFAnchor anchor) {
		super(parent, anchor);
	}

	@Override
	protected TextObjectRecord createTextObjRecord() {
		return null;
	}

	protected EscherContainerRecord createSpContainer() {
		EscherContainerRecord spContainer = new EscherContainerRecord();
		EscherSpRecord sp = new EscherSpRecord();
		EscherOptRecord opt = new EscherOptRecord();
		EscherClientDataRecord clientData = new EscherClientDataRecord();
		spContainer.setRecordId(EscherContainerRecord.SP_CONTAINER);
		spContainer.setOptions(((short) (15)));
		sp.setRecordId(EscherSpRecord.RECORD_ID);
		sp.setOptions(((short) (((EscherAggregate.ST_NOT_PRIMATIVE) << 4) | 2)));
		if ((getParent()) == null) {
			sp.setFlags(((EscherSpRecord.FLAG_HAVEANCHOR) | (EscherSpRecord.FLAG_HASSHAPETYPE)));
		}else {
			sp.setFlags((((EscherSpRecord.FLAG_CHILD) | (EscherSpRecord.FLAG_HAVEANCHOR)) | (EscherSpRecord.FLAG_HASSHAPETYPE)));
		}
		opt.setRecordId(EscherOptRecord.RECORD_ID);
		opt.setEscherProperty(new EscherSimpleProperty(EscherProperties.TRANSFORM__ROTATION, false, false, 0));
		opt.setEscherProperty(new EscherSimpleProperty(EscherProperties.GEOMETRY__RIGHT, false, false, 100));
		opt.setEscherProperty(new EscherSimpleProperty(EscherProperties.GEOMETRY__BOTTOM, false, false, 100));
		opt.setEscherProperty(new EscherShapePathProperty(EscherProperties.GEOMETRY__SHAPEPATH, EscherShapePathProperty.COMPLEX));
		opt.setEscherProperty(new EscherSimpleProperty(EscherProperties.GEOMETRY__FILLOK, false, false, 65537));
		opt.setEscherProperty(new EscherSimpleProperty(EscherProperties.LINESTYLE__LINESTARTARROWHEAD, false, false, 0));
		opt.setEscherProperty(new EscherSimpleProperty(EscherProperties.LINESTYLE__LINEENDARROWHEAD, false, false, 0));
		opt.setEscherProperty(new EscherSimpleProperty(EscherProperties.LINESTYLE__LINEENDCAPSTYLE, false, false, 0));
		opt.setEscherProperty(new EscherSimpleProperty(EscherProperties.LINESTYLE__LINEDASHING, HSSFShape.LINESTYLE_SOLID));
		opt.setEscherProperty(new EscherBoolProperty(EscherProperties.LINESTYLE__NOLINEDRAWDASH, 524296));
		opt.setEscherProperty(new EscherSimpleProperty(EscherProperties.LINESTYLE__LINEWIDTH, HSSFShape.LINEWIDTH_DEFAULT));
		opt.setEscherProperty(new EscherRGBProperty(EscherProperties.FILL__FILLCOLOR, HSSFShape.FILL__FILLCOLOR_DEFAULT));
		opt.setEscherProperty(new EscherRGBProperty(EscherProperties.LINESTYLE__COLOR, HSSFShape.LINESTYLE__COLOR_DEFAULT));
		opt.setEscherProperty(new EscherBoolProperty(EscherProperties.FILL__NOFILLHITTEST, 1));
		opt.setEscherProperty(new EscherBoolProperty(EscherProperties.GROUPSHAPE__PRINT, 524288));
		clientData.setRecordId(EscherClientDataRecord.RECORD_ID);
		clientData.setOptions(((short) (0)));
		spContainer.addChildRecord(sp);
		spContainer.addChildRecord(opt);
		spContainer.addChildRecord(clientData);
		return spContainer;
	}

	protected ObjRecord createObjRecord() {
		ObjRecord obj = new ObjRecord();
		CommonObjectDataSubRecord c = new CommonObjectDataSubRecord();
		c.setObjectType(HSSFPolygon.OBJECT_TYPE_MICROSOFT_OFFICE_DRAWING);
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
	}

	public int[] getXPoints() {
		EscherArrayProperty verticesProp = getOptRecord().lookup(EscherProperties.GEOMETRY__VERTICES);
		if (null == verticesProp) {
			return new int[]{  };
		}
		int[] array = new int[(verticesProp.getNumberOfElementsInArray()) - 1];
		for (int i = 0; i < ((verticesProp.getNumberOfElementsInArray()) - 1); i++) {
			byte[] property = verticesProp.getElement(i);
			short x = LittleEndian.getShort(property, 0);
			array[i] = x;
		}
		return array;
	}

	public int[] getYPoints() {
		EscherArrayProperty verticesProp = getOptRecord().lookup(EscherProperties.GEOMETRY__VERTICES);
		if (null == verticesProp) {
			return new int[]{  };
		}
		int[] array = new int[(verticesProp.getNumberOfElementsInArray()) - 1];
		for (int i = 0; i < ((verticesProp.getNumberOfElementsInArray()) - 1); i++) {
			byte[] property = verticesProp.getElement(i);
			short x = LittleEndian.getShort(property, 2);
			array[i] = x;
		}
		return array;
	}

	public void setPoints(int[] xPoints, int[] yPoints) {
		if ((xPoints.length) != (yPoints.length)) {
			HSSFPolygon.logger.log(POILogger.ERROR, "xPoint.length must be equal to yPoints.length");
			return;
		}
		if ((xPoints.length) == 0) {
			HSSFPolygon.logger.log(POILogger.ERROR, "HSSFPolygon must have at least one point");
		}
		EscherArrayProperty verticesProp = new EscherArrayProperty(EscherProperties.GEOMETRY__VERTICES, false, new byte[0]);
		verticesProp.setNumberOfElementsInArray(((xPoints.length) + 1));
		verticesProp.setNumberOfElementsInMemory(((xPoints.length) + 1));
		verticesProp.setSizeOfElements(65520);
		for (int i = 0; i < (xPoints.length); i++) {
			byte[] data = new byte[4];
			LittleEndian.putShort(data, 0, ((short) (xPoints[i])));
			LittleEndian.putShort(data, 2, ((short) (yPoints[i])));
			verticesProp.setElement(i, data);
		}
		int point = xPoints.length;
		byte[] data = new byte[4];
		LittleEndian.putShort(data, 0, ((short) (xPoints[0])));
		LittleEndian.putShort(data, 2, ((short) (yPoints[0])));
		verticesProp.setElement(point, data);
		setPropertyValue(verticesProp);
		EscherArrayProperty segmentsProp = new EscherArrayProperty(EscherProperties.GEOMETRY__SEGMENTINFO, false, null);
		segmentsProp.setSizeOfElements(2);
		segmentsProp.setNumberOfElementsInArray((((xPoints.length) * 2) + 4));
		segmentsProp.setNumberOfElementsInMemory((((xPoints.length) * 2) + 4));
		segmentsProp.setElement(0, new byte[]{ ((byte) (0)), ((byte) (64)) });
		segmentsProp.setElement(1, new byte[]{ ((byte) (0)), ((byte) (172)) });
		for (int i = 0; i < (xPoints.length); i++) {
			segmentsProp.setElement((2 + (i * 2)), new byte[]{ ((byte) (1)), ((byte) (0)) });
			segmentsProp.setElement((3 + (i * 2)), new byte[]{ ((byte) (0)), ((byte) (172)) });
		}
		segmentsProp.setElement(((segmentsProp.getNumberOfElementsInArray()) - 2), new byte[]{ ((byte) (1)), ((byte) (96)) });
		segmentsProp.setElement(((segmentsProp.getNumberOfElementsInArray()) - 1), new byte[]{ ((byte) (0)), ((byte) (128)) });
		setPropertyValue(segmentsProp);
	}

	public void setPolygonDrawArea(int width, int height) {
		setPropertyValue(new EscherSimpleProperty(EscherProperties.GEOMETRY__RIGHT, width));
		setPropertyValue(new EscherSimpleProperty(EscherProperties.GEOMETRY__BOTTOM, height));
	}

	public int getDrawAreaWidth() {
		EscherSimpleProperty property = getOptRecord().lookup(EscherProperties.GEOMETRY__RIGHT);
		return property == null ? 100 : property.getPropertyValue();
	}

	public int getDrawAreaHeight() {
		EscherSimpleProperty property = getOptRecord().lookup(EscherProperties.GEOMETRY__BOTTOM);
		return property == null ? 100 : property.getPropertyValue();
	}
}

