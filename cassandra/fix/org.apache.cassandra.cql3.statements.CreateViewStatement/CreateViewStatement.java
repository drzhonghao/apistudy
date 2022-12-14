

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.cassandra.auth.Permission;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.config.ColumnDefinition.Raw;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.config.ViewDefinition;
import org.apache.cassandra.cql3.CFName;
import org.apache.cassandra.cql3.CQLStatement;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.cql3.Relation;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.cql3.VariableSpecifications;
import org.apache.cassandra.cql3.WhereClause;
import org.apache.cassandra.cql3.restrictions.StatementRestrictions;
import org.apache.cassandra.cql3.selection.RawSelector;
import org.apache.cassandra.cql3.selection.Selectable;
import org.apache.cassandra.cql3.statements.CFProperties;
import org.apache.cassandra.cql3.statements.CFStatement;
import org.apache.cassandra.cql3.statements.ParsedStatement;
import org.apache.cassandra.cql3.statements.SchemaAlteringStatement;
import org.apache.cassandra.cql3.statements.SelectStatement;
import org.apache.cassandra.cql3.statements.TableAttributes;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.DurationType;
import org.apache.cassandra.db.view.View;
import org.apache.cassandra.exceptions.AlreadyExistsException;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.exceptions.RequestValidationException;
import org.apache.cassandra.exceptions.UnauthorizedException;
import org.apache.cassandra.schema.TableParams;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.service.ClientWarn;
import org.apache.cassandra.service.MigrationManager;
import org.apache.cassandra.service.QueryState;
import org.apache.cassandra.thrift.ThriftValidation;
import org.apache.cassandra.transport.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.config.CFMetaData.Builder.createView;
import static org.apache.cassandra.transport.Event.SchemaChange.Change.CREATED;
import static org.apache.cassandra.transport.Event.SchemaChange.Target.TABLE;


public class CreateViewStatement extends SchemaAlteringStatement {
	private static final Logger logger = LoggerFactory.getLogger(CreateViewStatement.class);

	private final CFName baseName;

	private final List<RawSelector> selectClause;

	private final WhereClause whereClause;

	private final List<ColumnDefinition.Raw> partitionKeys;

	private final List<ColumnDefinition.Raw> clusteringKeys;

	public final CFProperties properties = new CFProperties();

	private final boolean ifNotExists;

	public CreateViewStatement(CFName viewName, CFName baseName, List<RawSelector> selectClause, WhereClause whereClause, List<ColumnDefinition.Raw> partitionKeys, List<ColumnDefinition.Raw> clusteringKeys, boolean ifNotExists) {
		super(viewName);
		this.baseName = baseName;
		this.selectClause = selectClause;
		this.whereClause = whereClause;
		this.partitionKeys = partitionKeys;
		this.clusteringKeys = clusteringKeys;
		this.ifNotExists = ifNotExists;
	}

	public void checkAccess(ClientState state) throws InvalidRequestException, UnauthorizedException {
		if (!(baseName.hasKeyspace()))
			baseName.setKeyspace(keyspace(), true);

		state.hasColumnFamilyAccess(keyspace(), baseName.getColumnFamily(), Permission.ALTER);
	}

	public void validate(ClientState state) throws RequestValidationException {
	}

	private interface AddColumn {
		void add(ColumnIdentifier identifier, AbstractType<?> type);
	}

	private void add(CFMetaData baseCfm, Iterable<ColumnIdentifier> columns, CreateViewStatement.AddColumn adder) {
		for (ColumnIdentifier column : columns) {
			AbstractType<?> type = baseCfm.getColumnDefinition(column).type;
			adder.add(column, type);
		}
	}

