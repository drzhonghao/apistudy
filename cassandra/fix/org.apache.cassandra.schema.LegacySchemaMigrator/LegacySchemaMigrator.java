

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.config.SchemaConstants;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.FieldIdentifier;
import org.apache.cassandra.cql3.QueryProcessor;
import org.apache.cassandra.cql3.SuperColumnCompatibility;
import org.apache.cassandra.cql3.UntypedResultSet;
import org.apache.cassandra.cql3.functions.FunctionName;
import org.apache.cassandra.cql3.functions.UDAggregate;
import org.apache.cassandra.cql3.functions.UDFunction;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.CompactTables;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.SystemKeyspace;
import org.apache.cassandra.db.compaction.AbstractCompactionStrategy;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.CollectionType;
import org.apache.cassandra.db.marshal.ColumnToCollectionType;
import org.apache.cassandra.db.marshal.CompositeType;
import org.apache.cassandra.db.marshal.CounterColumnType;
import org.apache.cassandra.db.marshal.EmptyType;
import org.apache.cassandra.db.marshal.LongType;
import org.apache.cassandra.db.marshal.MapType;
import org.apache.cassandra.db.marshal.TypeParser;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.cassandra.db.marshal.UserType;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.schema.CachingParams;
import org.apache.cassandra.schema.CompactionParams;
import org.apache.cassandra.schema.CompressionParams;
import org.apache.cassandra.schema.Functions;
import org.apache.cassandra.schema.IndexMetadata;
import org.apache.cassandra.schema.Indexes;
import org.apache.cassandra.schema.KeyspaceMetadata;
import org.apache.cassandra.schema.KeyspaceParams;
import org.apache.cassandra.schema.ReplicationParams;
import org.apache.cassandra.schema.SchemaKeyspace;
import org.apache.cassandra.schema.SpeculativeRetryParam;
import org.apache.cassandra.schema.TableParams;
import org.apache.cassandra.schema.Tables;
import org.apache.cassandra.schema.TriggerMetadata;
import org.apache.cassandra.schema.Triggers;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.FBUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.config.ColumnDefinition.Kind.CLUSTERING;
import static org.apache.cassandra.config.ColumnDefinition.Kind.REGULAR;
import static org.apache.cassandra.config.ColumnDefinition.Kind.STATIC;
import static org.apache.cassandra.schema.CompactionParams.Option.MAX_THRESHOLD;
import static org.apache.cassandra.schema.CompactionParams.Option.MIN_THRESHOLD;
import static org.apache.cassandra.schema.IndexMetadata.Kind.valueOf;


@SuppressWarnings("deprecation")
public final class LegacySchemaMigrator {
	private LegacySchemaMigrator() {
	}

	private static final Logger logger = LoggerFactory.getLogger(LegacySchemaMigrator.class);

	static final List<CFMetaData> LegacySchemaTables = ImmutableList.of(SystemKeyspace.LegacyKeyspaces, SystemKeyspace.LegacyColumnfamilies, SystemKeyspace.LegacyColumns, SystemKeyspace.LegacyTriggers, SystemKeyspace.LegacyUsertypes, SystemKeyspace.LegacyFunctions, SystemKeyspace.LegacyAggregates);

	public static void migrate() {
		Collection<LegacySchemaMigrator.Keyspace> keyspaces = LegacySchemaMigrator.readSchema();
		if (keyspaces.isEmpty()) {
			LegacySchemaMigrator.unloadLegacySchemaTables();
			return;
		}
		LegacySchemaMigrator.logger.info("Moving {} keyspaces from legacy schema tables to the new schema keyspace ({})", keyspaces.size(), SchemaConstants.SCHEMA_KEYSPACE_NAME);
		keyspaces.forEach(LegacySchemaMigrator::storeKeyspaceInNewSchemaTables);
		keyspaces.forEach(LegacySchemaMigrator::migrateBuiltIndexesForKeyspace);
		LegacySchemaMigrator.logger.info("Truncating legacy schema tables");
		LegacySchemaMigrator.truncateLegacySchemaTables();
		LegacySchemaMigrator.unloadLegacySchemaTables();
		LegacySchemaMigrator.logger.info("Completed migration of legacy schema tables");
	}

	private static void migrateBuiltIndexesForKeyspace(LegacySchemaMigrator.Keyspace keyspace) {
		keyspace.tables.forEach(LegacySchemaMigrator::migrateBuiltIndexesForTable);
	}

	private static void migrateBuiltIndexesForTable(LegacySchemaMigrator.Table table) {
		table.metadata.getIndexes().forEach(( index) -> LegacySchemaMigrator.migrateIndexBuildStatus(table.metadata.ksName, table.metadata.cfName, index));
	}

