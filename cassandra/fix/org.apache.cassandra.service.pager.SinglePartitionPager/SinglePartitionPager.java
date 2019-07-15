

import java.nio.ByteBuffer;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.ReadCommand;
import org.apache.cassandra.db.SinglePartitionReadCommand;
import org.apache.cassandra.db.filter.DataLimits;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.service.pager.PagingState;
import org.apache.cassandra.transport.ProtocolVersion;


public class SinglePartitionPager {
	private final SinglePartitionReadCommand command;

	private volatile PagingState.RowMark lastReturned;

	public SinglePartitionPager(SinglePartitionReadCommand command, PagingState state, ProtocolVersion protocolVersion) {
		this.command = command;
		if (state != null) {
			lastReturned = state.rowMark;
		}
	}

	private SinglePartitionPager(SinglePartitionReadCommand command, ProtocolVersion protocolVersion, PagingState.RowMark rowMark, int remaining, int remainingInPartition) {
		this.command = command;
		this.lastReturned = rowMark;
	}

	public SinglePartitionPager withUpdatedLimit(DataLimits newLimits) {
		return null;
	}

	public ByteBuffer key() {
		return command.partitionKey().getKey();
	}

	public DataLimits limits() {
		return command.limits();
	}

	public PagingState state() {
		return null;
	}

	protected ReadCommand nextPageReadCommand(int pageSize) {
		Clustering clustering = ((lastReturned) == null) ? null : lastReturned.clustering(command.metadata());
		return null;
	}

	protected void recordLast(DecoratedKey key, Row last) {
		if ((last != null) && ((last.clustering()) != (Clustering.STATIC_CLUSTERING))) {
		}
	}

	protected boolean isPreviouslyReturnedPartition(DecoratedKey key) {
		return (lastReturned) != null;
	}
}

