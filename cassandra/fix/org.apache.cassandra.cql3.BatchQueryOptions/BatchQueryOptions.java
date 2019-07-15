

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.cql3.QueryOptions;
import org.apache.cassandra.db.ConsistencyLevel;
import org.apache.cassandra.service.QueryState;
import org.apache.cassandra.utils.MD5Digest;


public abstract class BatchQueryOptions {
	public static BatchQueryOptions DEFAULT = BatchQueryOptions.withoutPerStatementVariables(QueryOptions.DEFAULT);

	protected final QueryOptions wrapped;

	private final List<Object> queryOrIdList;

	protected BatchQueryOptions(QueryOptions wrapped, List<Object> queryOrIdList) {
		this.wrapped = wrapped;
		this.queryOrIdList = queryOrIdList;
	}

	public static BatchQueryOptions withoutPerStatementVariables(QueryOptions options) {
		return new BatchQueryOptions.WithoutPerStatementVariables(options, Collections.<Object>emptyList());
	}

	public static BatchQueryOptions withPerStatementVariables(QueryOptions options, List<List<ByteBuffer>> variables, List<Object> queryOrIdList) {
		return new BatchQueryOptions.WithPerStatementVariables(options, variables, queryOrIdList);
	}

	public abstract QueryOptions forStatement(int i);

	public void prepareStatement(int i, List<ColumnSpecification> boundNames) {
		forStatement(i).prepare(boundNames);
	}

	public ConsistencyLevel getConsistency() {
		return wrapped.getConsistency();
	}

	public ConsistencyLevel getSerialConsistency() {
		return wrapped.getSerialConsistency();
	}

	public List<Object> getQueryOrIdList() {
		return queryOrIdList;
	}

	public long getTimestamp(QueryState state) {
		return wrapped.getTimestamp(state);
	}

	private static class WithoutPerStatementVariables extends BatchQueryOptions {
		private WithoutPerStatementVariables(QueryOptions wrapped, List<Object> queryOrIdList) {
			super(wrapped, queryOrIdList);
		}

		public QueryOptions forStatement(int i) {
			return wrapped;
		}
	}

	private static class WithPerStatementVariables extends BatchQueryOptions {
		private final List<QueryOptions> perStatementOptions;

		private WithPerStatementVariables(QueryOptions wrapped, List<List<ByteBuffer>> variables, List<Object> queryOrIdList) {
			super(wrapped, queryOrIdList);
			this.perStatementOptions = new ArrayList<>(variables.size());
			for (final List<ByteBuffer> vars : variables) {
			}
		}

		public QueryOptions forStatement(int i) {
			return perStatementOptions.get(i);
		}

		@Override
		public void prepareStatement(int i, List<ColumnSpecification> boundNames) {
			if (isPreparedStatement(i)) {
				QueryOptions options = perStatementOptions.get(i);
				options.prepare(boundNames);
				options = QueryOptions.addColumnSpecifications(options, boundNames);
				perStatementOptions.set(i, options);
			}else {
				super.prepareStatement(i, boundNames);
			}
		}

		private boolean isPreparedStatement(int i) {
			return (getQueryOrIdList().get(i)) instanceof MD5Digest;
		}
	}
}

