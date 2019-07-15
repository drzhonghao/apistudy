

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.PartitionColumns;
import org.apache.cassandra.db.rows.BaseRowIterator;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.Rows;
import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.db.transform.Transformation;
import org.apache.cassandra.utils.Throwables;


public abstract class BaseRows<R extends Unfiltered, I extends BaseRowIterator<? extends Unfiltered>> implements BaseRowIterator<R> {
	private Row staticRow;

	private DecoratedKey partitionKey;

	public BaseRows(I input) {
		staticRow = input.staticRow();
		partitionKey = input.partitionKey();
	}

	BaseRows(BaseRows<?, ? extends I> copyFrom) {
		staticRow = copyFrom.staticRow;
		partitionKey = copyFrom.partitionKey();
	}

	public CFMetaData metadata() {
		return null;
	}

	public boolean isReverseOrder() {
		return false;
	}

	public PartitionColumns columns() {
		return null;
	}

	public DecoratedKey partitionKey() {
		return null;
	}

	public Row staticRow() {
		return (staticRow) == null ? Rows.EMPTY_STATIC_ROW : staticRow;
	}

	protected Throwable runOnClose(int length) {
		Throwable fail = null;
		for (int i = 0; i < length; i++) {
			try {
			} catch (Throwable t) {
				fail = Throwables.merge(fail, t);
			}
		}
		return fail;
	}

	void add(Transformation transformation) {
		if ((staticRow) != null) {
		}
	}

	protected Unfiltered applyOne(Unfiltered value, Transformation transformation) {
		return null;
	}

	@Override
	public final boolean hasNext() {
		return true;
	}
}

