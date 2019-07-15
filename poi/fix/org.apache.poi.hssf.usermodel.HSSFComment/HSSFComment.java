

import java.util.List;
import org.apache.poi.ddf.AbstractEscherOptRecord;
import org.apache.poi.ddf.DefaultEscherRecordFactory;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherOptRecord;
import org.apache.poi.ddf.EscherProperties;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.ddf.EscherSimpleProperty;
import org.apache.poi.hssf.record.CommonObjectDataSubRecord;
import org.apache.poi.hssf.record.EndSubRecord;
import org.apache.poi.hssf.record.EscherAggregate;
import org.apache.poi.hssf.record.NoteRecord;
import org.apache.poi.hssf.record.NoteStructureSubRecord;
import org.apache.poi.hssf.record.ObjRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.SubRecord;
import org.apache.poi.hssf.record.TextObjectRecord;
import org.apache.poi.hssf.usermodel.HSSFAnchor;
import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFShape;
import org.apache.poi.hssf.usermodel.HSSFSimpleShape;
import org.apache.poi.hssf.usermodel.HSSFTextbox;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.util.CellAddress;


public class HSSFComment extends HSSFTextbox implements Comment {
	private static final int FILL_TYPE_SOLID = 0;

	private static final int FILL_TYPE_PICTURE = 3;

	private static final int GROUP_SHAPE_PROPERTY_DEFAULT_VALUE = 655362;

	private static final int GROUP_SHAPE_HIDDEN_MASK = 16777218;

	private static final int GROUP_SHAPE_NOT_HIDDEN_MASK = -16777219;

	private final NoteRecord _note;

	public HSSFComment(EscherContainerRecord spContainer, ObjRecord objRecord, TextObjectRecord textObjectRecord, NoteRecord note) {
		super(spContainer, objRecord, textObjectRecord);
		_note = note;
	}

	public HSSFComment(HSSFShape parent, HSSFAnchor anchor) {
		this(parent, anchor, HSSFComment.createNoteRecord());
	}

	private HSSFComment(HSSFShape parent, HSSFAnchor anchor, NoteRecord note) {
		super(parent, anchor);
		_note = note;
		setFillColor(134217808);
		setVisible(false);
		setAuthor("");
		CommonObjectDataSubRecord cod = ((CommonObjectDataSubRecord) (getObjRecord().getSubRecords().get(0)));
		cod.setObjectType(CommonObjectDataSubRecord.OBJECT_TYPE_COMMENT);
	}

	protected HSSFComment(NoteRecord note, TextObjectRecord txo) {
		this(null, new HSSFClientAnchor(), note);
	}

	void afterInsert(HSSFPatriarch patriarch) {
		patriarch.getBoundAggregate().addTailRecord(getNoteRecord());
	}

	@Override
	protected EscherContainerRecord createSpContainer() {
		EscherContainerRecord spContainer = super.createSpContainer();
		EscherOptRecord opt = spContainer.getChildById(EscherOptRecord.RECORD_ID);
		opt.removeEscherProperty(EscherProperties.TEXT__TEXTLEFT);
		opt.removeEscherProperty(EscherProperties.TEXT__TEXTRIGHT);
		opt.removeEscherProperty(EscherProperties.TEXT__TEXTTOP);
		opt.removeEscherProperty(EscherProperties.TEXT__TEXTBOTTOM);
		opt.setEscherProperty(new EscherSimpleProperty(EscherProperties.GROUPSHAPE__PRINT, false, false, HSSFComment.GROUP_SHAPE_PROPERTY_DEFAULT_VALUE));
		return spContainer;
	}

	@Override
	protected ObjRecord createObjRecord() {
		ObjRecord obj = new ObjRecord();
		CommonObjectDataSubRecord c = new CommonObjectDataSubRecord();
		c.setObjectType(HSSFSimpleShape.OBJECT_TYPE_COMMENT);
		c.setLocked(true);
		c.setPrintable(true);
		c.setAutofill(false);
		c.setAutoline(true);
		NoteStructureSubRecord u = new NoteStructureSubRecord();
		EndSubRecord e = new EndSubRecord();
		obj.addSubRecord(c);
		obj.addSubRecord(u);
		obj.addSubRecord(e);
		return obj;
	}

	private static NoteRecord createNoteRecord() {
		NoteRecord note = new NoteRecord();
		note.setFlags(NoteRecord.NOTE_HIDDEN);
		note.setAuthor("");
		return note;
	}

	void setShapeId(int shapeId) {
		if (shapeId > 65535) {
			throw new IllegalArgumentException((("Cannot add more than " + 65535) + " shapes"));
		}
		CommonObjectDataSubRecord cod = ((CommonObjectDataSubRecord) (getObjRecord().getSubRecords().get(0)));
		cod.setObjectId(shapeId);
		_note.setShapeId(shapeId);
	}

