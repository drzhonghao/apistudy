

import org.apache.cassandra.db.rows.BaseRowIterator;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.RowIterator;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.transform.BaseRows;
import org.apache.cassandra.db.transform.Filter;


public final class FilteredRows extends BaseRows<Row, BaseRowIterator<?>> implements RowIterator {
	FilteredRows(RowIterator input) {
		super(input);
	}

	FilteredRows(UnfilteredRowIterator input, Filter filter) {
		super(input);
	}

	@Override
	public boolean isEmpty() {
		return (staticRow().isEmpty()) && (!(hasNext()));
	}

	public static RowIterator filter(UnfilteredRowIterator iterator, int nowInSecs) {
		return null;
	}
}

