

import java.nio.ByteBuffer;
import java.util.NavigableSet;
import java.util.Optional;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringBound;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.DataRange;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.PartitionPosition;
import org.apache.cassandra.db.PartitionRangeReadCommand;
import org.apache.cassandra.db.ReadCommand;
import org.apache.cassandra.db.ReadExecutionController;
import org.apache.cassandra.db.SinglePartitionReadCommand;
import org.apache.cassandra.db.Slice;
import org.apache.cassandra.db.Slices;
import org.apache.cassandra.db.filter.AbstractClusteringIndexFilter;
import org.apache.cassandra.db.filter.ClusteringIndexFilter;
import org.apache.cassandra.db.filter.ClusteringIndexNamesFilter;
import org.apache.cassandra.db.filter.ClusteringIndexSliceFilter;
import org.apache.cassandra.db.filter.ColumnFilter;
import org.apache.cassandra.db.filter.RowFilter;
import org.apache.cassandra.db.partitions.UnfilteredPartitionIterator;
import org.apache.cassandra.db.rows.RowIterator;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.rows.UnfilteredRowIterators;
import org.apache.cassandra.dht.AbstractBounds;
import org.apache.cassandra.index.Index;
import org.apache.cassandra.index.internal.CassandraIndex;
import org.apache.cassandra.utils.CloseableIterator;
import org.apache.cassandra.utils.btree.BTreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class CassandraIndexSearcher implements Index.Searcher {
	private static final Logger logger = LoggerFactory.getLogger(CassandraIndexSearcher.class);

	private final RowFilter.Expression expression;

	protected final CassandraIndex index;

	protected final ReadCommand command;

	public CassandraIndexSearcher(ReadCommand command, RowFilter.Expression expression, CassandraIndex index) {
		this.command = command;
		this.expression = expression;
		this.index = index;
	}

	@SuppressWarnings("resource")
	public UnfilteredPartitionIterator search(ReadExecutionController executionController) {
		DecoratedKey indexKey = index.getBackingTable().get().decorateKey(expression.getIndexValue());
		UnfilteredRowIterator indexIter = queryIndex(indexKey, command, executionController);
		try {
			return queryDataFromIndex(indexKey, UnfilteredRowIterators.filter(indexIter, command.nowInSec()), command, executionController);
		} catch (RuntimeException | Error e) {
			indexIter.close();
			throw e;
		}
	}

	private UnfilteredRowIterator queryIndex(DecoratedKey indexKey, ReadCommand command, ReadExecutionController executionController) {
		ClusteringIndexFilter filter = makeIndexFilter(command);
		ColumnFamilyStore indexCfs = index.getBackingTable().get();
		CFMetaData indexCfm = indexCfs.metadata;
		return SinglePartitionReadCommand.create(indexCfm, command.nowInSec(), indexKey, ColumnFilter.all(indexCfm), filter).queryMemtableAndDisk(indexCfs, executionController.indexReadController());
	}

	private ClusteringIndexFilter makeIndexFilter(ReadCommand command) {
		if (command instanceof SinglePartitionReadCommand) {
			SinglePartitionReadCommand sprc = ((SinglePartitionReadCommand) (command));
			ByteBuffer pk = sprc.partitionKey().getKey();
			ClusteringIndexFilter filter = sprc.clusteringIndexFilter();
			if (filter instanceof ClusteringIndexNamesFilter) {
				NavigableSet<Clustering> requested = ((ClusteringIndexNamesFilter) (filter)).requestedRows();
				BTreeSet.Builder<Clustering> clusterings = BTreeSet.builder(index.getIndexComparator());
				for (Clustering c : requested)
					clusterings.add(makeIndexClustering(pk, c));

				return new ClusteringIndexNamesFilter(clusterings.build(), filter.isReversed());
			}else {
				Slices requested = ((ClusteringIndexSliceFilter) (filter)).requestedSlices();
				Slices.Builder builder = new Slices.Builder(index.getIndexComparator());
				for (Slice slice : requested)
					builder.add(makeIndexBound(pk, slice.start()), makeIndexBound(pk, slice.end()));

				return new ClusteringIndexSliceFilter(builder.build(), filter.isReversed());
			}
		}else {
			DataRange dataRange = ((PartitionRangeReadCommand) (command)).dataRange();
			AbstractBounds<PartitionPosition> range = dataRange.keyRange();
			Slice slice = Slice.ALL;
			if ((range.left) instanceof DecoratedKey) {
				if ((range.right) instanceof DecoratedKey) {
					DecoratedKey startKey = ((DecoratedKey) (range.left));
					DecoratedKey endKey = ((DecoratedKey) (range.right));
					ClusteringBound start = ClusteringBound.BOTTOM;
					ClusteringBound end = ClusteringBound.TOP;
					if (!(dataRange.isNamesQuery())) {
						ClusteringIndexSliceFilter startSliceFilter = ((ClusteringIndexSliceFilter) (dataRange.clusteringIndexFilter(startKey)));
						ClusteringIndexSliceFilter endSliceFilter = ((ClusteringIndexSliceFilter) (dataRange.clusteringIndexFilter(endKey)));
						assert (!(startSliceFilter.isReversed())) && (!(endSliceFilter.isReversed()));
						Slices startSlices = startSliceFilter.requestedSlices();
						Slices endSlices = endSliceFilter.requestedSlices();
						if ((startSlices.size()) > 0)
							start = startSlices.get(0).start();

						if ((endSlices.size()) > 0)
							end = endSlices.get(((endSlices.size()) - 1)).end();

					}
					slice = Slice.make(makeIndexBound(startKey.getKey(), start), makeIndexBound(endKey.getKey(), end));
				}else {
					slice = Slice.make(makeIndexBound(((DecoratedKey) (range.left)).getKey(), ClusteringBound.BOTTOM), ClusteringBound.TOP);
				}
			}
			return new ClusteringIndexSliceFilter(Slices.with(index.getIndexComparator(), slice), false);
		}
	}

	private ClusteringBound makeIndexBound(ByteBuffer rowKey, ClusteringBound bound) {
		return null;
	}

	protected Clustering makeIndexClustering(ByteBuffer rowKey, Clustering clustering) {
		return null;
	}

	protected abstract UnfilteredPartitionIterator queryDataFromIndex(DecoratedKey indexKey, RowIterator indexHits, ReadCommand command, ReadExecutionController executionController);
}

