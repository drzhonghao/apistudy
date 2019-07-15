

import com.google.common.collect.Iterables;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;
import org.apache.cassandra.auth.Permission;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.config.ColumnDefinition.Raw;
import org.apache.cassandra.config.ViewDefinition;
import org.apache.cassandra.cql3.Attributes;
import org.apache.cassandra.cql3.CFName;
import org.apache.cassandra.cql3.CQLStatement;
import org.apache.cassandra.cql3.ColumnCondition;
import org.apache.cassandra.cql3.ColumnCondition.Raw;
import org.apache.cassandra.cql3.ColumnConditions;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.cql3.Conditions;
import org.apache.cassandra.cql3.Operation;
import org.apache.cassandra.cql3.Operations;
import org.apache.cassandra.cql3.QueryOptions;
import org.apache.cassandra.cql3.QueryProcessor;
import org.apache.cassandra.cql3.ResultSet;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.cql3.UpdateParameters;
import org.apache.cassandra.cql3.VariableSpecifications;
import org.apache.cassandra.cql3.WhereClause;
import org.apache.cassandra.cql3.functions.Function;
import org.apache.cassandra.cql3.restrictions.StatementRestrictions;
import org.apache.cassandra.cql3.selection.Selection;
import org.apache.cassandra.cql3.statements.Bound;
import org.apache.cassandra.cql3.statements.CFStatement;
import org.apache.cassandra.cql3.statements.CQL3CasRequest;
import org.apache.cassandra.cql3.statements.ParsedStatement;
import org.apache.cassandra.cql3.statements.RequestValidations;
import org.apache.cassandra.cql3.statements.StatementType;
import org.apache.cassandra.db.CBuilder;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringBound;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.ConsistencyLevel;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.IMutation;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.PartitionColumns;
import org.apache.cassandra.db.ReadCommand;
import org.apache.cassandra.db.ReadExecutionController;
import org.apache.cassandra.db.SinglePartitionReadCommand;
import org.apache.cassandra.db.Slice;
import org.apache.cassandra.db.Slices;
import org.apache.cassandra.db.filter.ClusteringIndexFilter;
import org.apache.cassandra.db.filter.ClusteringIndexNamesFilter;
import org.apache.cassandra.db.filter.ClusteringIndexSliceFilter;
import org.apache.cassandra.db.filter.ColumnFilter;
import org.apache.cassandra.db.filter.DataLimits;
import org.apache.cassandra.db.filter.RowFilter;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.BooleanType;
import org.apache.cassandra.db.partitions.FilteredPartition;
import org.apache.cassandra.db.partitions.Partition;
import org.apache.cassandra.db.partitions.PartitionIterator;
import org.apache.cassandra.db.partitions.PartitionIterators;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.db.rows.BaseRowIterator;
import org.apache.cassandra.db.rows.RowIterator;
import org.apache.cassandra.db.view.View;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.exceptions.RequestExecutionException;
import org.apache.cassandra.exceptions.RequestValidationException;
import org.apache.cassandra.exceptions.UnauthorizedException;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.service.QueryState;
import org.apache.cassandra.service.StorageProxy;
import org.apache.cassandra.service.paxos.Commit;
import org.apache.cassandra.thrift.ThriftValidation;
import org.apache.cassandra.transport.messages.ResultMessage;
import org.apache.cassandra.triggers.TriggerExecutor;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.Pair;
import org.apache.cassandra.utils.UUIDGen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class ModificationStatement implements CQLStatement {
	protected static final Logger logger = LoggerFactory.getLogger(ModificationStatement.class);

	public static final String CUSTOM_EXPRESSIONS_NOT_ALLOWED = "Custom index expressions cannot be used in WHERE clauses for UPDATE or DELETE statements";

	private static final ColumnIdentifier CAS_RESULT_COLUMN = new ColumnIdentifier("[applied]", false);

	protected final StatementType type;

	private final int boundTerms;

	public final CFMetaData cfm;

	private final Attributes attrs;

	private final StatementRestrictions restrictions;

	private final Operations operations;

	private final PartitionColumns updatedColumns;

	private final Conditions conditions;

	private final PartitionColumns conditionColumns;

	private final PartitionColumns requiresRead;

	public ModificationStatement(StatementType type, int boundTerms, CFMetaData cfm, Operations operations, StatementRestrictions restrictions, Conditions conditions, Attributes attrs) {
		this.type = type;
		this.boundTerms = boundTerms;
		this.cfm = cfm;
		this.restrictions = restrictions;
		this.operations = operations;
		this.conditions = conditions;
		this.attrs = attrs;
		if (!(conditions.isEmpty())) {
			RequestValidations.checkFalse(cfm.isCounter(), "Conditional updates are not supported on counter tables");
			RequestValidations.checkFalse(attrs.isTimestampSet(), "Cannot provide custom timestamp for conditional updates");
		}
		PartitionColumns.Builder conditionColumnsBuilder = PartitionColumns.builder();
		Iterable<ColumnDefinition> columns = conditions.getColumns();
		if (columns != null)
			conditionColumnsBuilder.addAll(columns);

		PartitionColumns.Builder updatedColumnsBuilder = PartitionColumns.builder();
		PartitionColumns.Builder requiresReadBuilder = PartitionColumns.builder();
		for (Operation operation : operations) {
			updatedColumnsBuilder.add(operation.column);
			if (operation.requiresRead()) {
				conditionColumnsBuilder.add(operation.column);
				requiresReadBuilder.add(operation.column);
			}
		}
		PartitionColumns modifiedColumns = updatedColumnsBuilder.build();
		if (((cfm.isCompactTable()) && (modifiedColumns.isEmpty())) && (updatesRegularRows()))
			modifiedColumns = cfm.partitionColumns();

		this.updatedColumns = modifiedColumns;
		this.conditionColumns = conditionColumnsBuilder.build();
		this.requiresRead = requiresReadBuilder.build();
	}

	public Iterable<Function> getFunctions() {
		List<Function> functions = new ArrayList<>();
		addFunctionsTo(functions);
		return functions;
	}

	public void addFunctionsTo(List<Function> functions) {
		attrs.addFunctionsTo(functions);
		restrictions.addFunctionsTo(functions);
		operations.addFunctionsTo(functions);
		conditions.addFunctionsTo(functions);
	}

	public StatementRestrictions getRestrictions() {
		return restrictions;
	}

	public abstract void addUpdateForKey(PartitionUpdate update, Clustering clustering, UpdateParameters params);

	public abstract void addUpdateForKey(PartitionUpdate update, Slice slice, UpdateParameters params);

	public int getBoundTerms() {
		return boundTerms;
	}

	public String keyspace() {
		return cfm.ksName;
	}

	public String columnFamily() {
		return cfm.cfName;
	}

	public boolean isCounter() {
		return cfm.isCounter();
	}

	public boolean isView() {
		return cfm.isView();
	}

	public long getTimestamp(long now, QueryOptions options) throws InvalidRequestException {
		return attrs.getTimestamp(now, options);
	}

	public boolean isTimestampSet() {
		return attrs.isTimestampSet();
	}

	public int getTimeToLive(QueryOptions options) throws InvalidRequestException {
		return attrs.getTimeToLive(options, cfm);
	}

	public void checkAccess(ClientState state) throws InvalidRequestException, UnauthorizedException {
		state.hasColumnFamilyAccess(cfm, Permission.MODIFY);
		if (hasConditions())
			state.hasColumnFamilyAccess(cfm, Permission.SELECT);

		Iterator<ViewDefinition> views = View.findAll(keyspace(), columnFamily()).iterator();
		if (views.hasNext()) {
			state.hasColumnFamilyAccess(cfm, Permission.SELECT);
			do {
				state.hasColumnFamilyAccess(views.next().metadata, Permission.MODIFY);
			} while (views.hasNext() );
		}
		for (Function function : getFunctions())
			state.ensureHasPermission(Permission.EXECUTE, function);

	}

	public void validate(ClientState state) throws InvalidRequestException {
		RequestValidations.checkFalse(((hasConditions()) && (attrs.isTimestampSet())), "Cannot provide custom timestamp for conditional updates");
		RequestValidations.checkFalse(((isCounter()) && (attrs.isTimestampSet())), "Cannot provide custom timestamp for counter updates");
		RequestValidations.checkFalse(((isCounter()) && (attrs.isTimeToLiveSet())), "Cannot provide custom TTL for counter updates");
		RequestValidations.checkFalse(isView(), "Cannot directly modify a materialized view");
	}

	public PartitionColumns updatedColumns() {
		return updatedColumns;
	}

	public PartitionColumns conditionColumns() {
		return conditionColumns;
	}

	public boolean updatesRegularRows() {
		return (cfm.clusteringColumns().isEmpty()) || (restrictions.hasClusteringColumnsRestrictions());
	}

	public boolean updatesStaticRow() {
		return operations.appliesToStaticColumns();
	}

	public List<Operation> getRegularOperations() {
		return operations.regularOperations();
	}

	public List<Operation> getStaticOperations() {
		return operations.staticOperations();
	}

	public Iterable<Operation> allOperations() {
		return operations;
	}

	public Iterable<ColumnDefinition> getColumnsWithConditions() {
		return conditions.getColumns();
	}

	public boolean hasIfNotExistCondition() {
		return conditions.isIfNotExists();
	}

	public boolean hasIfExistCondition() {
		return conditions.isIfExists();
	}

	public List<ByteBuffer> buildPartitionKeyNames(QueryOptions options) throws InvalidRequestException {
		List<ByteBuffer> partitionKeys = restrictions.getPartitionKeys(options);
		for (ByteBuffer key : partitionKeys)
			QueryProcessor.validateKey(key);

		return partitionKeys;
	}

	public NavigableSet<Clustering> createClustering(QueryOptions options) throws InvalidRequestException {
		if ((appliesOnlyToStaticColumns()) && (!(restrictions.hasClusteringColumnsRestrictions())))
			return FBUtilities.singleton(CBuilder.STATIC_BUILDER.build(), cfm.comparator);

		return restrictions.getClusteringColumns(options);
	}

	private boolean appliesOnlyToStaticColumns() {
		return ModificationStatement.appliesOnlyToStaticColumns(operations, conditions);
	}

	public static boolean appliesOnlyToStaticColumns(Operations operation, Conditions conditions) {
		return ((!(operation.appliesToRegularColumns())) && (!(conditions.appliesToRegularColumns()))) && ((operation.appliesToStaticColumns()) || (conditions.appliesToStaticColumns()));
	}

	public boolean requiresRead() {
		for (Operation op : allOperations())
			if (op.requiresRead())
				return true;


		return false;
	}

	private Map<DecoratedKey, Partition> readRequiredLists(Collection<ByteBuffer> partitionKeys, ClusteringIndexFilter filter, DataLimits limits, boolean local, ConsistencyLevel cl, long queryStartNanoTime) {
		if (!(requiresRead()))
			return null;

		try {
			cl.validateForRead(keyspace());
		} catch (InvalidRequestException e) {
			throw new InvalidRequestException(String.format("Write operation require a read but consistency %s is not supported on reads", cl));
		}
		List<SinglePartitionReadCommand> commands = new ArrayList<>(partitionKeys.size());
		int nowInSec = FBUtilities.nowInSeconds();
		for (ByteBuffer key : partitionKeys)
			commands.add(SinglePartitionReadCommand.create(cfm, nowInSec, ColumnFilter.selection(this.requiresRead), RowFilter.NONE, limits, cfm.decorateKey(key), filter));

		SinglePartitionReadCommand.Group group = new SinglePartitionReadCommand.Group(commands, DataLimits.NONE);
		if (local) {
			try (final ReadExecutionController executionController = group.executionController();final PartitionIterator iter = group.executeInternal(executionController)) {
				return asMaterializedMap(iter);
			}
		}
		try (final PartitionIterator iter = group.execute(cl, null, queryStartNanoTime)) {
			return asMaterializedMap(iter);
		}
	}

	private Map<DecoratedKey, Partition> asMaterializedMap(PartitionIterator iterator) {
		Map<DecoratedKey, Partition> map = new HashMap<>();
		while (iterator.hasNext()) {
			try (final RowIterator partition = iterator.next()) {
				map.put(partition.partitionKey(), FilteredPartition.create(partition));
			}
		} 
		return map;
	}

	public boolean hasConditions() {
		return !(conditions.isEmpty());
	}

	public boolean hasSlices() {
		return ((type.allowClusteringColumnSlices()) && (getRestrictions().hasClusteringColumnsRestrictions())) && (getRestrictions().isColumnRange());
	}

	public ResultMessage execute(QueryState queryState, QueryOptions options, long queryStartNanoTime) throws RequestExecutionException, RequestValidationException {
		if ((options.getConsistency()) == null)
			throw new InvalidRequestException("Invalid empty consistency level");

		return hasConditions() ? executeWithCondition(queryState, options, queryStartNanoTime) : executeWithoutCondition(queryState, options, queryStartNanoTime);
	}

	private ResultMessage executeWithoutCondition(QueryState queryState, QueryOptions options, long queryStartNanoTime) throws RequestExecutionException, RequestValidationException {
		ConsistencyLevel cl = options.getConsistency();
		if (isCounter())
			cl.validateCounterForWrite(cfm);
		else
			cl.validateForWrite(cfm.ksName);

		Collection<? extends IMutation> mutations = getMutations(options, false, options.getTimestamp(queryState), queryStartNanoTime);
		if (!(mutations.isEmpty()))
			StorageProxy.mutateWithTriggers(mutations, cl, false, queryStartNanoTime);

		return null;
	}

	public ResultMessage executeWithCondition(QueryState queryState, QueryOptions options, long queryStartNanoTime) throws RequestExecutionException, RequestValidationException {
		CQL3CasRequest request = makeCasRequest(queryState, options);
		try (final RowIterator result = StorageProxy.cas(keyspace(), columnFamily(), request.key, request, options.getSerialConsistency(), options.getConsistency(), queryState.getClientState(), queryStartNanoTime)) {
			return new ResultMessage.Rows(buildCasResultSet(result, options));
		}
	}

	private CQL3CasRequest makeCasRequest(QueryState queryState, QueryOptions options) {
		List<ByteBuffer> keys = buildPartitionKeyNames(options);
		RequestValidations.checkFalse(restrictions.keyIsInRelation(), "IN on the partition key is not supported with conditional %s", (type.isUpdate() ? "updates" : "deletions"));
		DecoratedKey key = cfm.decorateKey(keys.get(0));
		long now = options.getTimestamp(queryState);
		RequestValidations.checkFalse(restrictions.clusteringKeyRestrictionsHasIN(), "IN on the clustering key columns is not supported with conditional %s", (type.isUpdate() ? "updates" : "deletions"));
		Clustering clustering = Iterables.getOnlyElement(createClustering(options));
		CQL3CasRequest request = new CQL3CasRequest(cfm, key, false, conditionColumns(), updatesRegularRows(), updatesStaticRow());
		addConditions(clustering, request, options);
		return request;
	}

	public void addConditions(Clustering clustering, CQL3CasRequest request, QueryOptions options) throws InvalidRequestException {
		conditions.addConditionsTo(request, clustering, options);
	}

	private ResultSet buildCasResultSet(RowIterator partition, QueryOptions options) throws InvalidRequestException {
		return ModificationStatement.buildCasResultSet(keyspace(), columnFamily(), partition, getColumnsWithConditions(), false, options);
	}

	public static ResultSet buildCasResultSet(String ksName, String tableName, RowIterator partition, Iterable<ColumnDefinition> columnsWithConditions, boolean isBatch, QueryOptions options) throws InvalidRequestException {
		boolean success = partition == null;
		ColumnSpecification spec = new ColumnSpecification(ksName, tableName, ModificationStatement.CAS_RESULT_COLUMN, BooleanType.instance);
		ResultSet.ResultMetadata metadata = new ResultSet.ResultMetadata(Collections.singletonList(spec));
		List<List<ByteBuffer>> rows = Collections.singletonList(Collections.singletonList(BooleanType.instance.decompose(success)));
		ResultSet rs = new ResultSet(metadata, rows);
		return success ? rs : ModificationStatement.merge(rs, ModificationStatement.buildCasFailureResultSet(partition, columnsWithConditions, isBatch, options));
	}

	private static ResultSet merge(ResultSet left, ResultSet right) {
		if ((left.size()) == 0)
			return right;
		else
			if ((right.size()) == 0)
				return left;


		assert (left.size()) == 1;
		int size = (left.metadata.names.size()) + (right.metadata.names.size());
		List<ColumnSpecification> specs = new ArrayList<ColumnSpecification>(size);
		specs.addAll(left.metadata.names);
		specs.addAll(right.metadata.names);
		List<List<ByteBuffer>> rows = new ArrayList<>(right.size());
		for (int i = 0; i < (right.size()); i++) {
			List<ByteBuffer> row = new ArrayList<ByteBuffer>(size);
			row.addAll(left.rows.get(0));
			row.addAll(right.rows.get(i));
			rows.add(row);
		}
		return new ResultSet(new ResultSet.ResultMetadata(specs), rows);
	}

	private static ResultSet buildCasFailureResultSet(RowIterator partition, Iterable<ColumnDefinition> columnsWithConditions, boolean isBatch, QueryOptions options) throws InvalidRequestException {
		CFMetaData cfm = partition.metadata();
		Selection selection;
		if (columnsWithConditions == null) {
			selection = Selection.wildcard(cfm);
		}else {
			Set<ColumnDefinition> defs = new LinkedHashSet<>();
			if (isBatch) {
				defs.addAll(cfm.partitionKeyColumns());
				defs.addAll(cfm.clusteringColumns());
			}
			if ((cfm.isSuper()) && (cfm.isDense())) {
				defs.add(cfm.superColumnValueColumn());
			}else {
				for (ColumnDefinition def : columnsWithConditions)
					defs.add(def);

			}
			selection = Selection.forColumns(cfm, new ArrayList<>(defs));
		}
		Selection.ResultSetBuilder builder = selection.resultSetBuilder(options, false);
		return builder.build();
	}

	public ResultMessage executeInternal(QueryState queryState, QueryOptions options) throws RequestExecutionException, RequestValidationException {
		return hasConditions() ? executeInternalWithCondition(queryState, options) : executeInternalWithoutCondition(queryState, options, System.nanoTime());
	}

	public ResultMessage executeInternalWithoutCondition(QueryState queryState, QueryOptions options, long queryStartNanoTime) throws RequestExecutionException, RequestValidationException {
		for (IMutation mutation : getMutations(options, true, queryState.getTimestamp(), queryStartNanoTime))
			mutation.apply();

		return null;
	}

	public ResultMessage executeInternalWithCondition(QueryState state, QueryOptions options) throws RequestExecutionException, RequestValidationException {
		CQL3CasRequest request = makeCasRequest(state, options);
		try (final RowIterator result = ModificationStatement.casInternal(request, state)) {
			return new ResultMessage.Rows(buildCasResultSet(result, options));
		}
	}

	static RowIterator casInternal(CQL3CasRequest request, QueryState state) {
		UUID ballot = UUIDGen.getTimeUUIDFromMicros(state.getTimestamp());
		SinglePartitionReadCommand readCommand = request.readCommand(FBUtilities.nowInSeconds());
		FilteredPartition current;
		try (final ReadExecutionController executionController = readCommand.executionController();final PartitionIterator iter = readCommand.executeInternal(executionController)) {
			current = FilteredPartition.create(PartitionIterators.getOnlyElement(iter, readCommand));
		}
		if (!(request.appliesTo(current)))
			return current.rowIterator();

		PartitionUpdate updates = request.makeUpdates(current);
		updates = TriggerExecutor.instance.execute(updates);
		Commit proposal = Commit.newProposal(ballot, updates);
		proposal.makeMutation().apply();
		return null;
	}

	private Collection<? extends IMutation> getMutations(QueryOptions options, boolean local, long now, long queryStartNanoTime) {
		return null;
	}

	Slices createSlices(QueryOptions options) {
		SortedSet<ClusteringBound> startBounds = restrictions.getClusteringColumnsBounds(Bound.START, options);
		SortedSet<ClusteringBound> endBounds = restrictions.getClusteringColumnsBounds(Bound.END, options);
		return toSlices(startBounds, endBounds);
	}

	private UpdateParameters makeUpdateParameters(Collection<ByteBuffer> keys, NavigableSet<Clustering> clusterings, QueryOptions options, boolean local, long now, long queryStartNanoTime) {
		if (clusterings.contains(Clustering.STATIC_CLUSTERING))
			return makeUpdateParameters(keys, new ClusteringIndexSliceFilter(Slices.ALL, false), options, DataLimits.cqlLimits(1), local, now, queryStartNanoTime);

		return makeUpdateParameters(keys, new ClusteringIndexNamesFilter(clusterings, false), options, DataLimits.NONE, local, now, queryStartNanoTime);
	}

	private UpdateParameters makeUpdateParameters(Collection<ByteBuffer> keys, ClusteringIndexFilter filter, QueryOptions options, DataLimits limits, boolean local, long now, long queryStartNanoTime) {
		Map<DecoratedKey, Partition> lists = readRequiredLists(keys, filter, limits, local, options.getConsistency(), queryStartNanoTime);
		return new UpdateParameters(cfm, updatedColumns(), options, getTimestamp(now, options), getTimeToLive(options), lists);
	}

	private Slices toSlices(SortedSet<ClusteringBound> startBounds, SortedSet<ClusteringBound> endBounds) {
		assert (startBounds.size()) == (endBounds.size());
		Slices.Builder builder = new Slices.Builder(cfm.comparator);
		Iterator<ClusteringBound> starts = startBounds.iterator();
		Iterator<ClusteringBound> ends = endBounds.iterator();
		while (starts.hasNext()) {
			Slice slice = Slice.make(starts.next(), ends.next());
			if (!(slice.isEmpty(cfm.comparator))) {
				builder.add(slice);
			}
		} 
		return builder.build();
	}

	public abstract static class Parsed extends CFStatement {
		protected final StatementType type;

		private final Attributes.Raw attrs;

		private final List<Pair<ColumnDefinition.Raw, ColumnCondition.Raw>> conditions;

		private final boolean ifNotExists;

		private final boolean ifExists;

		protected Parsed(CFName name, StatementType type, Attributes.Raw attrs, List<Pair<ColumnDefinition.Raw, ColumnCondition.Raw>> conditions, boolean ifNotExists, boolean ifExists) {
			super(name);
			this.type = type;
			this.attrs = attrs;
			this.conditions = (conditions == null) ? Collections.<Pair<ColumnDefinition.Raw, ColumnCondition.Raw>>emptyList() : conditions;
			this.ifNotExists = ifNotExists;
			this.ifExists = ifExists;
		}

		public ParsedStatement.Prepared prepare(ClientState clientState) {
			VariableSpecifications boundNames = getBoundVariables();
			ModificationStatement statement = prepare(boundNames, clientState);
			return new ParsedStatement.Prepared(statement, boundNames, boundNames.getPartitionKeyBindIndexes(statement.cfm));
		}

		public ModificationStatement prepare(VariableSpecifications boundNames, ClientState clientState) {
			CFMetaData metadata = ThriftValidation.validateColumnFamilyWithCompactMode(keyspace(), columnFamily(), clientState.isNoCompactMode());
			Attributes preparedAttributes = attrs.prepare(keyspace(), columnFamily());
			preparedAttributes.collectMarkerSpecification(boundNames);
			Conditions preparedConditions = prepareConditions(metadata, boundNames);
			return prepareInternal(metadata, boundNames, preparedConditions, preparedAttributes);
		}

		private Conditions prepareConditions(CFMetaData metadata, VariableSpecifications boundNames) {
			if (ifExists) {
				assert conditions.isEmpty();
				assert !(ifNotExists);
				return Conditions.IF_EXISTS_CONDITION;
			}
			if (ifNotExists) {
				assert conditions.isEmpty();
				assert !(ifExists);
				return Conditions.IF_NOT_EXISTS_CONDITION;
			}
			if (conditions.isEmpty())
				return Conditions.EMPTY_CONDITION;

			return prepareColumnConditions(metadata, boundNames);
		}

		private ColumnConditions prepareColumnConditions(CFMetaData metadata, VariableSpecifications boundNames) {
			RequestValidations.checkNull(attrs.timestamp, "Cannot provide custom timestamp for conditional updates");
			ColumnConditions.Builder builder = ColumnConditions.newBuilder();
			for (Pair<ColumnDefinition.Raw, ColumnCondition.Raw> entry : conditions) {
				ColumnDefinition def = entry.left.prepare(metadata);
				ColumnCondition condition = entry.right.prepare(keyspace(), def, metadata);
				condition.collectMarkerSpecification(boundNames);
				RequestValidations.checkFalse(def.isPrimaryKeyColumn(), "PRIMARY KEY column '%s' cannot have IF conditions", def.name);
				builder.add(condition);
			}
			return builder.build();
		}

		protected abstract ModificationStatement prepareInternal(CFMetaData cfm, VariableSpecifications boundNames, Conditions conditions, Attributes attrs);

		protected StatementRestrictions newRestrictions(CFMetaData cfm, VariableSpecifications boundNames, Operations operations, WhereClause where, Conditions conditions) {
			if (where.containsCustomExpressions())
				throw new InvalidRequestException(ModificationStatement.CUSTOM_EXPRESSIONS_NOT_ALLOWED);

			boolean applyOnlyToStaticColumns = ModificationStatement.appliesOnlyToStaticColumns(operations, conditions);
			return new StatementRestrictions(type, cfm, where, boundNames, applyOnlyToStaticColumns, false, false, false);
		}

		protected static ColumnDefinition getColumnDefinition(CFMetaData cfm, ColumnDefinition.Raw rawId) {
			return rawId.prepare(cfm);
		}
	}
}

