

import java.util.Iterator;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ConsistencyLevel;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.EmptyIterators;
import org.apache.cassandra.db.ReadCommand;
import org.apache.cassandra.db.ReadExecutionController;
import org.apache.cassandra.db.ReadQuery;
import org.apache.cassandra.db.filter.DataLimits;
import org.apache.cassandra.db.partitions.PartitionIterator;
import org.apache.cassandra.db.partitions.UnfilteredPartitionIterator;
import org.apache.cassandra.db.rows.BaseRowIterator;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.RowIterator;
import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.transform.Transformation;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.service.pager.QueryPager;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.utils.CloseableIterator;


abstract class AbstractQueryPager implements QueryPager {
	protected final ReadCommand command;

	protected final DataLimits limits;

	protected final ProtocolVersion protocolVersion;

	private final boolean enforceStrictLiveness;

	private int remaining;

	private DecoratedKey lastKey;

	private int remainingInPartition;

	private boolean exhausted;

	protected AbstractQueryPager(ReadCommand command, ProtocolVersion protocolVersion) {
		this.command = command;
		this.protocolVersion = protocolVersion;
		this.limits = command.limits();
		this.enforceStrictLiveness = command.metadata().enforceStrictLiveness();
		this.remaining = limits.count();
		this.remainingInPartition = limits.perPartitionCount();
	}

	public ReadExecutionController executionController() {
		return command.executionController();
	}

	public PartitionIterator fetchPage(int pageSize, ConsistencyLevel consistency, ClientState clientState, long queryStartNanoTime) {
		if (isExhausted())
			return EmptyIterators.partition();

		pageSize = Math.min(pageSize, remaining);
		AbstractQueryPager.Pager pager = new AbstractQueryPager.RowPager(limits.forPaging(pageSize), command.nowInSec());
		return Transformation.apply(nextPageReadCommand(pageSize).execute(consistency, clientState, queryStartNanoTime), pager);
	}

	public PartitionIterator fetchPageInternal(int pageSize, ReadExecutionController executionController) {
		if (isExhausted())
			return EmptyIterators.partition();

		pageSize = Math.min(pageSize, remaining);
		AbstractQueryPager.RowPager pager = new AbstractQueryPager.RowPager(limits.forPaging(pageSize), command.nowInSec());
		return Transformation.apply(nextPageReadCommand(pageSize).executeInternal(executionController), pager);
	}

	public UnfilteredPartitionIterator fetchPageUnfiltered(CFMetaData cfm, int pageSize, ReadExecutionController executionController) {
		if (isExhausted())
			return EmptyIterators.unfilteredPartition(cfm, false);

		pageSize = Math.min(pageSize, remaining);
		AbstractQueryPager.UnfilteredPager pager = new AbstractQueryPager.UnfilteredPager(limits.forPaging(pageSize), command.nowInSec());
		return Transformation.apply(nextPageReadCommand(pageSize).executeLocally(executionController), pager);
	}

	private class UnfilteredPager extends AbstractQueryPager.Pager<Unfiltered> {
		private UnfilteredPager(DataLimits pageLimits, int nowInSec) {
			super(pageLimits, nowInSec);
		}

		protected BaseRowIterator<Unfiltered> apply(BaseRowIterator<Unfiltered> partition) {
			return Transformation.apply(counter.applyTo(((UnfilteredRowIterator) (partition))), this);
		}
	}

	private class RowPager extends AbstractQueryPager.Pager<Row> {
		private RowPager(DataLimits pageLimits, int nowInSec) {
			super(pageLimits, nowInSec);
		}

		protected BaseRowIterator<Row> apply(BaseRowIterator<Row> partition) {
			return Transformation.apply(counter.applyTo(((RowIterator) (partition))), this);
		}
	}

	private abstract class Pager<T extends Unfiltered> extends Transformation<BaseRowIterator<T>> {
		private final DataLimits pageLimits;

		protected final DataLimits.Counter counter;

		private DecoratedKey currentKey;

		private Row lastRow;

		private boolean isFirstPartition = true;

		private Pager(DataLimits pageLimits, int nowInSec) {
			this.counter = pageLimits.newCounter(nowInSec, true, command.selectsFullPartition(), enforceStrictLiveness);
			this.pageLimits = pageLimits;
		}

		@Override
		public BaseRowIterator<T> applyToPartition(BaseRowIterator<T> partition) {
			currentKey = partition.partitionKey();
			if (isFirstPartition) {
				isFirstPartition = false;
				if ((isPreviouslyReturnedPartition(currentKey)) && (!(partition.hasNext()))) {
					partition.close();
					return null;
				}
			}
			return apply(partition);
		}

		protected abstract BaseRowIterator<T> apply(BaseRowIterator<T> partition);

		@Override
		public void onClose() {
			counter.onClose();
			recordLast(lastKey, lastRow);
			remaining -= counter.counted();
			if (((lastRow) != null) && (((lastRow.clustering()) == (Clustering.STATIC_CLUSTERING)) || ((lastRow.clustering()) == (Clustering.EMPTY)))) {
				remainingInPartition = 0;
			}else {
				remainingInPartition -= counter.countedInCurrentPartition();
			}
			exhausted = pageLimits.isExhausted(counter);
		}

		public Row applyToStatic(Row row) {
			if (!(row.isEmpty())) {
				remainingInPartition = limits.perPartitionCount();
				lastKey = currentKey;
				lastRow = row;
			}
			return row;
		}

		@Override
		public Row applyToRow(Row row) {
			if (!(currentKey.equals(lastKey))) {
				remainingInPartition = limits.perPartitionCount();
				lastKey = currentKey;
			}
			lastRow = row;
			return row;
		}
	}

	protected void restoreState(DecoratedKey lastKey, int remaining, int remainingInPartition) {
		this.lastKey = lastKey;
		this.remaining = remaining;
		this.remainingInPartition = remainingInPartition;
	}

	public boolean isExhausted() {
		return false;
	}

	public int maxRemaining() {
		return remaining;
	}

	protected int remainingInPartition() {
		return remainingInPartition;
	}

	protected abstract ReadCommand nextPageReadCommand(int pageSize);

	protected abstract void recordLast(DecoratedKey key, Row row);

	protected abstract boolean isPreviouslyReturnedPartition(DecoratedKey key);
}

