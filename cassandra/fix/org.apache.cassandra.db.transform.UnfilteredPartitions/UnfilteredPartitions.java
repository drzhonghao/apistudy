

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.partitions.UnfilteredPartitionIterator;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.transform.BasePartitions;


final class UnfilteredPartitions extends BasePartitions<UnfilteredRowIterator, UnfilteredPartitionIterator> implements UnfilteredPartitionIterator {
	final boolean isForThrift;

	public UnfilteredPartitions(UnfilteredPartitionIterator input) {
		super(input);
		this.isForThrift = input.isForThrift();
	}

	public boolean isForThrift() {
		return isForThrift;
	}

	public CFMetaData metadata() {
		return null;
	}
}

