

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.config.ViewDefinition;
import org.apache.cassandra.cql3.CQLStatement;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.cql3.MultiColumnRelation;
import org.apache.cassandra.cql3.Operator;
import org.apache.cassandra.cql3.QueryOptions;
import org.apache.cassandra.cql3.Relation;
import org.apache.cassandra.cql3.SingleColumnRelation;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.cql3.statements.CFStatement;
import org.apache.cassandra.cql3.statements.ParsedStatement;
import org.apache.cassandra.cql3.statements.SelectStatement;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.ReadQuery;
import org.apache.cassandra.db.compaction.CompactionManager;
import org.apache.cassandra.db.filter.RowFilter;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.view.ViewBuilder;
import org.apache.cassandra.schema.KeyspaceMetadata;
import org.apache.cassandra.schema.Views;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.utils.FBUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class View {
	private static final Logger logger = LoggerFactory.getLogger(View.class);

	public final String name;

	private volatile ViewDefinition definition;

	private final ColumnFamilyStore baseCfs;

	public volatile List<ColumnDefinition> baseNonPKColumnsInViewPK;

	private ViewBuilder builder;

	private final SelectStatement.RawStatement rawSelect;

	private SelectStatement select;

	private ReadQuery query;

	public View(ViewDefinition definition, ColumnFamilyStore baseCfs) {
		this.baseCfs = baseCfs;
		this.name = definition.viewName;
		this.rawSelect = definition.select;
		updateDefinition(definition);
	}

	public ViewDefinition getDefinition() {
		return definition;
	}

	public void updateDefinition(ViewDefinition definition) {
		this.definition = definition;
		List<ColumnDefinition> nonPKDefPartOfViewPK = new ArrayList<>();
		for (ColumnDefinition baseColumn : baseCfs.metadata.allColumns()) {
			ColumnDefinition viewColumn = getViewColumn(baseColumn);
			if (((viewColumn != null) && (!(baseColumn.isPrimaryKeyColumn()))) && (viewColumn.isPrimaryKeyColumn()))
				nonPKDefPartOfViewPK.add(baseColumn);

		}
		this.baseNonPKColumnsInViewPK = nonPKDefPartOfViewPK;
	}

	public ColumnDefinition getViewColumn(ColumnDefinition baseColumn) {
		return definition.metadata.getColumnDefinition(baseColumn.name);
	}

	public ColumnDefinition getBaseColumn(ColumnDefinition viewColumn) {
		ColumnDefinition baseColumn = baseCfs.metadata.getColumnDefinition(viewColumn.name);
		assert baseColumn != null;
		return baseColumn;
	}

	public boolean mayBeAffectedBy(DecoratedKey partitionKey, Row update) {
		if (!(getReadQuery().selectsClustering(partitionKey, update.clustering())))
			return false;

		return true;
	}

	public boolean matchesViewFilter(DecoratedKey partitionKey, Row baseRow, int nowInSec) {
		return (getReadQuery().selectsClustering(partitionKey, baseRow.clustering())) && (getSelectStatement().rowFilterForInternalCalls().isSatisfiedBy(baseCfs.metadata, partitionKey, baseRow, nowInSec));
	}

	public SelectStatement getSelectStatement() {
		if ((select) == null) {
			ClientState state = ClientState.forInternalCalls();
			state.setKeyspace(baseCfs.keyspace.getName());
			rawSelect.prepareKeyspace(state);
			ParsedStatement.Prepared prepared = rawSelect.prepare(true, ClientState.forInternalCalls());
			select = ((SelectStatement) (prepared.statement));
		}
		return select;
	}

	public ReadQuery getReadQuery() {
		if ((query) == null) {
			query = getSelectStatement().getQuery(QueryOptions.forInternalCalls(Collections.emptyList()), FBUtilities.nowInSeconds());
			View.logger.trace("View query: {}", rawSelect);
		}
		return query;
	}

	public synchronized void build() {
		if ((this.builder) != null) {
			View.logger.debug("Stopping current view builder due to schema change");
			this.builder.stop();
			this.builder = null;
		}
		CompactionManager.instance.submitViewBuilder(builder);
	}

	@javax.annotation.Nullable
	public static CFMetaData findBaseTable(String keyspace, String viewName) {
		ViewDefinition view = Schema.instance.getView(keyspace, viewName);
		return view == null ? null : Schema.instance.getCFMetaData(view.baseTableId);
	}

	public static Iterable<ViewDefinition> findAll(String keyspace, String baseTable) {
		KeyspaceMetadata ksm = Schema.instance.getKSMetaData(keyspace);
		final UUID baseId = Schema.instance.getId(keyspace, baseTable);
		return Iterables.filter(ksm.views, ( view) -> view.baseTableId.equals(baseId));
	}

	public static String buildSelectStatement(String cfName, Collection<ColumnDefinition> includedColumns, String whereClause) {
		StringBuilder rawSelect = new StringBuilder("SELECT ");
		if ((includedColumns == null) || (includedColumns.isEmpty()))
			rawSelect.append("*");
		else
			rawSelect.append(includedColumns.stream().map(( id) -> id.name.toCQLString()).collect(Collectors.joining(", ")));

		rawSelect.append(" FROM \"").append(cfName).append("\" WHERE ").append(whereClause).append(" ALLOW FILTERING");
		return rawSelect.toString();
	}

	public static String relationsToWhereClause(List<Relation> whereClause) {
		List<String> expressions = new ArrayList<>(whereClause.size());
		for (Relation rel : whereClause) {
			StringBuilder sb = new StringBuilder();
			if (rel.isMultiColumn()) {
				sb.append(((MultiColumnRelation) (rel)).getEntities().stream().map(ColumnDefinition.Raw::toString).collect(Collectors.joining(", ", "(", ")")));
			}else {
				sb.append(((SingleColumnRelation) (rel)).getEntity());
			}
			sb.append(" ").append(rel.operator()).append(" ");
			if (rel.isIN()) {
				sb.append(rel.getInValues().stream().map(Term.Raw::getText).collect(Collectors.joining(", ", "(", ")")));
			}else {
				sb.append(rel.getValue().getText());
			}
			expressions.add(sb.toString());
		}
		return expressions.stream().collect(Collectors.joining(" AND "));
	}

	public boolean hasSamePrimaryKeyColumnsAsBaseTable() {
		return baseNonPKColumnsInViewPK.isEmpty();
	}

	public boolean enforceStrictLiveness() {
		return !(baseNonPKColumnsInViewPK.isEmpty());
	}
}

