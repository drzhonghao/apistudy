

import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hssf.record.AbstractEscherHolderRecord;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.ContinueRecord;
import org.apache.poi.hssf.record.DBCellRecord;
import org.apache.poi.hssf.record.DrawingGroupRecord;
import org.apache.poi.hssf.record.DrawingRecord;
import org.apache.poi.hssf.record.EOFRecord;
import org.apache.poi.hssf.record.FilePassRecord;
import org.apache.poi.hssf.record.MulRKRecord;
import org.apache.poi.hssf.record.NumberRecord;
import org.apache.poi.hssf.record.ObjRecord;
import org.apache.poi.hssf.record.RKRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.RecordBase;
import org.apache.poi.hssf.record.RecordFactory;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.TextObjectRecord;
import org.apache.poi.hssf.record.UnknownRecord;
import org.apache.poi.hssf.record.WriteProtectRecord;
import org.apache.poi.hssf.record.crypto.Biff8EncryptionKey;
import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.util.RecordFormatException;


public final class RecordFactoryInputStream {
	private static final class StreamEncryptionInfo {
		private final int _initialRecordsSize;

		private final FilePassRecord _filePassRec;

		private final Record _lastRecord;

		private final boolean _hasBOFRecord;

		public StreamEncryptionInfo(RecordInputStream rs, List<Record> outputRecs) {
			Record rec;
			rs.nextRecord();
			int recSize = 4 + (rs.remaining());
			rec = RecordFactory.createSingleRecord(rs);
			outputRecs.add(rec);
			FilePassRecord fpr = null;
			if (rec instanceof BOFRecord) {
				_hasBOFRecord = true;
				if (rs.hasNextRecord()) {
					rs.nextRecord();
					rec = RecordFactory.createSingleRecord(rs);
					recSize += rec.getRecordSize();
					outputRecs.add(rec);
					if ((rec instanceof WriteProtectRecord) && (rs.hasNextRecord())) {
						rs.nextRecord();
						rec = RecordFactory.createSingleRecord(rs);
						recSize += rec.getRecordSize();
						outputRecs.add(rec);
					}
					if (rec instanceof FilePassRecord) {
						fpr = ((FilePassRecord) (rec));
					}
					if (rec instanceof EOFRecord) {
						throw new IllegalStateException("Nothing between BOF and EOF");
					}
				}
			}else {
				_hasBOFRecord = false;
			}
			_initialRecordsSize = recSize;
			_filePassRec = fpr;
			_lastRecord = rec;
		}

		public RecordInputStream createDecryptingStream(InputStream original) {
			String userPassword = Biff8EncryptionKey.getCurrentUserPassword();
			if (userPassword == null) {
				userPassword = Decryptor.DEFAULT_PASSWORD;
			}
			EncryptionInfo info = _filePassRec.getEncryptionInfo();
			try {
				if (!(info.getDecryptor().verifyPassword(userPassword))) {
					throw new EncryptedDocumentException(((Decryptor.DEFAULT_PASSWORD.equals(userPassword) ? "Default" : "Supplied") + " password is invalid for salt/verifier/verifierHash"));
				}
			} catch (GeneralSecurityException e) {
				throw new EncryptedDocumentException(e);
			}
			return new RecordInputStream(original, info, _initialRecordsSize);
		}

		public boolean hasEncryption() {
			return (_filePassRec) != null;
		}

		public Record getLastRecord() {
			return _lastRecord;
		}

		public boolean hasBOFRecord() {
			return _hasBOFRecord;
		}
	}

	private final RecordInputStream _recStream;

	private final boolean _shouldIncludeContinueRecords;

	private Record[] _unreadRecordBuffer;

	private int _unreadRecordIndex = -1;

	private Record _lastRecord;

	private DrawingRecord _lastDrawingRecord = new DrawingRecord();

	private int _bofDepth;

	private boolean _lastRecordWasEOFLevelZero;

