

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.ddf.DefaultEscherRecordFactory;
import org.apache.poi.ddf.EscherClientDataRecord;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.hssf.record.EmbeddedObjectRefSubRecord;
import org.apache.poi.hssf.record.EscherAggregate;
import org.apache.poi.hssf.record.ObjRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.SubRecord;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFPicture;
import org.apache.poi.hssf.usermodel.HSSFShape;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.ss.usermodel.ObjectData;
import org.apache.poi.util.HexDump;


public final class HSSFObjectData extends HSSFPicture implements ObjectData {
	private final DirectoryEntry _root;

	public HSSFObjectData(EscherContainerRecord spContainer, ObjRecord objRecord, DirectoryEntry _root) {
		super(spContainer, objRecord);
		this._root = _root;
	}

	@Override
	public String getOLE2ClassName() {
		return findObjectRecord().getOLEClassName();
	}

	@Override
	public DirectoryEntry getDirectory() throws IOException {
		EmbeddedObjectRefSubRecord subRecord = findObjectRecord();
		int streamId = subRecord.getStreamId().intValue();
		String streamName = "MBD" + (HexDump.toHex(streamId));
		Entry entry = _root.getEntry(streamName);
		if (entry instanceof DirectoryEntry) {
			return ((DirectoryEntry) (entry));
		}
		throw new IOException((("Stream " + streamName) + " was not an OLE2 directory"));
	}

	@Override
	public byte[] getObjectData() {
		return findObjectRecord().getObjectData();
	}

	@Override
	public boolean hasDirectoryEntry() {
		EmbeddedObjectRefSubRecord subRecord = findObjectRecord();
		Integer streamId = subRecord.getStreamId();
		return (streamId != null) && ((streamId.intValue()) != 0);
	}

	protected EmbeddedObjectRefSubRecord findObjectRecord() {
		Iterator<SubRecord> subRecordIter = getObjRecord().getSubRecords().iterator();
		while (subRecordIter.hasNext()) {
			Object subRecord = subRecordIter.next();
			if (subRecord instanceof EmbeddedObjectRefSubRecord) {
				return ((EmbeddedObjectRefSubRecord) (subRecord));
			}
		} 
		throw new IllegalStateException("Object data does not contain a reference to an embedded object OLE2 directory");
	}

	@Override
	protected EscherContainerRecord createSpContainer() {
		throw new IllegalStateException("HSSFObjectData cannot be created from scratch");
	}

	@Override
	protected ObjRecord createObjRecord() {
		throw new IllegalStateException("HSSFObjectData cannot be created from scratch");
	}

	@Override
	protected void afterRemove(HSSFPatriarch patriarch) {
		throw new IllegalStateException("HSSFObjectData cannot be created from scratch");
	}

	void afterInsert(HSSFPatriarch patriarch) {
		EscherAggregate agg = patriarch.getBoundAggregate();
		agg.associateShapeToObjRecord(getEscherContainer().getChildById(EscherClientDataRecord.RECORD_ID), getObjRecord());
	}

	@Override
	protected HSSFShape cloneShape() {
		EscherContainerRecord spContainer = new EscherContainerRecord();
		byte[] inSp = getEscherContainer().serialize();
		spContainer.fillFields(inSp, 0, new DefaultEscherRecordFactory());
		ObjRecord obj = ((ObjRecord) (getObjRecord().cloneViaReserialise()));
		return new HSSFObjectData(spContainer, obj, _root);
	}
}

