

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.cassandra.db.ConsistencyLevel;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.ReadExecutionController;
import org.apache.cassandra.db.SinglePartitionReadCommand;
import org.apache.cassandra.db.filter.DataLimits;
import org.apache.cassandra.db.partitions.BasePartitionIterator;
import org.apache.cassandra.db.partitions.PartitionIterator;
import org.apache.cassandra.db.rows.RowIterator;
import org.apache.cassandra.exceptions.RequestExecutionException;
import org.apache.cassandra.exceptions.RequestValidationException;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.service.pager.PagingState;
import org.apache.cassandra.service.pager.QueryPager;
import org.apache.cassandra.service.pager.SinglePartitionPager;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.utils.AbstractIterator;


public class MultiPartitionPager implements QueryPager {
	private final SinglePartitionPager[] pagers;

	private final DataLimits limit;

	private final int nowInSec;

	private int remaining;

	private int current;

	public MultiPartitionPager(SinglePartitionReadCommand.Group group, PagingState state, ProtocolVersion protocolVersion) {
		this.limit = group.limits();
		this.nowInSec = group.nowInSec();
		int i = 0;
		if (state != null)
			for (; i < (group.commands.size()); i++)
				if (group.commands.get(i).partitionKey().getKey().equals(state.partitionKey))
					break;



		if (i >= (group.commands.size())) {
			pagers = null;
			return;
		}
		pagers = new SinglePartitionPager[(group.commands.size()) - i];
		SinglePartitionReadCommand command = group.commands.get(i);
		pagers[0] = command.getPager(state, protocolVersion);
		for (int j = i + 1; j < (group.commands.size()); j++)
			pagers[(j - i)] = group.commands.get(j).getPager(null, protocolVersion);

		remaining = (state == null) ? limit.count() : state.remaining;
	}

	private MultiPartitionPager(SinglePartitionPager[] pagers, DataLimits limit, int nowInSec, int remaining, int current) {
		this.pagers = pagers;
		this.limit = limit;
		this.nowInSec = nowInSec;
		this.remaining = remaining;
		this.current = current;
	}

	public QueryPager withUpdatedLimit(DataLimits newLimits) {
		SinglePartitionPager[] newPagers = Arrays.copyOf(pagers, pagers.length);
		newPagers[current] = newPagers[current].withUpdatedLimit(newLimits);
		return new MultiPartitionPager(newPagers, newLimits, nowInSec, remaining, current);
	}

	public PagingState state() {
		if (isExhausted())
			return null;

		PagingState state = pagers[current].state();
		return null;
	}

	public boolean isExhausted() {
		if (((remaining) <= 0) || ((pagers) == null))
			return true;

		while ((current) < (pagers.length)) {
			if (!(pagers[current].isExhausted()))
				return false;

			(current)++;
		} 
		return true;
	}

	public ReadExecutionController executionController() {
		for (int i = current; i < (pagers.length); i++) {
			if ((pagers[i]) != null)
				return pagers[i].executionController();

		}
		throw new AssertionError("Shouldn't be called on an exhausted pager");
	}

	@SuppressWarnings("resource")
	public PartitionIterator fetchPage(int pageSize, ConsistencyLevel consistency, ClientState clientState, long queryStartNanoTime) throws RequestExecutionException, RequestValidationException {
		int toQuery = Math.min(remaining, pageSize);
		return new MultiPartitionPager.PagersIterator(toQuery, consistency, clientState, null, queryStartNanoTime);
	}

	@SuppressWarnings("resource")
	public PartitionIterator fetchPageInternal(int pageSize, ReadExecutionController executionController) throws RequestExecutionException, RequestValidationException {
		int toQuery = Math.min(remaining, pageSize);
		return new MultiPartitionPager.PagersIterator(toQuery, null, null, executionController, System.nanoTime());
	}

	private class PagersIterator extends AbstractIterator<RowIterator> implements PartitionIterator {
		private final int pageSize;

		private PartitionIterator result;

		private boolean closed;

		private final long queryStartNanoTime;

		private final ConsistencyLevel consistency;

		private final ClientState clientState;

		private final ReadExecutionController executionController;

		private int pagerMaxRemaining;

		private int counted;

		public PagersIterator(int pageSize, ConsistencyLevel consistency, ClientState clientState, ReadExecutionController executionController, long queryStartNanoTime) {
			this.pageSize = pageSize;
			this.consistency = consistency;
			this.clientState = clientState;
			this.executionController = executionController;
			this.queryStartNanoTime = queryStartNanoTime;
		}

		protected RowIterator computeNext() {
			while (((result) == null) || (!(result.hasNext()))) {
				if ((result) != null) {
					result.close();
					counted += (pagerMaxRemaining) - (pagers[current].maxRemaining());
				}
				boolean isDone = ((counted) >= (pageSize)) || ((((result) != null) && (limit.isGroupByLimit())) && (!(pagers[current].isExhausted())));
				if (isDone || (isExhausted())) {
					closed = true;
					return endOfData();
				}
				pagerMaxRemaining = pagers[current].maxRemaining();
				int toQuery = (pageSize) - (counted);
				result = ((consistency) == null) ? pagers[current].fetchPageInternal(toQuery, executionController) : pagers[current].fetchPage(toQuery, consistency, clientState, queryStartNanoTime);
			} 
			return result.next();
		}

		public void close() {
			remaining -= counted;
			if (((result) != null) && (!(closed)))
				result.close();

		}
	}

	public int maxRemaining() {
		return remaining;
	}
}