	private static void migrateIndexBuildStatus(String keyspace, String table, IndexMetadata index) {
		if (SystemKeyspace.isIndexBuilt(keyspace, ((table + '.') + (index.name)))) {
			SystemKeyspace.setIndexBuilt(keyspace, index.name);
			SystemKeyspace.setIndexRemoved(keyspace, ((table + '.') + (index.name)));
		}
	}

	static void unloadLegacySchemaTables() {
		KeyspaceMetadata systemKeyspace = Schema.instance.getKSMetaData(SchemaConstants.SYSTEM_KEYSPACE_NAME);
		Tables systemTables = systemKeyspace.tables;
		for (CFMetaData table : LegacySchemaMigrator.LegacySchemaTables)
			systemTables = systemTables.without(table.cfName);

		LegacySchemaMigrator.LegacySchemaTables.forEach(Schema.instance::unload);
		Schema.instance.setKeyspaceMetadata(systemKeyspace.withSwapped(systemTables));
	}

	private static void truncateLegacySchemaTables() {
		LegacySchemaMigrator.LegacySchemaTables.forEach(( table) -> Schema.instance.getColumnFamilyStoreInstance(table.cfId).truncateBlocking());
	}

	private static void storeKeyspaceInNewSchemaTables(LegacySchemaMigrator.Keyspace keyspace) {
		LegacySchemaMigrator.logger.info("Migrating keyspace {}", keyspace);
		Mutation.SimpleBuilder builder = SchemaKeyspace.makeCreateKeyspaceMutation(keyspace.name, keyspace.params, keyspace.timestamp);
		for (LegacySchemaMigrator.Table table : keyspace.tables) {
		}
		for (LegacySchemaMigrator.Type type : keyspace.types) {
		}
		for (LegacySchemaMigrator.Function function : keyspace.functions) {
		}
		for (LegacySchemaMigrator.Aggregate aggregate : keyspace.aggregates) {
		}
		builder.build().apply();
	}

	private static Collection<LegacySchemaMigrator.Keyspace> readSchema() {
		String query = String.format("SELECT keyspace_name FROM %s.%s", SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.LEGACY_KEYSPACES);
		Collection<String> keyspaceNames = new ArrayList<>();
		LegacySchemaMigrator.query(query).forEach(( row) -> keyspaceNames.add(row.getString("keyspace_name")));
		keyspaceNames.removeAll(SchemaConstants.LOCAL_SYSTEM_KEYSPACE_NAMES);
		Collection<LegacySchemaMigrator.Keyspace> keyspaces = new ArrayList<>();
		keyspaceNames.forEach(( name) -> keyspaces.add(LegacySchemaMigrator.readKeyspace(name)));
		return keyspaces;
	}

	private static LegacySchemaMigrator.Keyspace readKeyspace(String keyspaceName) {
		long timestamp = LegacySchemaMigrator.readKeyspaceTimestamp(keyspaceName);
		KeyspaceParams params = LegacySchemaMigrator.readKeyspaceParams(keyspaceName);
		Collection<LegacySchemaMigrator.Table> tables = LegacySchemaMigrator.readTables(keyspaceName);
		Collection<LegacySchemaMigrator.Type> types = LegacySchemaMigrator.readTypes(keyspaceName);
		Collection<LegacySchemaMigrator.Function> functions = LegacySchemaMigrator.readFunctions(keyspaceName);
		Functions.Builder functionsBuilder = Functions.builder();
		functions.forEach(( udf) -> functionsBuilder.add(udf.metadata));
		Collection<LegacySchemaMigrator.Aggregate> aggregates = LegacySchemaMigrator.readAggregates(functionsBuilder.build(), keyspaceName);
		return new LegacySchemaMigrator.Keyspace(timestamp, keyspaceName, params, tables, types, functions, aggregates);
	}

	private static long readKeyspaceTimestamp(String keyspaceName) {
		String query = String.format("SELECT writeTime(durable_writes) AS timestamp FROM %s.%s WHERE keyspace_name = ?", SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.LEGACY_KEYSPACES);
		return LegacySchemaMigrator.query(query, keyspaceName).one().getLong("timestamp");
	}

	private static KeyspaceParams readKeyspaceParams(String keyspaceName) {
		String query = String.format("SELECT * FROM %s.%s WHERE keyspace_name = ?", SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.LEGACY_KEYSPACES);
		UntypedResultSet.Row row = LegacySchemaMigrator.query(query, keyspaceName).one();
		boolean durableWrites = row.getBoolean("durable_writes");
		Map<String, String> replication = new HashMap<>();
		replication.putAll(FBUtilities.fromJsonMap(row.getString("strategy_options")));
		replication.put(ReplicationParams.CLASS, row.getString("strategy_class"));
		return KeyspaceParams.create(durableWrites, replication);
	}

