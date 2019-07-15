

import java.util.List;
import org.apache.poi.ddf.AbstractEscherOptRecord;
import org.apache.poi.ddf.EscherClientDataRecord;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherOptRecord;
import org.apache.poi.ddf.EscherProperties;
import org.apache.poi.ddf.EscherProperty;
import org.apache.poi.ddf.EscherPropertyFactory;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.ddf.EscherSimpleProperty;
import org.apache.poi.ddf.EscherSpRecord;
import org.apache.poi.hslf.model.MovieShape;
import org.apache.poi.hslf.record.ExObjRefAtom;
import org.apache.poi.hslf.record.HSLFEscherClientDataRecord;
import org.apache.poi.hslf.record.InteractiveInfo;
import org.apache.poi.hslf.record.InteractiveInfoAtom;
import org.apache.poi.hslf.record.Record;
import org.apache.poi.hslf.record.RecordTypes;
import org.apache.poi.hslf.usermodel.HSLFGroupShape;
import org.apache.poi.hslf.usermodel.HSLFObjectShape;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.sl.usermodel.ShapeContainer;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


public final class HSLFShapeFactory {
	protected static final POILogger logger = POILogFactory.getLogger(HSLFShapeFactory.class);

	public static HSLFShape createShape(EscherContainerRecord spContainer, ShapeContainer<HSLFShape, HSLFTextParagraph> parent) {
		if ((spContainer.getRecordId()) == (EscherContainerRecord.SPGR_CONTAINER)) {
			return HSLFShapeFactory.createShapeGroup(spContainer, parent);
		}
		return HSLFShapeFactory.createSimpleShape(spContainer, parent);
	}

	public static HSLFGroupShape createShapeGroup(EscherContainerRecord spContainer, ShapeContainer<HSLFShape, HSLFTextParagraph> parent) {
		boolean isTable = false;
		EscherContainerRecord ecr = ((EscherContainerRecord) (spContainer.getChild(0)));
		EscherRecord opt = HSLFShape.getEscherChild(ecr, RecordTypes.EscherUserDefined);
		if (opt != null) {
			EscherPropertyFactory f = new EscherPropertyFactory();
			List<EscherProperty> props = f.createProperties(opt.serialize(), 8, opt.getInstance());
			for (EscherProperty ep : props) {
				if ((((ep.getPropertyNumber()) == (EscherProperties.GROUPSHAPE__TABLEPROPERTIES)) && (ep instanceof EscherSimpleProperty)) && (((((EscherSimpleProperty) (ep)).getPropertyValue()) & 1) == 1)) {
					isTable = true;
					break;
				}
			}
		}
		HSLFGroupShape group;
		if (isTable) {
		}else {
		}
		group = null;
		return group;
	}

	public static HSLFShape createSimpleShape(EscherContainerRecord spContainer, ShapeContainer<HSLFShape, HSLFTextParagraph> parent) {
		HSLFShape shape = null;
		EscherSpRecord spRecord = spContainer.getChildById(EscherSpRecord.RECORD_ID);
		ShapeType type = ShapeType.forId(spRecord.getShapeType(), false);
		return shape;
	}

	private static HSLFShape createFrame(EscherContainerRecord spContainer, ShapeContainer<HSLFShape, HSLFTextParagraph> parent) {
		InteractiveInfo info = HSLFShapeFactory.getClientDataRecord(spContainer, RecordTypes.InteractiveInfo.typeID);
		if ((info != null) && ((info.getInteractiveInfoAtom()) != null)) {
			switch (info.getInteractiveInfoAtom().getAction()) {
				case InteractiveInfoAtom.ACTION_OLE :
					return new HSLFObjectShape(spContainer, parent);
				case InteractiveInfoAtom.ACTION_MEDIA :
					return new MovieShape(spContainer, parent);
				default :
					break;
			}
		}
		ExObjRefAtom oes = HSLFShapeFactory.getClientDataRecord(spContainer, RecordTypes.ExObjRefAtom.typeID);
		return null;
	}

	private static HSLFShape createNonPrimitive(EscherContainerRecord spContainer, ShapeContainer<HSLFShape, HSLFTextParagraph> parent) {
		AbstractEscherOptRecord opt = HSLFShape.getEscherChild(spContainer, EscherOptRecord.RECORD_ID);
		EscherProperty prop = HSLFShape.getEscherProperty(opt, EscherProperties.GEOMETRY__VERTICES);
		if (prop != null) {
		}
		HSLFShapeFactory.logger.log(POILogger.INFO, "Creating AutoShape for a NotPrimitive shape");
		return null;
	}

	@SuppressWarnings("unchecked")
	protected static <T extends Record> T getClientDataRecord(EscherContainerRecord spContainer, int recordType) {
		HSLFEscherClientDataRecord cldata = spContainer.getChildById(EscherClientDataRecord.RECORD_ID);
		if (cldata != null)
			for (Record r : cldata.getHSLFChildRecords()) {
				if ((r.getRecordType()) == recordType) {
					return ((T) (r));
				}
			}

		return null;
	}
}

