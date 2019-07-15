

import com.google.common.base.Joiner;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.cql3.CFName;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.IndexName;
import org.apache.cassandra.cql3.Operator;
import org.apache.cassandra.cql3.QueryOptions;
import org.apache.cassandra.cql3.Relation;
import org.apache.cassandra.cql3.SuperColumnCompatibility;
import org.apache.cassandra.cql3.VariableSpecifications;
import org.apache.cassandra.cql3.WhereClause;
import org.apache.cassandra.cql3.functions.Function;
import org.apache.cassandra.cql3.restrictions.CustomIndexExpression;
import org.apache.cassandra.cql3.restrictions.IndexRestrictions;
import org.apache.cassandra.cql3.restrictions.Restriction;
import org.apache.cassandra.cql3.restrictions.Restrictions;
import org.apache.cassandra.cql3.restrictions.SingleRestriction;
import org.apache.cassandra.cql3.statements.Bound;
import org.apache.cassandra.cql3.statements.RequestValidations;
import org.apache.cassandra.cql3.statements.StatementType;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringBound;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.PartitionPosition;
import org.apache.cassandra.db.filter.RowFilter;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.dht.AbstractBounds;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.index.Index;
import org.apache.cassandra.index.SecondaryIndexManager;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.schema.IndexMetadata;
import org.apache.cassandra.schema.Indexes;
import org.apache.cassandra.utils.btree.BTreeSet;

import static org.apache.cassandra.db.PartitionPosition.ForKey.get;


public final class StatementRestrictions {
	public static final String REQUIRES_ALLOW_FILTERING_MESSAGE = "Cannot execute this query as it might involve data filtering and " + ("thus may have unpredictable performance. If you want to execute " + "this query despite the performance unpredictability, use ALLOW FILTERING");

	private final StatementType type;

	public final CFMetaData cfm;

	private Set<ColumnDefinition> notNullColumns;

	private final IndexRestrictions filterRestrictions = new IndexRestrictions();

	private boolean usesSecondaryIndexing;

	private boolean isKeyRange;

	private boolean hasRegularColumnsRestrictions;

	public static StatementRestrictions empty(StatementType type, CFMetaData cfm) {
		return new StatementRestrictions(type, cfm, false);
	}

	private StatementRestrictions(StatementType type, CFMetaData cfm, boolean allowFiltering) {
		this.type = type;
		this.cfm = cfm;
		this.notNullColumns = new HashSet<>();
	}

	public StatementRestrictions(StatementType type, CFMetaData cfm, WhereClause whereClause, VariableSpecifications boundNames, boolean selectsOnlyStaticColumns, boolean selectsComplexColumn, boolean allowFiltering, boolean forView) {
		this(type, cfm, allowFiltering);
		ColumnFamilyStore cfs;
		SecondaryIndexManager secondaryIndexManager = null;
		if (type.allowUseOfSecondaryIndices()) {
			cfs = Keyspace.open(cfm.ksName).getColumnFamilyStore(cfm.cfName);
			secondaryIndexManager = cfs.indexManager;
		}
		for (Relation relation : whereClause.relations) {
			if ((relation.operator()) == (Operator.IS_NOT)) {
				if (!forView)
					throw new InvalidRequestException(("Unsupported restriction: " + relation));

				for (ColumnDefinition def : relation.toRestriction(cfm, boundNames).getColumnDefs())
					this.notNullColumns.add(def);

			}else
				if (relation.isLIKE()) {
					Restriction restriction = relation.toRestriction(cfm, boundNames);
					if ((!(type.allowUseOfSecondaryIndices())) || (!(restriction.hasSupportingIndex(secondaryIndexManager))))
						throw new InvalidRequestException(String.format(("LIKE restriction is only supported on properly " + "indexed columns. %s is not valid."), relation.toString()));

					addRestriction(restriction);
				}else {
					if (((cfm.isSuper()) && (cfm.isDense())) && (!(relation.onToken())))
						addRestriction(relation.toSuperColumnAdapter().toRestriction(cfm, boundNames));
					else
						addRestriction(relation.toRestriction(cfm, boundNames));

				}

		}
		boolean hasQueriableClusteringColumnIndex = false;
		boolean hasQueriableIndex = false;
		if (type.allowUseOfSecondaryIndices()) {
			if (whereClause.containsCustomExpressions())
				processCustomIndexExpressions(whereClause.expressions, boundNames, secondaryIndexManager);

		}
		processPartitionKeyRestrictions(hasQueriableIndex, allowFiltering, forView);
		if (selectsOnlyStaticColumns && (hasClusteringColumnsRestrictions())) {
			if ((type.isDelete()) || (type.isUpdate()))
				throw RequestValidations.invalidRequest("Invalid restrictions on clustering columns since the %s statement modifies only static columns", type);

			if (type.isSelect())
				throw RequestValidations.invalidRequest("Cannot restrict clustering columns when selecting only static columns");

		}
		processClusteringColumnsRestrictions(hasQueriableIndex, selectsOnlyStaticColumns, selectsComplexColumn, forView, allowFiltering);
		if ((isKeyRange) && hasQueriableClusteringColumnIndex)
			usesSecondaryIndexing = true;

		if (usesSecondaryIndexing)
			validateSecondaryIndexSelections(selectsOnlyStaticColumns);

	}

