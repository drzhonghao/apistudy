

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Columns;
import org.apache.cassandra.db.ConsistencyLevel;
import org.apache.cassandra.db.DataRange;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.EmptyIterators;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.Memtable;
import org.apache.cassandra.db.PartitionColumns;
import org.apache.cassandra.db.PartitionPosition;
import org.apache.cassandra.db.ReadCommand;
import org.apache.cassandra.db.ReadExecutionController;
import org.apache.cassandra.db.filter.ClusteringIndexFilter;
import org.apache.cassandra.db.filter.ColumnFilter;
import org.apache.cassandra.db.filter.DataLimits;
import org.apache.cassandra.db.filter.RowFilter;
import org.apache.cassandra.db.lifecycle.View;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.partitions.CachedPartition;
import org.apache.cassandra.db.partitions.PartitionIterator;
import org.apache.cassandra.db.partitions.UnfilteredPartitionIterator;
import org.apache.cassandra.db.partitions.UnfilteredPartitionIterators;
import org.apache.cassandra.db.rows.BaseRowIterator;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.transform.Transformation;
import org.apache.cassandra.dht.AbstractBounds;
import org.apache.cassandra.exceptions.RequestExecutionException;
import org.apache.cassandra.index.Index;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.sstable.ISSTableScanner;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.sstable.format.SSTableReadsListener;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.metrics.LatencyMetrics;
import org.apache.cassandra.metrics.TableMetrics;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.schema.IndexMetadata;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.service.pager.PagingState;
import org.apache.cassandra.service.pager.QueryPager;
import org.apache.cassandra.thrift.ThriftResultsMerger;
import org.apache.cassandra.tracing.Tracing;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.utils.CloseableIterator;
import org.apache.cassandra.utils.FBUtilities;

import static org.apache.cassandra.net.MessagingService.Verb.PAGED_RANGE;
import static org.apache.cassandra.net.MessagingService.Verb.RANGE_SLICE;


public class PartitionRangeReadCommand extends ReadCommand {
	protected static final ReadCommand.SelectionDeserializer selectionDeserializer = new PartitionRangeReadCommand.Deserializer();

	private final DataRange dataRange;

	private int oldestUnrepairedTombstone = Integer.MAX_VALUE;

	public static PartitionRangeReadCommand create(boolean isForThrift, CFMetaData metadata, int nowInSec, ColumnFilter columnFilter, RowFilter rowFilter, DataLimits limits, DataRange dataRange) {
		return null;
	}

	public static PartitionRangeReadCommand allDataRead(CFMetaData metadata, int nowInSec) {
		return null;
	}

	public DataRange dataRange() {
		return dataRange;
	}

	public ClusteringIndexFilter clusteringIndexFilter(DecoratedKey key) {
		return dataRange.clusteringIndexFilter(key);
	}

	public boolean isNamesQuery() {
		return dataRange.isNamesQuery();
	}

	public PartitionRangeReadCommand forSubRange(AbstractBounds<PartitionPosition> range, boolean isRangeContinuation) {
		return null;
	}

	public PartitionRangeReadCommand copy() {
		return null;
	}

	public PartitionRangeReadCommand copyAsDigestQuery() {
		return null;
	}

	public ReadCommand withUpdatedLimit(DataLimits newLimits) {
		return null;
	}

	public PartitionRangeReadCommand withUpdatedDataRange(DataRange newDataRange) {
		return null;
	}

	public PartitionRangeReadCommand withUpdatedLimitsAndDataRange(DataLimits newLimits, DataRange newDataRange) {
		return null;
	}

	public long getTimeout() {
		return DatabaseDescriptor.getRangeRpcTimeout();
	}

	public boolean selectsKey(DecoratedKey key) {
		if (!(dataRange().contains(key)))
			return false;

		return rowFilter().partitionKeyRestrictionsAreSatisfiedBy(key, metadata().getKeyValidator());
	}

	public boolean selectsClustering(DecoratedKey key, Clustering clustering) {
		if (clustering == (Clustering.STATIC_CLUSTERING))
			return !(columnFilter().fetchedColumns().statics.isEmpty());

		if (!(dataRange().clusteringIndexFilter(key).selects(clustering)))
			return false;

		return rowFilter().clusteringKeyRestrictionsAreSatisfiedBy(clustering);
	}

	public PartitionIterator execute(ConsistencyLevel consistency, ClientState clientState, long queryStartNanoTime) throws RequestExecutionException {
		return null;
	}

	public QueryPager getPager(PagingState pagingState, ProtocolVersion protocolVersion) {
		return null;
	}

	protected void recordLatency(TableMetrics metric, long latencyNanos) {
		metric.rangeLatency.addNano(latencyNanos);
	}

