import org.apache.poi.hssf.record.aggregates.*;


import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.model.RecordStream;
import org.apache.poi.hssf.record.*;

/**
 * Manages the all the records associated with a chart sub-stream.<p>
 * Includes the initial {@link BOFRecord} and final {@link EOFRecord}.
 */
public final class ChartSubstreamRecordAggregate extends RecordAggregate {

	private final BOFRecord _bofRec;
	/**
	 * All the records between BOF and EOF
	 */
	private final List<RecordBase> _recs;
	private PageSettingsBlock _psBlock;

	public ChartSubstreamRecordAggregate(RecordStream rs) {
		_bofRec = (BOFRecord) rs.getNext();
		List<RecordBase> temp = new ArrayList<>();
		while (rs.peekNextClass() != EOFRecord.class) {
			if (PageSettingsBlock.isComponentRecord(rs.peekNextSid())) {
				if (_psBlock != null) {
					if (rs.peekNextSid() == HeaderFooterRecord.sid) {
						// test samples: 45538_classic_Footer.xls, 45538_classic_Header.xls
						_psBlock.addLateHeaderFooter((HeaderFooterRecord)rs.getNext());
						continue;
					}
					throw new IllegalStateException(
							"Found more than one PageSettingsBlock in chart sub-stream, had sid: " + rs.peekNextSid());
				}
				_psBlock = new PageSettingsBlock(rs);
				temp.add(_psBlock);
				continue;
			}
			temp.add(rs.getNext());
		}
		_recs = temp;
		Record eof = rs.getNext(); // no need to save EOF in field
		if (!(eof instanceof EOFRecord)) {
			throw new IllegalStateException("Bad chart EOF");
		}
	}

	public void visitContainedRecords(RecordVisitor rv) {
		if (_recs.isEmpty()) {
			return;
		}
		rv.visitRecord(_bofRec);
		for (int i = 0; i < _recs.size(); i++) {
			RecordBase rb = _recs.get(i);
			if (rb instanceof RecordAggregate) {
				((RecordAggregate) rb).visitContainedRecords(rv);
			} else {
				rv.visitRecord((Record) rb);
			}
		}
		rv.visitRecord(EOFRecord.instance);
	}
}