	private static Collection<LegacySchemaMigrator.Table> readTables(String keyspaceName) {
		String query = String.format("SELECT columnfamily_name FROM %s.%s WHERE keyspace_name = ?", SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.LEGACY_COLUMNFAMILIES);
		Collection<String> tableNames = new ArrayList<>();
		LegacySchemaMigrator.query(query, keyspaceName).forEach(( row) -> tableNames.add(row.getString("columnfamily_name")));
		Collection<LegacySchemaMigrator.Table> tables = new ArrayList<>();
		tableNames.forEach(( name) -> tables.add(LegacySchemaMigrator.readTable(keyspaceName, name)));
		return tables;
	}

	private static LegacySchemaMigrator.Table readTable(String keyspaceName, String tableName) {
		long timestamp = LegacySchemaMigrator.readTableTimestamp(keyspaceName, tableName);
		CFMetaData metadata = LegacySchemaMigrator.readTableMetadata(keyspaceName, tableName);
		return new LegacySchemaMigrator.Table(timestamp, metadata);
	}

	private static long readTableTimestamp(String keyspaceName, String tableName) {
		String query = String.format("SELECT writeTime(type) AS timestamp FROM %s.%s WHERE keyspace_name = ? AND columnfamily_name = ?", SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.LEGACY_COLUMNFAMILIES);
		return LegacySchemaMigrator.query(query, keyspaceName, tableName).one().getLong("timestamp");
	}

	private static CFMetaData readTableMetadata(String keyspaceName, String tableName) {
		String tableQuery = String.format("SELECT * FROM %s.%s WHERE keyspace_name = ? AND columnfamily_name = ?", SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.LEGACY_COLUMNFAMILIES);
		UntypedResultSet.Row tableRow = LegacySchemaMigrator.query(tableQuery, keyspaceName, tableName).one();
		String columnsQuery = String.format("SELECT * FROM %s.%s WHERE keyspace_name = ? AND columnfamily_name = ?", SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.LEGACY_COLUMNS);
		UntypedResultSet columnRows = LegacySchemaMigrator.query(columnsQuery, keyspaceName, tableName);
		String triggersQuery = String.format("SELECT * FROM %s.%s WHERE keyspace_name = ? AND columnfamily_name = ?", SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.LEGACY_TRIGGERS);
		UntypedResultSet triggerRows = LegacySchemaMigrator.query(triggersQuery, keyspaceName, tableName);
		return LegacySchemaMigrator.decodeTableMetadata(tableName, tableRow, columnRows, triggerRows);
	}

	private static CFMetaData decodeTableMetadata(String tableName, UntypedResultSet.Row tableRow, UntypedResultSet columnRows, UntypedResultSet triggerRows) {
		String ksName = tableRow.getString("keyspace_name");
		String cfName = tableRow.getString("columnfamily_name");
		AbstractType<?> rawComparator = TypeParser.parse(tableRow.getString("comparator"));
		AbstractType<?> subComparator = (tableRow.has("subcomparator")) ? TypeParser.parse(tableRow.getString("subcomparator")) : null;
		boolean isSuper = "super".equals(tableRow.getString("type").toLowerCase(Locale.ENGLISH));
		boolean isCompound = (rawComparator instanceof CompositeType) || isSuper;
		Boolean rawIsDense = (tableRow.has("is_dense")) ? tableRow.getBoolean("is_dense") : null;
		boolean isDense;
		if ((rawIsDense != null) && (!rawIsDense))
			isDense = false;
		else
			isDense = LegacySchemaMigrator.calculateIsDense(rawComparator, columnRows, isSuper);

		Iterable<UntypedResultSet.Row> filteredColumnRows = ((!isDense) && ((rawIsDense == null) || rawIsDense)) ? LegacySchemaMigrator.filterOutRedundantRowsForSparse(columnRows, isSuper, isCompound) : columnRows;
		AbstractType<?> defaultValidator = TypeParser.parse(tableRow.getString("default_validator"));
		boolean isCounter = defaultValidator instanceof CounterColumnType;
		UUID cfId = (tableRow.has("cf_id")) ? tableRow.getUUID("cf_id") : CFMetaData.generateLegacyCfId(ksName, cfName);
		boolean isCQLTable = ((!isSuper) && (!isDense)) && isCompound;
		boolean isStaticCompactTable = (!isDense) && (!isCompound);
		boolean needsUpgrade = (!isCQLTable) && (LegacySchemaMigrator.checkNeedsUpgrade(filteredColumnRows, isSuper, isStaticCompactTable));
		List<ColumnDefinition> columnDefs = LegacySchemaMigrator.createColumnsFromColumnRows(filteredColumnRows, ksName, cfName, rawComparator, subComparator, isSuper, isCQLTable, isStaticCompactTable, needsUpgrade);
		if (needsUpgrade) {
			LegacySchemaMigrator.addDefinitionForUpgrade(columnDefs, ksName, cfName, isStaticCompactTable, isSuper, rawComparator, subComparator, defaultValidator);
		}
		CFMetaData cfm = CFMetaData.create(ksName, cfName, cfId, isDense, isCompound, isSuper, isCounter, false, columnDefs, DatabaseDescriptor.getPartitioner());
		Indexes indexes = LegacySchemaMigrator.createIndexesFromColumnRows(cfm, filteredColumnRows, ksName, cfName, rawComparator, subComparator, isSuper, isCQLTable, isStaticCompactTable, needsUpgrade);
		cfm.indexes(indexes);
		if (tableRow.has("dropped_columns"))
			LegacySchemaMigrator.addDroppedColumns(cfm, rawComparator, tableRow.getMap("dropped_columns", UTF8Type.instance, LongType.instance));

		return cfm.params(LegacySchemaMigrator.decodeTableParams(tableRow)).triggers(LegacySchemaMigrator.createTriggersFromTriggerRows(triggerRows));
	}

