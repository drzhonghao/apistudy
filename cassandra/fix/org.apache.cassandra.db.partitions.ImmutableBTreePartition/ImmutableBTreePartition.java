

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.DeletionInfo;
import org.apache.cassandra.db.PartitionColumns;
import org.apache.cassandra.db.partitions.AbstractBTreePartition;
import org.apache.cassandra.db.rows.BaseRowIterator;
import org.apache.cassandra.db.rows.EncodingStats;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;


public class ImmutableBTreePartition extends AbstractBTreePartition {
	protected final AbstractBTreePartition.Holder holder;

	public ImmutableBTreePartition(CFMetaData metadata, DecoratedKey partitionKey, PartitionColumns columns, Row staticRow, Object[] tree, DeletionInfo deletionInfo, EncodingStats stats) {
		super(metadata, partitionKey);
		holder = null;
	}

	protected ImmutableBTreePartition(CFMetaData metadata, DecoratedKey partitionKey, AbstractBTreePartition.Holder holder) {
		super(metadata, partitionKey);
		this.holder = holder;
	}

	public static ImmutableBTreePartition create(UnfilteredRowIterator iterator) {
		return ImmutableBTreePartition.create(iterator, 16);
	}

	public static ImmutableBTreePartition create(UnfilteredRowIterator iterator, boolean ordered) {
		return ImmutableBTreePartition.create(iterator, 16, ordered);
	}

	public static ImmutableBTreePartition create(UnfilteredRowIterator iterator, int initialRowCapacity) {
		return ImmutableBTreePartition.create(iterator, initialRowCapacity, true);
	}

	public static ImmutableBTreePartition create(UnfilteredRowIterator iterator, int initialRowCapacity, boolean ordered) {
		return new ImmutableBTreePartition(iterator.metadata(), iterator.partitionKey(), AbstractBTreePartition.build(iterator, initialRowCapacity, ordered));
	}

	protected AbstractBTreePartition.Holder holder() {
		return holder;
	}

	protected boolean canHaveShadowedData() {
		return false;
	}
}