	@Override
	public void setVisible(boolean visible) {
		_note.setFlags((visible ? NoteRecord.NOTE_VISIBLE : NoteRecord.NOTE_HIDDEN));
		setHidden((!visible));
	}

	@Override
	public boolean isVisible() {
		return (_note.getFlags()) == (NoteRecord.NOTE_VISIBLE);
	}

	@Override
	public CellAddress getAddress() {
		return new CellAddress(getRow(), getColumn());
	}

	@Override
	public void setAddress(CellAddress address) {
		setRow(address.getRow());
		setColumn(address.getColumn());
	}

	@Override
	public void setAddress(int row, int col) {
		setRow(row);
		setColumn(col);
	}

	@Override
	public int getRow() {
		return _note.getRow();
	}

	@Override
	public void setRow(int row) {
		_note.setRow(row);
	}

	@Override
	public int getColumn() {
		return _note.getColumn();
	}

	@Override
	public void setColumn(int col) {
		_note.setColumn(col);
	}

	@Override
	public String getAuthor() {
		return _note.getAuthor();
	}

	@Override
	public void setAuthor(String author) {
		if ((_note) != null)
			_note.setAuthor(author);

	}

	protected NoteRecord getNoteRecord() {
		return _note;
	}

	public boolean hasPosition() {
		if ((_note) == null)
			return false;

		if (((getColumn()) < 0) || ((getRow()) < 0))
			return false;

		return true;
	}

	@Override
	public ClientAnchor getClientAnchor() {
		HSSFAnchor ha = super.getAnchor();
		if (ha instanceof ClientAnchor) {
			return ((ClientAnchor) (ha));
		}
		throw new IllegalStateException(("Anchor can not be changed in " + (ClientAnchor.class.getSimpleName())));
	}

	@Override
	public void setShapeType(int shapeType) {
		throw new IllegalStateException(("Shape type can not be changed in " + (this.getClass().getSimpleName())));
	}

	@Override
	public void afterRemove(HSSFPatriarch patriarch) {
		super.afterRemove(patriarch);
		patriarch.getBoundAggregate().removeTailRecord(getNoteRecord());
	}

	@Override
	protected HSSFShape cloneShape() {
		TextObjectRecord txo = ((TextObjectRecord) (getTextObjectRecord().cloneViaReserialise()));
		EscherContainerRecord spContainer = new EscherContainerRecord();
		byte[] inSp = getEscherContainer().serialize();
		spContainer.fillFields(inSp, 0, new DefaultEscherRecordFactory());
		ObjRecord obj = ((ObjRecord) (getObjRecord().cloneViaReserialise()));
		NoteRecord note = ((NoteRecord) (getNoteRecord().cloneViaReserialise()));
		return new HSSFComment(spContainer, obj, txo, note);
	}

	public void setBackgroundImage(int pictureIndex) {
		setPropertyValue(new EscherSimpleProperty(EscherProperties.FILL__PATTERNTEXTURE, false, true, pictureIndex));
		setPropertyValue(new EscherSimpleProperty(EscherProperties.FILL__FILLTYPE, false, false, HSSFComment.FILL_TYPE_PICTURE));
	}

	public void resetBackgroundImage() {
		EscherSimpleProperty property = getOptRecord().lookup(EscherProperties.FILL__PATTERNTEXTURE);
		if (null != property) {
			getOptRecord().removeEscherProperty(EscherProperties.FILL__PATTERNTEXTURE);
		}
		setPropertyValue(new EscherSimpleProperty(EscherProperties.FILL__FILLTYPE, false, false, HSSFComment.FILL_TYPE_SOLID));
	}

	public int getBackgroundImageId() {
		EscherSimpleProperty property = getOptRecord().lookup(EscherProperties.FILL__PATTERNTEXTURE);
		return property == null ? 0 : property.getPropertyValue();
	}

	private void setHidden(boolean value) {
		EscherSimpleProperty property = getOptRecord().lookup(EscherProperties.GROUPSHAPE__PRINT);
		if (value) {
			setPropertyValue(new EscherSimpleProperty(EscherProperties.GROUPSHAPE__PRINT, false, false, ((property.getPropertyValue()) | (HSSFComment.GROUP_SHAPE_HIDDEN_MASK))));
		}else {
			setPropertyValue(new EscherSimpleProperty(EscherProperties.GROUPSHAPE__PRINT, false, false, ((property.getPropertyValue()) & (HSSFComment.GROUP_SHAPE_NOT_HIDDEN_MASK))));
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof HSSFComment)) {
			return false;
		}
		HSSFComment other = ((HSSFComment) (obj));
		return getNoteRecord().equals(other.getNoteRecord());
	}

	@Override
	public int hashCode() {
		return (((getRow()) * 17) + (getColumn())) * 31;
	}
}

