

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.partitions.BasePartitionIterator;
import org.apache.cassandra.db.partitions.PartitionIterator;
import org.apache.cassandra.db.partitions.UnfilteredPartitionIterator;
import org.apache.cassandra.db.rows.RowIterator;
import org.apache.cassandra.db.transform.BasePartitions;
import org.apache.cassandra.db.transform.EmptyPartitionsDiscarder;
import org.apache.cassandra.db.transform.Filter;
import org.apache.cassandra.db.transform.Transformation;


public final class FilteredPartitions extends BasePartitions<RowIterator, BasePartitionIterator<?>> implements PartitionIterator {
	FilteredPartitions(PartitionIterator input) {
		super(input);
	}

	FilteredPartitions(UnfilteredPartitionIterator input, Filter filter) {
		super(input);
	}

	public static FilteredPartitions filter(UnfilteredPartitionIterator iterator, int nowInSecs) {
		FilteredPartitions filtered = FilteredPartitions.filter(iterator, new Filter(nowInSecs, iterator.metadata().enforceStrictLiveness()));
		return iterator.isForThrift() ? filtered : ((FilteredPartitions) (Transformation.apply(filtered, new EmptyPartitionsDiscarder())));
	}

	public static FilteredPartitions filter(UnfilteredPartitionIterator iterator, Filter filter) {
		return null;
	}
}