	@com.google.common.annotations.VisibleForTesting
	public UnfilteredPartitionIterator queryStorage(final ColumnFamilyStore cfs, ReadExecutionController executionController) {
		ColumnFamilyStore.ViewFragment view = cfs.select(View.selectLive(dataRange().keyRange()));
		Tracing.trace("Executing seq scan across {} sstables for {}", view.sstables.size(), dataRange().keyRange().getString(metadata().getKeyValidator()));
		final List<UnfilteredPartitionIterator> iterators = new ArrayList<>(((Iterables.size(view.memtables)) + (view.sstables.size())));
		try {
			for (Memtable memtable : view.memtables) {
				@SuppressWarnings("resource")
				Memtable.MemtableUnfilteredPartitionIterator iter = memtable.makePartitionIterator(columnFilter(), dataRange(), isForThrift());
				oldestUnrepairedTombstone = Math.min(oldestUnrepairedTombstone, iter.getMinLocalDeletionTime());
				iterators.add((isForThrift() ? ThriftResultsMerger.maybeWrap(iter, metadata(), nowInSec()) : iter));
			}
			SSTableReadsListener readCountUpdater = PartitionRangeReadCommand.newReadCountUpdater();
			for (SSTableReader sstable : view.sstables) {
				@SuppressWarnings("resource")
				UnfilteredPartitionIterator iter = sstable.getScanner(columnFilter(), dataRange(), isForThrift(), readCountUpdater);
				iterators.add((isForThrift() ? ThriftResultsMerger.maybeWrap(iter, metadata(), nowInSec()) : iter));
				if (!(sstable.isRepaired()))
					oldestUnrepairedTombstone = Math.min(oldestUnrepairedTombstone, sstable.getMinLocalDeletionTime());

			}
			return iterators.isEmpty() ? EmptyIterators.unfilteredPartition(metadata(), isForThrift()) : checkCacheFilter(UnfilteredPartitionIterators.mergeLazily(iterators, nowInSec()), cfs);
		} catch (RuntimeException | Error e) {
			try {
				FBUtilities.closeAll(iterators);
			} catch (Exception suppressed) {
				e.addSuppressed(suppressed);
			}
			throw e;
		}
	}

	private static SSTableReadsListener newReadCountUpdater() {
		return new SSTableReadsListener() {
			@Override
			public void onScanningStarted(SSTableReader sstable) {
				sstable.incrementReadCount();
			}
		};
	}

	@Override
	protected int oldestUnrepairedTombstone() {
		return oldestUnrepairedTombstone;
	}

	private UnfilteredPartitionIterator checkCacheFilter(UnfilteredPartitionIterator iter, final ColumnFamilyStore cfs) {
		class CacheFilter extends Transformation {
			@Override
			public BaseRowIterator applyToPartition(BaseRowIterator iter) {
				DecoratedKey dk = iter.partitionKey();
				CachedPartition cached = cfs.getRawCachedPartition(dk);
				ClusteringIndexFilter filter = dataRange().clusteringIndexFilter(dk);
				if ((cached != null) && (cfs.isFilterFullyCoveredBy(filter, limits(), cached, nowInSec()))) {
					iter.close();
					return filter.getUnfilteredRowIterator(columnFilter(), cached);
				}
				return iter;
			}
		}
		return Transformation.apply(iter, new CacheFilter());
	}

	public MessageOut<ReadCommand> createMessage(int version) {
		return dataRange().isPaging() ? new MessageOut<>(PAGED_RANGE, this, ReadCommand.pagedRangeSerializer) : new MessageOut<>(RANGE_SLICE, this, ReadCommand.rangeSliceSerializer);
	}

	protected void appendCQLWhereClause(StringBuilder sb) {
		if ((dataRange.isUnrestricted()) && (rowFilter().isEmpty()))
			return;

		sb.append(" WHERE ");
		if (!(rowFilter().isEmpty())) {
			sb.append(rowFilter());
			if (!(dataRange.isUnrestricted()))
				sb.append(" AND ");

		}
		if (!(dataRange.isUnrestricted()))
			sb.append(dataRange.toCQLString(metadata()));

	}

	public PartitionIterator postReconciliationProcessing(PartitionIterator result) {
		ColumnFamilyStore cfs = Keyspace.open(metadata().ksName).getColumnFamilyStore(metadata().cfName);
		Index index = getIndex(cfs);
		return index == null ? result : index.postProcessorFor(this).apply(result, this);
	}

	@Override
	public boolean selectsFullPartition() {
		return (metadata().isStaticCompactTable()) || ((dataRange.selectsAllPartition()) && (!(rowFilter().hasExpressionOnClusteringOrRegularColumns())));
	}

	@Override
	public String toString() {
		return String.format("Read(%s.%s columns=%s rowfilter=%s limits=%s %s)", metadata().ksName, metadata().cfName, columnFilter(), rowFilter(), limits(), dataRange().toString(metadata()));
	}

	protected void serializeSelection(DataOutputPlus out, int version) throws IOException {
		DataRange.serializer.serialize(dataRange(), out, version, metadata());
	}

	protected long selectionSerializedSize(int version) {
		return DataRange.serializer.serializedSize(dataRange(), version, metadata());
	}

	public boolean isLimitedToOnePartition() {
		return false;
	}

	private static class Deserializer extends ReadCommand.SelectionDeserializer {
		public ReadCommand deserialize(DataInputPlus in, int version, boolean isDigest, int digestVersion, boolean isForThrift, CFMetaData metadata, int nowInSec, ColumnFilter columnFilter, RowFilter rowFilter, DataLimits limits, IndexMetadata index) throws IOException {
			DataRange range = DataRange.serializer.deserialize(in, version, metadata);
			return null;
		}
	}
}