	private void addRestriction(Restriction restriction) {
		ColumnDefinition def = restriction.getFirstColumn();
		if (def.isPartitionKey()) {
		}else
			if (def.isClusteringColumn()) {
			}else {
			}

	}

	public void addFunctionsTo(List<Function> functions) {
	}

	public IndexRestrictions getIndexRestrictions() {
		return filterRestrictions;
	}

	public Set<ColumnDefinition> nonPKRestrictedColumns(boolean includeNotNullRestrictions) {
		Set<ColumnDefinition> columns = new HashSet<>();
		for (Restrictions r : filterRestrictions.getRestrictions()) {
			for (ColumnDefinition def : r.getColumnDefs())
				if (!(def.isPrimaryKeyColumn()))
					columns.add(def);


		}
		if (includeNotNullRestrictions) {
			for (ColumnDefinition def : notNullColumns) {
				if (!(def.isPrimaryKeyColumn()))
					columns.add(def);

			}
		}
		return columns;
	}

	public Set<ColumnDefinition> notNullColumns() {
		return notNullColumns;
	}

	public boolean isRestricted(ColumnDefinition column) {
		if (notNullColumns.contains(column))
			return true;

		return getRestrictions(column.kind).getColumnDefs().contains(column);
	}

	public boolean keyIsInRelation() {
		return false;
	}

	public boolean isKeyRange() {
		return this.isKeyRange;
	}

	public boolean isColumnRestrictedByEq(ColumnDefinition columnDef) {
		Set<Restriction> restrictions = getRestrictions(columnDef.kind).getRestrictions(columnDef);
		return restrictions.stream().filter(SingleRestriction.class::isInstance).anyMatch(( p) -> ((SingleRestriction) (p)).isEQ());
	}

	private Restrictions getRestrictions(ColumnDefinition.Kind kind) {
		return null;
	}

	public boolean usesSecondaryIndexing() {
		return this.usesSecondaryIndexing;
	}

	private void processPartitionKeyRestrictions(boolean hasQueriableIndex, boolean allowFiltering, boolean forView) {
		if (!(type.allowPartitionKeyRanges())) {
		}else {
		}
	}

	public boolean hasPartitionKeyRestrictions() {
		return false;
	}

	public boolean hasNonPrimaryKeyRestrictions() {
		return false;
	}

	private Collection<ColumnIdentifier> getPartitionKeyUnrestrictedComponents() {
		List<ColumnDefinition> list = new ArrayList<>(cfm.partitionKeyColumns());
		return ColumnDefinition.toIdentifiers(list);
	}

	public boolean isPartitionKeyRestrictionsOnToken() {
		return false;
	}

	public boolean clusteringKeyRestrictionsHasIN() {
		return false;
	}

	private void processClusteringColumnsRestrictions(boolean hasQueriableIndex, boolean selectsOnlyStaticColumns, boolean selectsComplexColumn, boolean forView, boolean allowFiltering) {
		if ((!(type.allowClusteringColumnSlices())) && ((!(cfm.isCompactTable())) || ((cfm.isCompactTable()) && (!(hasClusteringColumnsRestrictions()))))) {
			if ((!selectsOnlyStaticColumns) && (hasUnrestrictedClusteringColumns()))
				throw RequestValidations.invalidRequest("Some clustering keys are missing: %s", Joiner.on(", ").join(getUnrestrictedClusteringColumns()));

		}else {
		}
	}

	private Collection<ColumnIdentifier> getUnrestrictedClusteringColumns() {
		List<ColumnDefinition> missingClusteringColumns = new ArrayList<>(cfm.clusteringColumns());
		return ColumnDefinition.toIdentifiers(missingClusteringColumns);
	}

	private boolean hasUnrestrictedClusteringColumns() {
		return false;
	}

