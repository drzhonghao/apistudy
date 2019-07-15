

import org.apache.cassandra.db.DeletionPurger;
import org.apache.cassandra.db.rows.BaseRowIterator;
import org.apache.cassandra.db.rows.RangeTombstoneMarker;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.RowIterator;
import org.apache.cassandra.db.rows.Rows;
import org.apache.cassandra.db.transform.Transformation;


public final class Filter extends Transformation {
	private final int nowInSec;

	private final boolean enforceStrictLiveness;

	public Filter(int nowInSec, boolean enforceStrictLiveness) {
		this.nowInSec = nowInSec;
		this.enforceStrictLiveness = enforceStrictLiveness;
	}

	@Override
	protected RowIterator applyToPartition(BaseRowIterator iterator) {
		return null;
	}

	@Override
	protected Row applyToStatic(Row row) {
		if (row.isEmpty())
			return Rows.EMPTY_STATIC_ROW;

		row = row.purge(DeletionPurger.PURGE_ALL, nowInSec, enforceStrictLiveness);
		return row == null ? Rows.EMPTY_STATIC_ROW : row;
	}

	@Override
	protected Row applyToRow(Row row) {
		return row.purge(DeletionPurger.PURGE_ALL, nowInSec, enforceStrictLiveness);
	}

	@Override
	protected RangeTombstoneMarker applyToMarker(RangeTombstoneMarker marker) {
		return null;
	}
}

