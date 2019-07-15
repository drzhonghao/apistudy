

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.db.partitions.AbstractBTreePartition;
import org.apache.cassandra.db.partitions.CachedPartition;
import org.apache.cassandra.db.partitions.ImmutableBTreePartition;
import org.apache.cassandra.db.partitions.Partition;
import org.apache.cassandra.db.rows.BaseRowIterator;
import org.apache.cassandra.db.rows.SerializationHelper;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.rows.UnfilteredRowIteratorSerializer;
import org.apache.cassandra.io.ISerializer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.net.MessagingService;

import static org.apache.cassandra.db.rows.SerializationHelper.Flag.LOCAL;


public class CachedBTreePartition extends ImmutableBTreePartition implements CachedPartition {
	private final int createdAtInSec;

	private final int cachedLiveRows;

	private final int rowsWithNonExpiringCells;

	private final int nonTombstoneCellCount;

	private final int nonExpiringLiveCells;

	private CachedBTreePartition(CFMetaData metadata, DecoratedKey partitionKey, AbstractBTreePartition.Holder holder, int createdAtInSec, int cachedLiveRows, int rowsWithNonExpiringCells, int nonTombstoneCellCount, int nonExpiringLiveCells) {
		super(metadata, partitionKey, holder);
		this.createdAtInSec = createdAtInSec;
		this.cachedLiveRows = cachedLiveRows;
		this.rowsWithNonExpiringCells = rowsWithNonExpiringCells;
		this.nonTombstoneCellCount = nonTombstoneCellCount;
		this.nonExpiringLiveCells = nonExpiringLiveCells;
	}

	public static CachedBTreePartition create(UnfilteredRowIterator iterator, int nowInSec) {
		return CachedBTreePartition.create(iterator, 16, nowInSec);
	}

	public static CachedBTreePartition create(UnfilteredRowIterator iterator, int initialRowCapacity, int nowInSec) {
		AbstractBTreePartition.Holder holder = ImmutableBTreePartition.build(iterator, initialRowCapacity);
		int cachedLiveRows = 0;
		int rowsWithNonExpiringCells = 0;
		int nonTombstoneCellCount = 0;
		int nonExpiringLiveCells = 0;
		boolean enforceStrictLiveness = iterator.metadata().enforceStrictLiveness();
		return new CachedBTreePartition(iterator.metadata(), iterator.partitionKey(), holder, nowInSec, cachedLiveRows, rowsWithNonExpiringCells, nonTombstoneCellCount, nonExpiringLiveCells);
	}

	public int cachedLiveRows() {
		return cachedLiveRows;
	}

	public int rowsWithNonExpiringCells() {
		return rowsWithNonExpiringCells;
	}

	public int nonTombstoneCellCount() {
		return nonTombstoneCellCount;
	}

	public int nonExpiringLiveCells() {
		return nonExpiringLiveCells;
	}

	static class Serializer implements ISerializer<CachedPartition> {
		public void serialize(CachedPartition partition, DataOutputPlus out) throws IOException {
			int version = MessagingService.current_version;
			assert partition instanceof CachedBTreePartition;
			CachedBTreePartition p = ((CachedBTreePartition) (partition));
			out.writeInt(p.createdAtInSec);
			out.writeInt(p.cachedLiveRows);
			out.writeInt(p.rowsWithNonExpiringCells);
			out.writeInt(p.nonTombstoneCellCount);
			out.writeInt(p.nonExpiringLiveCells);
			CFMetaData.serializer.serialize(partition.metadata(), out, version);
			try (UnfilteredRowIterator iter = p.unfilteredIterator()) {
				UnfilteredRowIteratorSerializer.serializer.serialize(iter, null, out, version, p.rowCount());
			}
		}

		public CachedPartition deserialize(DataInputPlus in) throws IOException {
			int version = MessagingService.current_version;
			int createdAtInSec = in.readInt();
			int cachedLiveRows = in.readInt();
			int rowsWithNonExpiringCells = in.readInt();
			int nonTombstoneCellCount = in.readInt();
			int nonExpiringLiveCells = in.readInt();
			CFMetaData metadata = CFMetaData.serializer.deserialize(in, version);
			UnfilteredRowIteratorSerializer.Header header = UnfilteredRowIteratorSerializer.serializer.deserializeHeader(metadata, null, in, version, LOCAL);
			assert (!(header.isReversed)) && ((header.rowEstimate) >= 0);
			AbstractBTreePartition.Holder holder;
			try (UnfilteredRowIterator partition = UnfilteredRowIteratorSerializer.serializer.deserialize(in, version, metadata, LOCAL, header)) {
				holder = ImmutableBTreePartition.build(partition, header.rowEstimate);
			}
			return new CachedBTreePartition(metadata, header.key, holder, createdAtInSec, cachedLiveRows, rowsWithNonExpiringCells, nonTombstoneCellCount, nonExpiringLiveCells);
		}

		public long serializedSize(CachedPartition partition) {
			int version = MessagingService.current_version;
			assert partition instanceof CachedBTreePartition;
			CachedBTreePartition p = ((CachedBTreePartition) (partition));
			try (UnfilteredRowIterator iter = p.unfilteredIterator()) {
				return ((((((TypeSizes.sizeof(p.createdAtInSec)) + (TypeSizes.sizeof(p.cachedLiveRows))) + (TypeSizes.sizeof(p.rowsWithNonExpiringCells))) + (TypeSizes.sizeof(p.nonTombstoneCellCount))) + (TypeSizes.sizeof(p.nonExpiringLiveCells))) + (CFMetaData.serializer.serializedSize(partition.metadata(), version))) + (UnfilteredRowIteratorSerializer.serializer.serializedSize(iter, null, MessagingService.current_version, p.rowCount()));
			}
		}
	}
}

