

import java.util.ArrayList;
import java.util.List;
import org.apache.poi.hssf.record.CommonObjectDataSubRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.SubRecord;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.LittleEndianByteArrayInputStream;
import org.apache.poi.util.LittleEndianByteArrayOutputStream;
import org.apache.poi.util.RecordFormatException;


public final class ObjRecord extends Record implements Cloneable {
	public static final short sid = 93;

	private static final int NORMAL_PAD_ALIGNMENT = 2;

	private static int MAX_PAD_ALIGNMENT = 4;

	private List<SubRecord> subrecords;

	private final byte[] _uninterpretedData;

	private boolean _isPaddedToQuadByteMultiple;

	public ObjRecord() {
		subrecords = new ArrayList<>(2);
		_uninterpretedData = null;
	}

	public ObjRecord(RecordInputStream in) {
		byte[] subRecordData = in.readRemainder();
		if ((LittleEndian.getUShort(subRecordData, 0)) != (CommonObjectDataSubRecord.sid)) {
			_uninterpretedData = subRecordData;
			subrecords = null;
			return;
		}
		subrecords = new ArrayList<>();
		LittleEndianByteArrayInputStream subRecStream = new LittleEndianByteArrayInputStream(subRecordData);
		CommonObjectDataSubRecord cmo = ((CommonObjectDataSubRecord) (SubRecord.createSubRecord(subRecStream, 0)));
		subrecords.add(cmo);
		while (true) {
			SubRecord subRecord = SubRecord.createSubRecord(subRecStream, cmo.getObjectType());
			subrecords.add(subRecord);
			if (subRecord.isTerminating()) {
				break;
			}
		} 
		final int nRemainingBytes = (subRecordData.length) - (subRecStream.getReadIndex());
		if (nRemainingBytes > 0) {
			_isPaddedToQuadByteMultiple = ((subRecordData.length) % (ObjRecord.MAX_PAD_ALIGNMENT)) == 0;
			if (nRemainingBytes >= (_isPaddedToQuadByteMultiple ? ObjRecord.MAX_PAD_ALIGNMENT : ObjRecord.NORMAL_PAD_ALIGNMENT)) {
				if (!(ObjRecord.canPaddingBeDiscarded(subRecordData, nRemainingBytes))) {
					String msg = (("Leftover " + nRemainingBytes) + " bytes in subrecord data ") + (HexDump.toHex(subRecordData));
					throw new RecordFormatException(msg);
				}
				_isPaddedToQuadByteMultiple = false;
			}
		}else {
			_isPaddedToQuadByteMultiple = false;
		}
		_uninterpretedData = null;
	}

	private static boolean canPaddingBeDiscarded(byte[] data, int nRemainingBytes) {
		for (int i = (data.length) - nRemainingBytes; i < (data.length); i++) {
			if ((data[i]) != 0) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("[OBJ]\n");
		if ((subrecords) != null) {
			for (final SubRecord record : subrecords) {
				sb.append("SUBRECORD: ").append(record);
			}
		}
		sb.append("[/OBJ]\n");
		return sb.toString();
	}

	@Override
	public int getRecordSize() {
		if ((_uninterpretedData) != null) {
			return (_uninterpretedData.length) + 4;
		}
		int size = 0;
		for (SubRecord record : subrecords) {
		}
		if (_isPaddedToQuadByteMultiple) {
			while ((size % (ObjRecord.MAX_PAD_ALIGNMENT)) != 0) {
				size++;
			} 
		}else {
			while ((size % (ObjRecord.NORMAL_PAD_ALIGNMENT)) != 0) {
				size++;
			} 
		}
		return size + 4;
	}

	@Override
	public int serialize(int offset, byte[] data) {
		int recSize = getRecordSize();
		int dataSize = recSize - 4;
		LittleEndianByteArrayOutputStream out = new LittleEndianByteArrayOutputStream(data, offset, recSize);
		out.writeShort(ObjRecord.sid);
		out.writeShort(dataSize);
		if ((_uninterpretedData) == null) {
			for (int i = 0; i < (subrecords.size()); i++) {
				SubRecord record = subrecords.get(i);
				record.serialize(out);
			}
			int expectedEndIx = offset + dataSize;
			while ((out.getWriteIndex()) < expectedEndIx) {
				out.writeByte(0);
			} 
		}else {
			out.write(_uninterpretedData);
		}
		return recSize;
	}

	@Override
	public short getSid() {
		return ObjRecord.sid;
	}

	public List<SubRecord> getSubRecords() {
		return subrecords;
	}

	public void clearSubRecords() {
		subrecords.clear();
	}

	public void addSubRecord(int index, SubRecord element) {
		subrecords.add(index, element);
	}

	public boolean addSubRecord(SubRecord o) {
		return subrecords.add(o);
	}

	@Override
	public ObjRecord clone() {
		ObjRecord rec = new ObjRecord();
		for (SubRecord record : subrecords) {
			rec.addSubRecord(record.clone());
		}
		return rec;
	}
}