	private void processCustomIndexExpressions(List<CustomIndexExpression> expressions, VariableSpecifications boundNames, SecondaryIndexManager indexManager) {
		if (!(MessagingService.instance().areAllNodesAtLeast30()))
			throw new InvalidRequestException("Please upgrade all nodes to at least 3.0 before using custom index expressions");

		if ((expressions.size()) > 1)
			throw new InvalidRequestException(IndexRestrictions.MULTIPLE_EXPRESSIONS);

		CustomIndexExpression expression = expressions.get(0);
		CFName cfName = expression.targetIndex.getCfName();
		if ((cfName.hasKeyspace()) && (!(expression.targetIndex.getKeyspace().equals(cfm.ksName)))) {
		}
		if (((cfName.getColumnFamily()) != null) && (!(cfName.getColumnFamily().equals(cfm.cfName)))) {
		}
		if (!(cfm.getIndexes().has(expression.targetIndex.getIdx()))) {
		}
		Index index = indexManager.getIndex(cfm.getIndexes().get(expression.targetIndex.getIdx()).get());
		if (!(index.getIndexMetadata().isCustom())) {
		}
		AbstractType<?> expressionType = index.customExpressionValueType();
		if (expressionType == null) {
		}
		expression.prepareValue(cfm, expressionType, boundNames);
		filterRestrictions.add(expression);
	}

	public RowFilter getRowFilter(SecondaryIndexManager indexManager, QueryOptions options) {
		if (filterRestrictions.isEmpty())
			return RowFilter.NONE;

		RowFilter filter = RowFilter.create();
		for (Restrictions restrictions : filterRestrictions.getRestrictions())
			restrictions.addRowFilterTo(filter, indexManager, options);

		for (CustomIndexExpression expression : filterRestrictions.getCustomIndexExpressions())
			expression.addToRowFilter(filter, cfm, options);

		return filter;
	}

	public List<ByteBuffer> getPartitionKeys(final QueryOptions options) {
		return null;
	}

	private ByteBuffer getPartitionKeyBound(Bound b, QueryOptions options) {
		return null;
	}

	public AbstractBounds<PartitionPosition> getPartitionKeyBounds(QueryOptions options) {
		IPartitioner p = cfm.partitioner;
		return getPartitionKeyBounds(p, options);
	}

	private AbstractBounds<PartitionPosition> getPartitionKeyBounds(IPartitioner p, QueryOptions options) {
		ByteBuffer startKeyBytes = getPartitionKeyBound(Bound.START, options);
		ByteBuffer finishKeyBytes = getPartitionKeyBound(Bound.END, options);
		PartitionPosition startKey = get(startKeyBytes, p);
		PartitionPosition finishKey = get(finishKeyBytes, p);
		if (((startKey.compareTo(finishKey)) > 0) && (!(finishKey.isMinimum())))
			return null;

		return null;
	}

	private AbstractBounds<PartitionPosition> getPartitionKeyBoundsForTokenRestrictions(IPartitioner p, QueryOptions options) {
		Token startToken = getTokenBound(Bound.START, options, p);
		Token endToken = getTokenBound(Bound.END, options, p);
		int cmp = startToken.compareTo(endToken);
		return null;
	}

	private Token getTokenBound(Bound b, QueryOptions options, IPartitioner p) {
		return null;
	}

	public boolean hasClusteringColumnsRestrictions() {
		return false;
	}

	public NavigableSet<Clustering> getClusteringColumns(QueryOptions options) {
		if (cfm.isStaticCompactTable())
			return BTreeSet.empty(cfm.comparator);

		return null;
	}

	public NavigableSet<ClusteringBound> getClusteringColumnsBounds(Bound b, QueryOptions options) {
		return null;
	}

	public boolean isColumnRange() {
		int numberOfClusteringColumns = (cfm.isStaticCompactTable()) ? 0 : cfm.clusteringColumns().size();
		return false;
	}

	public boolean needFiltering() {
		int numberOfRestrictions = filterRestrictions.getCustomIndexExpressions().size();
		for (Restrictions restrictions : filterRestrictions.getRestrictions())
			numberOfRestrictions += restrictions.size();

		return false;
	}

	private void validateSecondaryIndexSelections(boolean selectsOnlyStaticColumns) {
		RequestValidations.checkFalse(keyIsInRelation(), "Select on indexed columns and with IN clause for the PRIMARY KEY are not supported");
		RequestValidations.checkFalse(selectsOnlyStaticColumns, "Queries using 2ndary indexes don't support selecting only static columns");
	}

	public boolean hasAllPKColumnsRestrictedByEqualities() {
		return false;
	}

	public boolean hasRegularColumnsRestrictions() {
		return hasRegularColumnsRestrictions;
	}

	private SuperColumnCompatibility.SuperColumnRestrictions cached;

	public SuperColumnCompatibility.SuperColumnRestrictions getSuperColumnRestrictions() {
		assert (cfm.isSuper()) && (cfm.isDense());
		if ((cached) == null) {
		}
		return cached;
	}
}