	private static boolean calculateIsDense(AbstractType<?> comparator, UntypedResultSet columnRows, boolean isSuper) {
		for (UntypedResultSet.Row columnRow : columnRows) {
			if ("regular".equals(columnRow.getString("type")))
				return false;

		}
		if (isSuper)
			return true;

		int maxClusteringIdx = -1;
		for (UntypedResultSet.Row columnRow : columnRows)
			if ("clustering_key".equals(columnRow.getString("type")))
				maxClusteringIdx = Math.max(maxClusteringIdx, (columnRow.has("component_index") ? columnRow.getInt("component_index") : 0));


		return maxClusteringIdx >= 0 ? maxClusteringIdx == ((comparator.componentsCount()) - 1) : !(LegacySchemaMigrator.isCQL3OnlyPKComparator(comparator));
	}

	private static Iterable<UntypedResultSet.Row> filterOutRedundantRowsForSparse(UntypedResultSet columnRows, boolean isSuper, boolean isCompound) {
		Collection<UntypedResultSet.Row> filteredRows = new ArrayList<>();
		for (UntypedResultSet.Row columnRow : columnRows) {
			String kind = columnRow.getString("type");
			if ((!isSuper) && ("compact_value".equals(kind)))
				continue;

			if ((("clustering_key".equals(kind)) && (!isSuper)) && (!isCompound))
				continue;

			filteredRows.add(columnRow);
		}
		return filteredRows;
	}

	private static boolean isCQL3OnlyPKComparator(AbstractType<?> comparator) {
		if (!(comparator instanceof CompositeType))
			return false;

		CompositeType ct = ((CompositeType) (comparator));
		return ((ct.types.size()) == 1) && ((ct.types.get(0)) instanceof UTF8Type);
	}

	private static TableParams decodeTableParams(UntypedResultSet.Row row) {
		TableParams.Builder params = TableParams.builder();
		params.readRepairChance(row.getDouble("read_repair_chance")).dcLocalReadRepairChance(row.getDouble("local_read_repair_chance")).gcGraceSeconds(row.getInt("gc_grace_seconds"));
		if (row.has("comment"))
			params.comment(row.getString("comment"));

		if (row.has("memtable_flush_period_in_ms"))
			params.memtableFlushPeriodInMs(row.getInt("memtable_flush_period_in_ms"));

		params.caching(LegacySchemaMigrator.cachingFromRow(row.getString("caching")));
		if (row.has("default_time_to_live"))
			params.defaultTimeToLive(row.getInt("default_time_to_live"));

		if (row.has("speculative_retry"))
			params.speculativeRetry(SpeculativeRetryParam.fromString(row.getString("speculative_retry")));

		Map<String, String> compressionParameters = FBUtilities.fromJsonMap(row.getString("compression_parameters"));
		String crcCheckChance = compressionParameters.remove("crc_check_chance");
		if (crcCheckChance != null)
			params.crcCheckChance(Double.parseDouble(crcCheckChance));

		params.compression(CompressionParams.fromMap(compressionParameters));
		params.compaction(LegacySchemaMigrator.compactionFromRow(row));
		if (row.has("min_index_interval"))
			params.minIndexInterval(row.getInt("min_index_interval"));

		if (row.has("max_index_interval"))
			params.maxIndexInterval(row.getInt("max_index_interval"));

		if (row.has("bloom_filter_fp_chance"))
			params.bloomFilterFpChance(row.getDouble("bloom_filter_fp_chance"));

		return params.build();
	}