	public Event.SchemaChange announceMigration(QueryState queryState, boolean isLocalOnly) throws RequestValidationException {
		if (!(DatabaseDescriptor.enableMaterializedViews())) {
			throw new InvalidRequestException("Materialized views are disabled. Enable in cassandra.yaml to use.");
		}
		properties.validate();
		if (!(baseName.getKeyspace().equals(keyspace())))
			throw new InvalidRequestException("Cannot create a materialized view on a table in a separate keyspace");

		CFMetaData cfm = ThriftValidation.validateColumnFamily(baseName.getKeyspace(), baseName.getColumnFamily());
		if (cfm.isCounter())
			throw new InvalidRequestException("Materialized views are not supported on counter tables");

		if (cfm.isSuper())
			throw new InvalidRequestException("Materialized views are not supported on SuperColumn tables");

		if (cfm.isView())
			throw new InvalidRequestException("Materialized views cannot be created against other materialized views");

		if ((cfm.params.gcGraceSeconds) == 0) {
			throw new InvalidRequestException(String.format(("Cannot create materialized view '%s' for base table " + ((("'%s' with gc_grace_seconds of 0, since this value is " + "used to TTL undelivered updates. Setting gc_grace_seconds") + " too low might cause undelivered updates to expire ") + "before being replayed.")), cfName.getColumnFamily(), baseName.getColumnFamily()));
		}
		Set<ColumnIdentifier> included = Sets.newHashSetWithExpectedSize(selectClause.size());
		for (RawSelector selector : selectClause) {
			Selectable.Raw selectable = selector.selectable;
			if (selectable instanceof Selectable.WithFieldSelection.Raw)
				throw new InvalidRequestException("Cannot select out a part of type when defining a materialized view");

			if (selectable instanceof Selectable.WithFunction.Raw)
				throw new InvalidRequestException("Cannot use function when defining a materialized view");

			if (selectable instanceof Selectable.WritetimeOrTTL.Raw)
				throw new InvalidRequestException("Cannot use function when defining a materialized view");

			if ((selector.alias) != null)
				throw new InvalidRequestException("Cannot use alias when defining a materialized view");

			Selectable s = selectable.prepare(cfm);
			if (s instanceof Term.Raw)
				throw new InvalidRequestException("Cannot use terms in selection when defining a materialized view");

			ColumnDefinition cdef = ((ColumnDefinition) (s));
			included.add(cdef.name);
		}
		Set<ColumnDefinition.Raw> targetPrimaryKeys = new HashSet<>();
		for (ColumnDefinition.Raw identifier : Iterables.concat(partitionKeys, clusteringKeys)) {
			if (!(targetPrimaryKeys.add(identifier)))
				throw new InvalidRequestException(("Duplicate entry found in PRIMARY KEY: " + identifier));

			ColumnDefinition cdef = identifier.prepare(cfm);
			if (cdef.type.isMultiCell())
				throw new InvalidRequestException(String.format("Cannot use MultiCell column '%s' in PRIMARY KEY of materialized view", identifier));

			if (cdef.isStatic())
				throw new InvalidRequestException(String.format("Cannot use Static column '%s' in PRIMARY KEY of materialized view", identifier));

			if ((cdef.type) instanceof DurationType)
				throw new InvalidRequestException(String.format("Cannot use Duration column '%s' in PRIMARY KEY of materialized view", identifier));

		}
		Map<ColumnDefinition.Raw, Boolean> orderings = Collections.emptyMap();
		List<ColumnDefinition.Raw> groups = Collections.emptyList();
		SelectStatement.Parameters parameters = new SelectStatement.Parameters(orderings, groups, false, true, false);
		SelectStatement.RawStatement rawSelect = new SelectStatement.RawStatement(baseName, parameters, selectClause, whereClause, null, null);
		ClientState state = ClientState.forInternalCalls();
		state.setKeyspace(keyspace());
		rawSelect.prepareKeyspace(state);
		rawSelect.setBoundVariables(getBoundVariables());
		ParsedStatement.Prepared prepared = rawSelect.prepare(true, queryState.getClientState());
		SelectStatement select = ((SelectStatement) (prepared.statement));
		StatementRestrictions restrictions = select.getRestrictions();
		if (!(prepared.boundNames.isEmpty()))
			throw new InvalidRequestException("Cannot use query parameters in CREATE MATERIALIZED VIEW statements");

		final boolean allowFilteringNonKeyColumns = Boolean.parseBoolean(System.getProperty("cassandra.mv.allow_filtering_nonkey_columns_unsafe", "false"));
		if ((!(restrictions.nonPKRestrictedColumns(false).isEmpty())) && (!allowFilteringNonKeyColumns)) {
			throw new InvalidRequestException(String.format(("Non-primary key columns cannot be restricted in the SELECT statement used" + " for materialized view creation (got restrictions on: %s)"), restrictions.nonPKRestrictedColumns(false).stream().map(( def) -> def.name.toString()).collect(Collectors.joining(", "))));
		}
		String whereClauseText = View.relationsToWhereClause(whereClause.relations);
		Set<ColumnIdentifier> basePrimaryKeyCols = new HashSet<>();
		for (ColumnDefinition definition : Iterables.concat(cfm.partitionKeyColumns(), cfm.clusteringColumns()))
			basePrimaryKeyCols.add(definition.name);

		List<ColumnIdentifier> targetClusteringColumns = new ArrayList<>();
		List<ColumnIdentifier> targetPartitionKeys = new ArrayList<>();
		boolean hasNonPKColumn = false;
		for (ColumnDefinition.Raw raw : partitionKeys)
			hasNonPKColumn |= CreateViewStatement.getColumnIdentifier(cfm, basePrimaryKeyCols, hasNonPKColumn, raw, targetPartitionKeys, restrictions);

		for (ColumnDefinition.Raw raw : clusteringKeys)
			hasNonPKColumn |= CreateViewStatement.getColumnIdentifier(cfm, basePrimaryKeyCols, hasNonPKColumn, raw, targetClusteringColumns, restrictions);

		boolean missingClusteringColumns = false;
		StringBuilder columnNames = new StringBuilder();
		List<ColumnIdentifier> includedColumns = new ArrayList<>();
		for (ColumnDefinition def : cfm.allColumns()) {
			ColumnIdentifier identifier = def.name;
			boolean includeDef = (included.isEmpty()) || (included.contains(identifier));
			if (includeDef && (def.isStatic())) {
				throw new InvalidRequestException(String.format("Unable to include static column '%s' which would be included by Materialized View SELECT * statement", identifier));
			}
			boolean defInTargetPrimaryKey = (targetClusteringColumns.contains(identifier)) || (targetPartitionKeys.contains(identifier));
			if (includeDef && (!defInTargetPrimaryKey)) {
				includedColumns.add(identifier);
			}
			if (!(def.isPrimaryKeyColumn()))
				continue;

			if (!defInTargetPrimaryKey) {
				if (missingClusteringColumns)
					columnNames.append(',');
				else
					missingClusteringColumns = true;

				columnNames.append(identifier);
			}
		}
		if (missingClusteringColumns)
			throw new InvalidRequestException(String.format("Cannot create Materialized View %s without primary key columns from base %s (%s)", columnFamily(), baseName.getColumnFamily(), columnNames.toString()));

		if (targetPartitionKeys.isEmpty())
			throw new InvalidRequestException("Must select at least a column for a Materialized View");

		if (targetClusteringColumns.isEmpty())
			throw new InvalidRequestException("No columns are defined for Materialized View other than primary key");

		TableParams params = properties.properties.asNewTableParams();
		if ((params.defaultTimeToLive) > 0) {
			throw new InvalidRequestException(("Cannot set default_time_to_live for a materialized view. " + ("Data in a materialized view always expire at the same time than " + "the corresponding data in the parent table.")));
		}
		CFMetaData.Builder cfmBuilder = createView(keyspace(), columnFamily());
		add(cfm, targetPartitionKeys, cfmBuilder::addPartitionKey);
		add(cfm, targetClusteringColumns, cfmBuilder::addClusteringColumn);
		add(cfm, includedColumns, cfmBuilder::addRegularColumn);
		cfmBuilder.withId(properties.properties.getId());
		CFMetaData viewCfm = cfmBuilder.build().params(params);
		ViewDefinition definition = new ViewDefinition(keyspace(), columnFamily(), Schema.instance.getId(keyspace(), baseName.getColumnFamily()), baseName.getColumnFamily(), included.isEmpty(), rawSelect, whereClauseText, viewCfm);
		CreateViewStatement.logger.warn(("Creating materialized view {} for {}.{}. " + "Materialized views are experimental and are not recommended for production use."), definition.viewName, cfm.ksName, cfm.cfName);
		try {
			ClientWarn.instance.warn("Materialized views are experimental and are not recommended for production use.");
			MigrationManager.announceNewView(definition, isLocalOnly);
			return new Event.SchemaChange(CREATED, TABLE, keyspace(), columnFamily());
		} catch (AlreadyExistsException e) {
			if (ifNotExists)
				return null;

			throw e;
		}
	}

	private static boolean getColumnIdentifier(CFMetaData cfm, Set<ColumnIdentifier> basePK, boolean hasNonPKColumn, ColumnDefinition.Raw raw, List<ColumnIdentifier> columns, StatementRestrictions restrictions) {
		ColumnDefinition def = raw.prepare(cfm);
		boolean isPk = basePK.contains(def.name);
		if ((!isPk) && hasNonPKColumn)
			throw new InvalidRequestException(String.format("Cannot include more than one non-primary key column '%s' in materialized view primary key", def.name));

		boolean isSinglePartitionKey = (def.isPartitionKey()) && ((cfm.partitionKeyColumns().size()) == 1);
		if ((!isSinglePartitionKey) && (!(restrictions.isRestricted(def))))
			throw new InvalidRequestException(String.format("Primary key column '%s' is required to be filtered by 'IS NOT NULL'", def.name));

		columns.add(def.name);
		return !isPk;
	}
}

