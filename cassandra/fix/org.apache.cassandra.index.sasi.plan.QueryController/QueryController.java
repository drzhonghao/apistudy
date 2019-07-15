

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.DataRange;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.PartitionPosition;
import org.apache.cassandra.db.PartitionRangeReadCommand;
import org.apache.cassandra.db.ReadCommand;
import org.apache.cassandra.db.ReadExecutionController;
import org.apache.cassandra.db.SinglePartitionReadCommand;
import org.apache.cassandra.db.filter.ClusteringIndexFilter;
import org.apache.cassandra.db.filter.ColumnFilter;
import org.apache.cassandra.db.filter.DataLimits;
import org.apache.cassandra.db.filter.RowFilter;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.index.Index;
import org.apache.cassandra.index.SecondaryIndexManager;
import org.apache.cassandra.index.sasi.SASIIndex;
import org.apache.cassandra.index.sasi.SSTableIndex;
import org.apache.cassandra.index.sasi.TermIterator;
import org.apache.cassandra.index.sasi.conf.ColumnIndex;
import org.apache.cassandra.index.sasi.conf.view.View;
import org.apache.cassandra.index.sasi.disk.Token;
import org.apache.cassandra.index.sasi.exceptions.TimeQuotaExceededException;
import org.apache.cassandra.index.sasi.plan.Expression;
import org.apache.cassandra.index.sasi.plan.Operation;
import org.apache.cassandra.index.sasi.utils.RangeIntersectionIterator;
import org.apache.cassandra.index.sasi.utils.RangeIterator;
import org.apache.cassandra.index.sasi.utils.RangeUnionIterator;
import org.apache.cassandra.io.sstable.SSTable;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.utils.Pair;

import static org.apache.cassandra.index.sasi.plan.Expression.Op.NOT_EQ;
import static org.apache.cassandra.index.sasi.plan.Operation.OperationType.AND;
import static org.apache.cassandra.index.sasi.plan.Operation.OperationType.OR;


public class QueryController {
	private final long executionQuota;

	private final long executionStart;

	private final ColumnFamilyStore cfs;

	private final PartitionRangeReadCommand command;

	private final DataRange range;

	private final Map<Collection<Expression>, List<RangeIterator<Long, Token>>> resources = new HashMap<>();

	public QueryController(ColumnFamilyStore cfs, PartitionRangeReadCommand command, long timeQuotaMs) {
		this.cfs = cfs;
		this.command = command;
		this.range = command.dataRange();
		this.executionQuota = TimeUnit.MILLISECONDS.toNanos(timeQuotaMs);
		this.executionStart = System.nanoTime();
	}

	public boolean isForThrift() {
		return command.isForThrift();
	}

	public CFMetaData metadata() {
		return command.metadata();
	}

	public Collection<RowFilter.Expression> getExpressions() {
		return command.rowFilter().getExpressions();
	}

	public DataRange dataRange() {
		return command.dataRange();
	}

	public AbstractType<?> getKeyValidator() {
		return cfs.metadata.getKeyValidator();
	}

	public ColumnIndex getIndex(RowFilter.Expression expression) {
		Optional<Index> index = cfs.indexManager.getBestIndexFor(expression);
		return index.isPresent() ? ((SASIIndex) (index.get())).getIndex() : null;
	}

	public UnfilteredRowIterator getPartition(DecoratedKey key, ReadExecutionController executionController) {
		if (key == null)
			throw new NullPointerException();

		try {
			SinglePartitionReadCommand partition = SinglePartitionReadCommand.create(command.isForThrift(), cfs.metadata, command.nowInSec(), command.columnFilter(), command.rowFilter().withoutExpressions(), DataLimits.NONE, key, command.clusteringIndexFilter(key));
			return partition.queryMemtableAndDisk(cfs, executionController);
		} finally {
			checkpoint();
		}
	}

	public RangeIterator.Builder<Long, Token> getIndexes(Operation.OperationType op, Collection<Expression> expressions) {
		if (resources.containsKey(expressions))
			throw new IllegalArgumentException("Can't process the same expressions multiple times.");

		RangeIterator.Builder<Long, Token> builder = (op == (OR)) ? RangeUnionIterator.<Long, Token>builder() : RangeIntersectionIterator.<Long, Token>builder();
		List<RangeIterator<Long, Token>> perIndexUnions = new ArrayList<>();
		for (Map.Entry<Expression, Set<SSTableIndex>> e : getView(op, expressions).entrySet()) {
			@SuppressWarnings("resource")
			RangeIterator<Long, Token> index = TermIterator.build(e.getKey(), e.getValue());
			builder.add(index);
			perIndexUnions.add(index);
		}
		resources.put(expressions, perIndexUnions);
		return builder;
	}

	public void checkpoint() {
		if (((System.nanoTime()) - (executionStart)) >= (executionQuota))
			throw new TimeQuotaExceededException();

	}

	public void releaseIndexes(Operation operation) {
	}

	private void releaseIndexes(List<RangeIterator<Long, Token>> indexes) {
		if (indexes == null)
			return;

		indexes.forEach(FileUtils::closeQuietly);
	}

	public void finish() {
		resources.values().forEach(this::releaseIndexes);
	}

	private Map<Expression, Set<SSTableIndex>> getView(Operation.OperationType op, Collection<Expression> expressions) {
		Pair<Expression, Set<SSTableIndex>> primary = (op == (AND)) ? calculatePrimary(expressions) : null;
		Map<Expression, Set<SSTableIndex>> indexes = new HashMap<>();
		for (Expression e : expressions) {
			if ((!(e.isIndexed())) || ((e.getOp()) == (NOT_EQ)))
				continue;

			if ((primary != null) && (e.equals(primary.left))) {
				indexes.put(primary.left, primary.right);
				continue;
			}
			View view = e.index.getView();
			if (view == null)
				continue;

			Set<SSTableIndex> readers = new HashSet<>();
			if ((primary != null) && ((primary.right.size()) > 0)) {
				for (SSTableIndex index : primary.right)
					readers.addAll(view.match(index.minKey(), index.maxKey()));

			}else {
				readers.addAll(applyScope(view.match(e)));
			}
			indexes.put(e, readers);
		}
		return indexes;
	}

	private Pair<Expression, Set<SSTableIndex>> calculatePrimary(Collection<Expression> expressions) {
		Expression expression = null;
		Set<SSTableIndex> primaryIndexes = Collections.emptySet();
		for (Expression e : expressions) {
			if (!(e.isIndexed()))
				continue;

			View view = e.index.getView();
			if (view == null)
				continue;

			Set<SSTableIndex> indexes = applyScope(view.match(e));
			if ((expression == null) || ((primaryIndexes.size()) > (indexes.size()))) {
				primaryIndexes = indexes;
				expression = e;
			}
		}
		return expression == null ? null : Pair.create(expression, primaryIndexes);
	}

	private Set<SSTableIndex> applyScope(Set<SSTableIndex> indexes) {
		return Sets.filter(indexes, ( index) -> {
			SSTableReader sstable = index.getSSTable();
			return ((range.startKey().compareTo(sstable.last)) <= 0) && ((range.stopKey().isMinimum()) || ((sstable.first.compareTo(range.stopKey())) <= 0));
		});
	}
}