	public RecordFactoryInputStream(InputStream in, boolean shouldIncludeContinueRecords) {
		RecordInputStream rs = new RecordInputStream(in);
		List<Record> records = new ArrayList<>();
		RecordFactoryInputStream.StreamEncryptionInfo sei = new RecordFactoryInputStream.StreamEncryptionInfo(rs, records);
		if (sei.hasEncryption()) {
			rs = sei.createDecryptingStream(in);
		}else {
		}
		if (!(records.isEmpty())) {
			_unreadRecordBuffer = new Record[records.size()];
			records.toArray(_unreadRecordBuffer);
			_unreadRecordIndex = 0;
		}
		_recStream = rs;
		_shouldIncludeContinueRecords = shouldIncludeContinueRecords;
		_lastRecord = sei.getLastRecord();
		_bofDepth = (sei.hasBOFRecord()) ? 1 : 0;
		_lastRecordWasEOFLevelZero = false;
	}

	public Record nextRecord() {
		Record r;
		r = getNextUnreadRecord();
		if (r != null) {
			return r;
		}
		while (true) {
			if (!(_recStream.hasNextRecord())) {
				return null;
			}
			if (_lastRecordWasEOFLevelZero) {
				if ((_recStream.getNextSid()) != (BOFRecord.sid)) {
					return null;
				}
			}
			_recStream.nextRecord();
			r = readNextRecord();
			if (r == null) {
				continue;
			}
			return r;
		} 
	}

	private Record getNextUnreadRecord() {
		if ((_unreadRecordBuffer) != null) {
			int ix = _unreadRecordIndex;
			if (ix < (_unreadRecordBuffer.length)) {
				Record result = _unreadRecordBuffer[ix];
				_unreadRecordIndex = ix + 1;
				return result;
			}
			_unreadRecordIndex = -1;
			_unreadRecordBuffer = null;
		}
		return null;
	}

	private Record readNextRecord() {
		Record record = RecordFactory.createSingleRecord(_recStream);
		_lastRecordWasEOFLevelZero = false;
		if (record instanceof BOFRecord) {
			(_bofDepth)++;
			return record;
		}
		if (record instanceof EOFRecord) {
			(_bofDepth)--;
			if ((_bofDepth) < 1) {
				_lastRecordWasEOFLevelZero = true;
			}
			return record;
		}
		if (record instanceof DBCellRecord) {
			return null;
		}
		if (record instanceof RKRecord) {
			return RecordFactory.convertToNumberRecord(((RKRecord) (record)));
		}
		if (record instanceof MulRKRecord) {
			Record[] records = RecordFactory.convertRKRecords(((MulRKRecord) (record)));
			_unreadRecordBuffer = records;
			_unreadRecordIndex = 1;
			return records[0];
		}
		if (((record.getSid()) == (DrawingGroupRecord.sid)) && ((_lastRecord) instanceof DrawingGroupRecord)) {
			DrawingGroupRecord lastDGRecord = ((DrawingGroupRecord) (_lastRecord));
			lastDGRecord.join(((AbstractEscherHolderRecord) (record)));
			return null;
		}
		if ((record.getSid()) == (ContinueRecord.sid)) {
			ContinueRecord contRec = ((ContinueRecord) (record));
			if (((_lastRecord) instanceof ObjRecord) || ((_lastRecord) instanceof TextObjectRecord)) {
				if (_shouldIncludeContinueRecords) {
					return record;
				}
				return null;
			}
			if ((_lastRecord) instanceof DrawingGroupRecord) {
				((DrawingGroupRecord) (_lastRecord)).processContinueRecord(contRec.getData());
				return null;
			}
			if ((_lastRecord) instanceof DrawingRecord) {
				return contRec;
			}
			if ((_lastRecord) instanceof UnknownRecord) {
				return record;
			}
			if ((_lastRecord) instanceof EOFRecord) {
				return record;
			}
			throw new RecordFormatException(("Unhandled Continue Record followining " + (_lastRecord.getClass())));
		}
		_lastRecord = record;
		if (record instanceof DrawingRecord) {
			_lastDrawingRecord = ((DrawingRecord) (record));
		}
		return record;
	}
}

