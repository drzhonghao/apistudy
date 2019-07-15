

import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.PartitionColumns;
import org.apache.cassandra.db.rows.BaseRowIterator;
import org.apache.cassandra.db.rows.EncodingStats;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.transform.BaseRows;
import org.apache.cassandra.db.transform.Transformation;


final class UnfilteredRows extends BaseRows<Unfiltered, UnfilteredRowIterator> implements UnfilteredRowIterator {
	private PartitionColumns partitionColumns;

	private DeletionTime partitionLevelDeletion;

	public UnfilteredRows(UnfilteredRowIterator input) {
		this(input, input.columns());
	}

	public UnfilteredRows(UnfilteredRowIterator input, PartitionColumns columns) {
		super(input);
		partitionColumns = columns;
		partitionLevelDeletion = input.partitionLevelDeletion();
	}

	void add(Transformation add) {
	}

	@Override
	public PartitionColumns columns() {
		return partitionColumns;
	}

	@Override
	public DeletionTime partitionLevelDeletion() {
		return partitionLevelDeletion;
	}

	public EncodingStats stats() {
		return null;
	}

	@Override
	public boolean isEmpty() {
		return ((staticRow().isEmpty()) && (partitionLevelDeletion().isLive())) && (!(hasNext()));
	}
}