	@com.google.common.annotations.VisibleForTesting
	public static CachingParams cachingFromRow(String caching) {
		switch (caching) {
			case "NONE" :
				return CachingParams.CACHE_NOTHING;
			case "KEYS_ONLY" :
				return CachingParams.CACHE_KEYS;
			case "ROWS_ONLY" :
				return new CachingParams(false, Integer.MAX_VALUE);
			case "ALL" :
				return CachingParams.CACHE_EVERYTHING;
			default :
				return CachingParams.fromMap(FBUtilities.fromJsonMap(caching));
		}
	}

	@SuppressWarnings("unchecked")
	private static CompactionParams compactionFromRow(UntypedResultSet.Row row) {
		Class<? extends AbstractCompactionStrategy> klass = CFMetaData.createCompactionStrategy(row.getString("compaction_strategy_class"));
		Map<String, String> options = FBUtilities.fromJsonMap(row.getString("compaction_strategy_options"));
		int minThreshold = row.getInt("min_compaction_threshold");
		int maxThreshold = row.getInt("max_compaction_threshold");
		Map<String, String> optionsWithThresholds = new HashMap<>(options);
		optionsWithThresholds.putIfAbsent(MIN_THRESHOLD.toString(), Integer.toString(minThreshold));
		optionsWithThresholds.putIfAbsent(MAX_THRESHOLD.toString(), Integer.toString(maxThreshold));
		try {
			Map<String, String> unrecognizedOptions = ((Map<String, String>) (klass.getMethod("validateOptions", Map.class).invoke(null, optionsWithThresholds)));
			if (unrecognizedOptions.isEmpty())
				options = optionsWithThresholds;

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return CompactionParams.create(klass, options);
	}

	private static boolean checkNeedsUpgrade(Iterable<UntypedResultSet.Row> defs, boolean isSuper, boolean isStaticCompactTable) {
		if (isSuper)
			return true;

		if (isStaticCompactTable)
			return !(LegacySchemaMigrator.hasKind(defs, STATIC));

		return !(LegacySchemaMigrator.hasRegularColumns(defs));
	}

	private static boolean hasRegularColumns(Iterable<UntypedResultSet.Row> columnRows) {
		for (UntypedResultSet.Row row : columnRows) {
			if (LegacySchemaMigrator.isEmptyCompactValueColumn(row))
				return false;

			if ((LegacySchemaMigrator.deserializeKind(row.getString("type"))) == (REGULAR))
				return true;

		}
		return false;
	}

	private static boolean isEmptyCompactValueColumn(UntypedResultSet.Row row) {
		return ("compact_value".equals(row.getString("type"))) && (row.getString("column_name").isEmpty());
	}

	private static void addDefinitionForUpgrade(List<ColumnDefinition> defs, String ksName, String cfName, boolean isStaticCompactTable, boolean isSuper, AbstractType<?> rawComparator, AbstractType<?> subComparator, AbstractType<?> defaultValidator) {
		CompactTables.DefaultNames names = CompactTables.defaultNameGenerator(defs);
		if (isSuper) {
			defs.add(ColumnDefinition.regularDef(ksName, cfName, SuperColumnCompatibility.SUPER_COLUMN_MAP_COLUMN_STR, MapType.getInstance(subComparator, defaultValidator, true)));
		}else
			if (isStaticCompactTable) {
				defs.add(ColumnDefinition.clusteringDef(ksName, cfName, names.defaultClusteringName(), rawComparator, 0));
				defs.add(ColumnDefinition.regularDef(ksName, cfName, names.defaultCompactValueName(), defaultValidator));
			}else {
				defs.add(ColumnDefinition.regularDef(ksName, cfName, names.defaultCompactValueName(), EmptyType.instance));
			}

	}

	private static boolean hasKind(Iterable<UntypedResultSet.Row> defs, ColumnDefinition.Kind kind) {
		for (UntypedResultSet.Row row : defs)
			if ((LegacySchemaMigrator.deserializeKind(row.getString("type"))) == kind)
				return true;


		return false;
	}

	private static void addDroppedColumns(CFMetaData cfm, AbstractType<?> comparator, Map<String, Long> droppedTimes) {
		AbstractType<?> last = comparator.getComponents().get(((comparator.componentsCount()) - 1));
		Map<ByteBuffer, CollectionType> collections = (last instanceof ColumnToCollectionType) ? ((ColumnToCollectionType) (last)).defined : Collections.emptyMap();
		for (Map.Entry<String, Long> entry : droppedTimes.entrySet()) {
			String name = entry.getKey();
			ByteBuffer nameBytes = UTF8Type.instance.decompose(name);
			long time = entry.getValue();
			AbstractType<?> type = (collections.containsKey(nameBytes)) ? collections.get(nameBytes) : BytesType.instance;
			cfm.getDroppedColumns().put(nameBytes, new CFMetaData.DroppedColumn(name, type, time));
		}
	}

	private static List<ColumnDefinition> createColumnsFromColumnRows(Iterable<UntypedResultSet.Row> rows, String keyspace, String table, AbstractType<?> rawComparator, AbstractType<?> rawSubComparator, boolean isSuper, boolean isCQLTable, boolean isStaticCompactTable, boolean needsUpgrade) {
		List<ColumnDefinition> columns = new ArrayList<>();
		for (UntypedResultSet.Row row : rows) {
			if (LegacySchemaMigrator.isEmptyCompactValueColumn(row))
				continue;

			columns.add(LegacySchemaMigrator.createColumnFromColumnRow(row, keyspace, table, rawComparator, rawSubComparator, isSuper, isCQLTable, isStaticCompactTable, needsUpgrade));
		}
		return columns;
	}

	private static ColumnDefinition createColumnFromColumnRow(UntypedResultSet.Row row, String keyspace, String table, AbstractType<?> rawComparator, AbstractType<?> rawSubComparator, boolean isSuper, boolean isCQLTable, boolean isStaticCompactTable, boolean needsUpgrade) {
		String rawKind = row.getString("type");
		ColumnDefinition.Kind kind = LegacySchemaMigrator.deserializeKind(rawKind);
		if ((needsUpgrade && isStaticCompactTable) && (kind == (REGULAR)))
			kind = STATIC;

		int componentIndex = ColumnDefinition.NO_POSITION;
		if (kind.isPrimaryKeyKind())
			componentIndex = (row.has("component_index")) ? row.getInt("component_index") : 0;

		AbstractType<?> comparator = (isCQLTable) ? UTF8Type.instance : CompactTables.columnDefinitionComparator(rawKind, isSuper, rawComparator, rawSubComparator);
		ColumnIdentifier name = ColumnIdentifier.getInterned(comparator.fromString(row.getString("column_name")), comparator);
		AbstractType<?> validator = LegacySchemaMigrator.parseType(row.getString("validator"));
		if ((validator.isUDT()) && (validator.isMultiCell()))
			validator = validator.freeze();
		else
			validator = validator.freezeNestedMulticellTypes();

		return new ColumnDefinition(keyspace, table, name, validator, componentIndex, kind);
	}

	private static Indexes createIndexesFromColumnRows(CFMetaData cfm, Iterable<UntypedResultSet.Row> rows, String keyspace, String table, AbstractType<?> rawComparator, AbstractType<?> rawSubComparator, boolean isSuper, boolean isCQLTable, boolean isStaticCompactTable, boolean needsUpgrade) {
		Indexes.Builder indexes = Indexes.builder();
		for (UntypedResultSet.Row row : rows) {
			IndexMetadata.Kind kind = null;
			if (row.has("index_type"))
				kind = valueOf(row.getString("index_type"));

			if (kind == null)
				continue;

			Map<String, String> indexOptions = null;
			if (row.has("index_options"))
				indexOptions = FBUtilities.fromJsonMap(row.getString("index_options"));

			if (row.has("index_name")) {
				String indexName = row.getString("index_name");
				ColumnDefinition column = LegacySchemaMigrator.createColumnFromColumnRow(row, keyspace, table, rawComparator, rawSubComparator, isSuper, isCQLTable, isStaticCompactTable, needsUpgrade);
				indexes.add(IndexMetadata.fromLegacyMetadata(cfm, column, indexName, kind, indexOptions));
			}else {
				LegacySchemaMigrator.logger.error("Failed to find index name for legacy migration of index on {}.{}", keyspace, table);
			}
		}
		return indexes.build();
	}

	private static ColumnDefinition.Kind deserializeKind(String kind) {
		if ("clustering_key".equalsIgnoreCase(kind))
			return CLUSTERING;

		if ("compact_value".equalsIgnoreCase(kind))
			return REGULAR;

		return Enum.valueOf(ColumnDefinition.Kind.class, kind.toUpperCase());
	}

	private static Triggers createTriggersFromTriggerRows(UntypedResultSet rows) {
		Triggers.Builder triggers = Triggers.builder();
		rows.forEach(( row) -> triggers.add(LegacySchemaMigrator.createTriggerFromTriggerRow(row)));
		return triggers.build();
	}

	private static TriggerMetadata createTriggerFromTriggerRow(UntypedResultSet.Row row) {
		String name = row.getString("trigger_name");
		String classOption = row.getTextMap("trigger_options").get("class");
		return new TriggerMetadata(name, classOption);
	}

	private static Collection<LegacySchemaMigrator.Type> readTypes(String keyspaceName) {
		String query = String.format("SELECT type_name FROM %s.%s WHERE keyspace_name = ?", SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.LEGACY_USERTYPES);
		Collection<String> typeNames = new ArrayList<>();
		LegacySchemaMigrator.query(query, keyspaceName).forEach(( row) -> typeNames.add(row.getString("type_name")));
		Collection<LegacySchemaMigrator.Type> types = new ArrayList<>();
		typeNames.forEach(( name) -> types.add(LegacySchemaMigrator.readType(keyspaceName, name)));
		return types;
	}

	private static LegacySchemaMigrator.Type readType(String keyspaceName, String typeName) {
		UserType metadata = LegacySchemaMigrator.readTypeMetadata(keyspaceName, typeName);
		return null;
	}

	private static UserType readTypeMetadata(String keyspaceName, String typeName) {
		String query = String.format("SELECT * FROM %s.%s WHERE keyspace_name = ? AND type_name = ?", SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.LEGACY_USERTYPES);
		UntypedResultSet.Row row = LegacySchemaMigrator.query(query, keyspaceName, typeName).one();
		List<FieldIdentifier> names = row.getList("field_names", UTF8Type.instance).stream().map(( t) -> FieldIdentifier.forInternalString(t)).collect(Collectors.toList());
		List<AbstractType<?>> types = row.getList("field_types", UTF8Type.instance).stream().map(LegacySchemaMigrator::parseType).collect(Collectors.toList());
		return new UserType(keyspaceName, ByteBufferUtil.bytes(typeName), names, types, true);
	}

	private static Collection<LegacySchemaMigrator.Function> readFunctions(String keyspaceName) {
		String query = String.format("SELECT function_name, signature FROM %s.%s WHERE keyspace_name = ?", SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.LEGACY_FUNCTIONS);
		HashMultimap<String, List<String>> functionSignatures = HashMultimap.create();
		LegacySchemaMigrator.query(query, keyspaceName).forEach(( row) -> functionSignatures.put(row.getString("function_name"), row.getList("signature", UTF8Type.instance)));
		Collection<LegacySchemaMigrator.Function> functions = new ArrayList<>();
		functionSignatures.entries().forEach(( pair) -> functions.add(LegacySchemaMigrator.readFunction(keyspaceName, pair.getKey(), pair.getValue())));
		return functions;
	}

	private static LegacySchemaMigrator.Function readFunction(String keyspaceName, String functionName, List<String> signature) {
		long timestamp = LegacySchemaMigrator.readFunctionTimestamp(keyspaceName, functionName, signature);
		UDFunction metadata = LegacySchemaMigrator.readFunctionMetadata(keyspaceName, functionName, signature);
		return new LegacySchemaMigrator.Function(timestamp, metadata);
	}

	private static long readFunctionTimestamp(String keyspaceName, String functionName, List<String> signature) {
		String query = String.format(("SELECT writeTime(return_type) AS timestamp " + ("FROM %s.%s " + "WHERE keyspace_name = ? AND function_name = ? AND signature = ?")), SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.LEGACY_FUNCTIONS);
		return LegacySchemaMigrator.query(query, keyspaceName, functionName, signature).one().getLong("timestamp");
	}

	private static UDFunction readFunctionMetadata(String keyspaceName, String functionName, List<String> signature) {
		String query = String.format("SELECT * FROM %s.%s WHERE keyspace_name = ? AND function_name = ? AND signature = ?", SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.LEGACY_FUNCTIONS);
		UntypedResultSet.Row row = LegacySchemaMigrator.query(query, keyspaceName, functionName, signature).one();
		FunctionName name = new FunctionName(keyspaceName, functionName);
		List<ColumnIdentifier> argNames = new ArrayList<>();
		if (row.has("argument_names"))
			for (String arg : row.getList("argument_names", UTF8Type.instance))
				argNames.add(new ColumnIdentifier(arg, true));


		List<AbstractType<?>> argTypes = new ArrayList<>();
		if (row.has("argument_types"))
			for (String type : row.getList("argument_types", UTF8Type.instance))
				argTypes.add(LegacySchemaMigrator.parseType(type));


		AbstractType<?> returnType = LegacySchemaMigrator.parseType(row.getString("return_type"));
		String language = row.getString("language");
		String body = row.getString("body");
		boolean calledOnNullInput = row.getBoolean("called_on_null_input");
		try {
			return UDFunction.create(name, argNames, argTypes, returnType, calledOnNullInput, language, body);
		} catch (InvalidRequestException e) {
			return UDFunction.createBrokenFunction(name, argNames, argTypes, returnType, calledOnNullInput, language, body, e);
		}
	}

	private static Collection<LegacySchemaMigrator.Aggregate> readAggregates(Functions functions, String keyspaceName) {
		String query = String.format("SELECT aggregate_name, signature FROM %s.%s WHERE keyspace_name = ?", SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.LEGACY_AGGREGATES);
		HashMultimap<String, List<String>> aggregateSignatures = HashMultimap.create();
		LegacySchemaMigrator.query(query, keyspaceName).forEach(( row) -> aggregateSignatures.put(row.getString("aggregate_name"), row.getList("signature", UTF8Type.instance)));
		Collection<LegacySchemaMigrator.Aggregate> aggregates = new ArrayList<>();
		aggregateSignatures.entries().forEach(( pair) -> aggregates.add(LegacySchemaMigrator.readAggregate(functions, keyspaceName, pair.getKey(), pair.getValue())));
		return aggregates;
	}

	private static LegacySchemaMigrator.Aggregate readAggregate(Functions functions, String keyspaceName, String aggregateName, List<String> signature) {
		long timestamp = LegacySchemaMigrator.readAggregateTimestamp(keyspaceName, aggregateName, signature);
		UDAggregate metadata = LegacySchemaMigrator.readAggregateMetadata(functions, keyspaceName, aggregateName, signature);
		return new LegacySchemaMigrator.Aggregate(timestamp, metadata);
	}

	private static long readAggregateTimestamp(String keyspaceName, String aggregateName, List<String> signature) {
		String query = String.format(("SELECT writeTime(return_type) AS timestamp " + ("FROM %s.%s " + "WHERE keyspace_name = ? AND aggregate_name = ? AND signature = ?")), SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.LEGACY_AGGREGATES);
		return LegacySchemaMigrator.query(query, keyspaceName, aggregateName, signature).one().getLong("timestamp");
	}

	private static UDAggregate readAggregateMetadata(Functions functions, String keyspaceName, String functionName, List<String> signature) {
		String query = String.format("SELECT * FROM %s.%s WHERE keyspace_name = ? AND aggregate_name = ? AND signature = ?", SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.LEGACY_AGGREGATES);
		UntypedResultSet.Row row = LegacySchemaMigrator.query(query, keyspaceName, functionName, signature).one();
		FunctionName name = new FunctionName(keyspaceName, functionName);
		List<String> types = row.getList("argument_types", UTF8Type.instance);
		List<AbstractType<?>> argTypes = new ArrayList<>();
		if (types != null) {
			argTypes = new ArrayList<>(types.size());
			for (String type : types)
				argTypes.add(LegacySchemaMigrator.parseType(type));

		}
		AbstractType<?> returnType = LegacySchemaMigrator.parseType(row.getString("return_type"));
		FunctionName stateFunc = new FunctionName(keyspaceName, row.getString("state_func"));
		AbstractType<?> stateType = LegacySchemaMigrator.parseType(row.getString("state_type"));
		FunctionName finalFunc = (row.has("final_func")) ? new FunctionName(keyspaceName, row.getString("final_func")) : null;
		ByteBuffer initcond = (row.has("initcond")) ? row.getBytes("initcond") : null;
		try {
			return UDAggregate.create(functions, name, argTypes, returnType, stateFunc, finalFunc, stateType, initcond);
		} catch (InvalidRequestException reason) {
			return UDAggregate.createBroken(name, argTypes, returnType, initcond, reason);
		}
	}

	private static UntypedResultSet query(String query, Object... values) {
		return QueryProcessor.executeOnceInternal(query, values);
	}

	private static AbstractType<?> parseType(String str) {
		return TypeParser.parse(str);
	}

	private static final class Keyspace {
		final long timestamp;

		final String name;

		final KeyspaceParams params;

		final Collection<LegacySchemaMigrator.Table> tables;

		final Collection<LegacySchemaMigrator.Type> types;

		final Collection<LegacySchemaMigrator.Function> functions;

		final Collection<LegacySchemaMigrator.Aggregate> aggregates;

		Keyspace(long timestamp, String name, KeyspaceParams params, Collection<LegacySchemaMigrator.Table> tables, Collection<LegacySchemaMigrator.Type> types, Collection<LegacySchemaMigrator.Function> functions, Collection<LegacySchemaMigrator.Aggregate> aggregates) {
			this.timestamp = timestamp;
			this.name = name;
			this.params = params;
			this.tables = tables;
			this.types = types;
			this.functions = functions;
			this.aggregates = aggregates;
		}
	}

	private static final class Table {
		final long timestamp;

		final CFMetaData metadata;

		Table(long timestamp, CFMetaData metadata) {
			this.timestamp = timestamp;
			this.metadata = metadata;
		}
	}

	private static final class Type {
		final long timestamp;

		final UserType metadata;

		Type(long timestamp, UserType metadata) {
			this.timestamp = timestamp;
			this.metadata = metadata;
		}
	}

	private static final class Function {
		final long timestamp;

		final UDFunction metadata;

		Function(long timestamp, UDFunction metadata) {
			this.timestamp = timestamp;
			this.metadata = metadata;
		}
	}

	private static final class Aggregate {
		final long timestamp;

		final UDAggregate metadata;

		Aggregate(long timestamp, UDAggregate metadata) {
			this.timestamp = timestamp;
			this.metadata = metadata;
		}
	}
}

