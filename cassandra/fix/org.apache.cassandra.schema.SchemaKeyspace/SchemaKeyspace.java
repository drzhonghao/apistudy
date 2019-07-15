

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.config.SchemaConstants;
import org.apache.cassandra.config.ViewDefinition;
import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.cql3.FieldIdentifier;
import org.apache.cassandra.cql3.QueryProcessor;
import org.apache.cassandra.cql3.Terms;
import org.apache.cassandra.cql3.UntypedResultSet;
import org.apache.cassandra.cql3.functions.AbstractFunction;
import org.apache.cassandra.cql3.functions.FunctionName;
import org.apache.cassandra.cql3.functions.ScalarFunction;
import org.apache.cassandra.cql3.functions.UDAggregate;
import org.apache.cassandra.cql3.functions.UDFunction;
import org.apache.cassandra.cql3.statements.ParsedStatement;
import org.apache.cassandra.cql3.statements.SelectStatement;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.PartitionColumns;
import org.apache.cassandra.db.PartitionRangeReadCommand;
import org.apache.cassandra.db.ReadCommand;
import org.apache.cassandra.db.ReadExecutionController;
import org.apache.cassandra.db.commitlog.CommitLogPosition;
import org.apache.cassandra.db.filter.ColumnFilter;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.CompositeType;
import org.apache.cassandra.db.marshal.ListType;
import org.apache.cassandra.db.marshal.MapType;
import org.apache.cassandra.db.marshal.ReversedType;
import org.apache.cassandra.db.marshal.SetType;
import org.apache.cassandra.db.marshal.TupleType;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.cassandra.db.marshal.UserType;
import org.apache.cassandra.db.partitions.PartitionIterator;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.db.partitions.UnfilteredPartitionIterator;
import org.apache.cassandra.db.rows.BaseRowIterator;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.RowIterator;
import org.apache.cassandra.db.rows.RowIterators;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.view.View;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.schema.CQLTypeParser;
import org.apache.cassandra.schema.CachingParams;
import org.apache.cassandra.schema.CompactionParams;
import org.apache.cassandra.schema.CompressionParams;
import org.apache.cassandra.schema.Functions;
import org.apache.cassandra.schema.IndexMetadata;
import org.apache.cassandra.schema.Indexes;
import org.apache.cassandra.schema.KeyspaceMetadata;
import org.apache.cassandra.schema.KeyspaceParams;
import org.apache.cassandra.schema.Keyspaces;
import org.apache.cassandra.schema.ReplicationParams;
import org.apache.cassandra.schema.SpeculativeRetryParam;
import org.apache.cassandra.schema.TableParams;
import org.apache.cassandra.schema.Tables;
import org.apache.cassandra.schema.TriggerMetadata;
import org.apache.cassandra.schema.Triggers;
import org.apache.cassandra.schema.Types;
import org.apache.cassandra.schema.Views;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.config.CFMetaData.Flag.COMPOUND;
import static org.apache.cassandra.config.CFMetaData.Flag.COUNTER;
import static org.apache.cassandra.config.CFMetaData.Flag.DENSE;
import static org.apache.cassandra.config.CFMetaData.Flag.SUPER;
import static org.apache.cassandra.config.ColumnDefinition.ClusteringOrder.DESC;
import static org.apache.cassandra.schema.IndexMetadata.Kind.valueOf;
import static org.apache.cassandra.schema.KeyspaceParams.Option.DURABLE_WRITES;
import static org.apache.cassandra.schema.KeyspaceParams.Option.REPLICATION;


public final class SchemaKeyspace {
	private SchemaKeyspace() {
	}

	private static final Logger logger = LoggerFactory.getLogger(SchemaKeyspace.class);

	private static final boolean FLUSH_SCHEMA_TABLES = Boolean.parseBoolean(System.getProperty("cassandra.test.flush_local_schema_changes", "true"));

	private static final boolean IGNORE_CORRUPTED_SCHEMA_TABLES = Boolean.parseBoolean(System.getProperty("cassandra.ignore_corrupted_schema_tables", "false"));

	public static final String KEYSPACES = "keyspaces";

	public static final String TABLES = "tables";

	public static final String COLUMNS = "columns";

	public static final String DROPPED_COLUMNS = "dropped_columns";

	public static final String TRIGGERS = "triggers";

	public static final String VIEWS = "views";

	public static final String TYPES = "types";

	public static final String FUNCTIONS = "functions";

	public static final String AGGREGATES = "aggregates";

	public static final String INDEXES = "indexes";

	public static final ImmutableList<String> ALL = ImmutableList.of(SchemaKeyspace.COLUMNS, SchemaKeyspace.DROPPED_COLUMNS, SchemaKeyspace.TRIGGERS, SchemaKeyspace.TYPES, SchemaKeyspace.FUNCTIONS, SchemaKeyspace.AGGREGATES, SchemaKeyspace.INDEXES, SchemaKeyspace.TABLES, SchemaKeyspace.VIEWS, SchemaKeyspace.KEYSPACES);

	private static final Set<String> TABLES_WITH_CDC_ADDED = ImmutableSet.of(SchemaKeyspace.TABLES, SchemaKeyspace.VIEWS);

	public static final ImmutableList<String> ALL_FOR_DIGEST = ImmutableList.of(SchemaKeyspace.KEYSPACES, SchemaKeyspace.TABLES, SchemaKeyspace.COLUMNS, SchemaKeyspace.TRIGGERS, SchemaKeyspace.VIEWS, SchemaKeyspace.TYPES, SchemaKeyspace.FUNCTIONS, SchemaKeyspace.AGGREGATES, SchemaKeyspace.INDEXES);

	private static final CFMetaData Keyspaces = SchemaKeyspace.compile(SchemaKeyspace.KEYSPACES, "keyspace definitions", ("CREATE TABLE %s (" + ((("keyspace_name text," + "durable_writes boolean,") + "replication frozen<map<text, text>>,") + "PRIMARY KEY ((keyspace_name)))")));

	private static final CFMetaData Tables = SchemaKeyspace.compile(SchemaKeyspace.TABLES, "table definitions", ("CREATE TABLE %s (" + (((((((((((((((((((("keyspace_name text," + "table_name text,") + "bloom_filter_fp_chance double,") + "caching frozen<map<text, text>>,") + "comment text,") + "compaction frozen<map<text, text>>,") + "compression frozen<map<text, text>>,") + "crc_check_chance double,") + "dclocal_read_repair_chance double,") + "default_time_to_live int,") + "extensions frozen<map<text, blob>>,") + "flags frozen<set<text>>,") + "gc_grace_seconds int,") + "id uuid,") + "max_index_interval int,") + "memtable_flush_period_in_ms int,") + "min_index_interval int,") + "read_repair_chance double,") + "speculative_retry text,") + "cdc boolean,") + "PRIMARY KEY ((keyspace_name), table_name))")));

	private static final CFMetaData Columns = SchemaKeyspace.compile(SchemaKeyspace.COLUMNS, "column definitions", ("CREATE TABLE %s (" + (((((((("keyspace_name text," + "table_name text,") + "column_name text,") + "clustering_order text,") + "column_name_bytes blob,") + "kind text,") + "position int,") + "type text,") + "PRIMARY KEY ((keyspace_name), table_name, column_name))")));

	private static final CFMetaData DroppedColumns = SchemaKeyspace.compile(SchemaKeyspace.DROPPED_COLUMNS, "dropped column registry", ("CREATE TABLE %s (" + ((((("keyspace_name text," + "table_name text,") + "column_name text,") + "dropped_time timestamp,") + "type text,") + "PRIMARY KEY ((keyspace_name), table_name, column_name))")));

	private static final CFMetaData Triggers = SchemaKeyspace.compile(SchemaKeyspace.TRIGGERS, "trigger definitions", ("CREATE TABLE %s (" + (((("keyspace_name text," + "table_name text,") + "trigger_name text,") + "options frozen<map<text, text>>,") + "PRIMARY KEY ((keyspace_name), table_name, trigger_name))")));

	private static final CFMetaData Views = SchemaKeyspace.compile(SchemaKeyspace.VIEWS, "view definitions", ("CREATE TABLE %s (" + ((((((((((((((((((((((("keyspace_name text," + "view_name text,") + "base_table_id uuid,") + "base_table_name text,") + "where_clause text,") + "bloom_filter_fp_chance double,") + "caching frozen<map<text, text>>,") + "comment text,") + "compaction frozen<map<text, text>>,") + "compression frozen<map<text, text>>,") + "crc_check_chance double,") + "dclocal_read_repair_chance double,") + "default_time_to_live int,") + "extensions frozen<map<text, blob>>,") + "gc_grace_seconds int,") + "id uuid,") + "include_all_columns boolean,") + "max_index_interval int,") + "memtable_flush_period_in_ms int,") + "min_index_interval int,") + "read_repair_chance double,") + "speculative_retry text,") + "cdc boolean,") + "PRIMARY KEY ((keyspace_name), view_name))")));

	private static final CFMetaData Indexes = SchemaKeyspace.compile(SchemaKeyspace.INDEXES, "secondary index definitions", ("CREATE TABLE %s (" + ((((("keyspace_name text," + "table_name text,") + "index_name text,") + "kind text,") + "options frozen<map<text, text>>,") + "PRIMARY KEY ((keyspace_name), table_name, index_name))")));

	private static final CFMetaData Types = SchemaKeyspace.compile(SchemaKeyspace.TYPES, "user defined type definitions", ("CREATE TABLE %s (" + (((("keyspace_name text," + "type_name text,") + "field_names frozen<list<text>>,") + "field_types frozen<list<text>>,") + "PRIMARY KEY ((keyspace_name), type_name))")));

	private static final CFMetaData Functions = SchemaKeyspace.compile(SchemaKeyspace.FUNCTIONS, "user defined function definitions", ("CREATE TABLE %s (" + (((((((("keyspace_name text," + "function_name text,") + "argument_types frozen<list<text>>,") + "argument_names frozen<list<text>>,") + "body text,") + "language text,") + "return_type text,") + "called_on_null_input boolean,") + "PRIMARY KEY ((keyspace_name), function_name, argument_types))")));

	private static final CFMetaData Aggregates = SchemaKeyspace.compile(SchemaKeyspace.AGGREGATES, "user defined aggregate definitions", ("CREATE TABLE %s (" + (((((((("keyspace_name text," + "aggregate_name text,") + "argument_types frozen<list<text>>,") + "final_func text,") + "initcond text,") + "return_type text,") + "state_func text,") + "state_type text,") + "PRIMARY KEY ((keyspace_name), aggregate_name, argument_types))")));

	public static final List<CFMetaData> ALL_TABLE_METADATA = ImmutableList.of(SchemaKeyspace.Keyspaces, SchemaKeyspace.Tables, SchemaKeyspace.Columns, SchemaKeyspace.Triggers, SchemaKeyspace.DroppedColumns, SchemaKeyspace.Views, SchemaKeyspace.Types, SchemaKeyspace.Functions, SchemaKeyspace.Aggregates, SchemaKeyspace.Indexes);

	private static CFMetaData compile(String name, String description, String schema) {
		return CFMetaData.compile(String.format(schema, name), SchemaConstants.SCHEMA_KEYSPACE_NAME).comment(description).gcGraceSeconds(((int) (TimeUnit.DAYS.toSeconds(7))));
	}

	public static KeyspaceMetadata metadata() {
		return KeyspaceMetadata.create(SchemaConstants.SCHEMA_KEYSPACE_NAME, KeyspaceParams.local(), org.apache.cassandra.schema.Tables.of(SchemaKeyspace.ALL_TABLE_METADATA));
	}

	public static void saveSystemKeyspacesSchema() {
		KeyspaceMetadata system = Schema.instance.getKSMetaData(SchemaConstants.SYSTEM_KEYSPACE_NAME);
		KeyspaceMetadata schema = Schema.instance.getKSMetaData(SchemaConstants.SCHEMA_KEYSPACE_NAME);
		long timestamp = FBUtilities.timestampMicros();
		for (String schemaTable : SchemaKeyspace.ALL) {
			String query = String.format("DELETE FROM %s.%s USING TIMESTAMP ? WHERE keyspace_name = ?", SchemaConstants.SCHEMA_KEYSPACE_NAME, schemaTable);
			for (String systemKeyspace : SchemaConstants.LOCAL_SYSTEM_KEYSPACE_NAMES)
				QueryProcessor.executeOnceInternal(query, timestamp, systemKeyspace);

		}
		SchemaKeyspace.makeCreateKeyspaceMutation(system, (timestamp + 1)).build().apply();
		SchemaKeyspace.makeCreateKeyspaceMutation(schema, (timestamp + 1)).build().apply();
	}

	public static void truncate() {
		SchemaKeyspace.ALL.reverse().forEach(( table) -> SchemaKeyspace.getSchemaCFS(table).truncateBlocking());
	}

	static void flush() {
		if (!(DatabaseDescriptor.isUnsafeSystem()))
			SchemaKeyspace.ALL.forEach(( table) -> FBUtilities.waitOnFuture(SchemaKeyspace.getSchemaCFS(table).forceFlush()));

	}

	public static Pair<UUID, UUID> calculateSchemaDigest() {
		Set<ByteBuffer> cdc = Collections.singleton(ByteBufferUtil.bytes("cdc"));
		return SchemaKeyspace.calculateSchemaDigest(cdc);
	}

	@com.google.common.annotations.VisibleForTesting
	static Pair<UUID, UUID> calculateSchemaDigest(Set<ByteBuffer> columnsToExclude) {
		MessageDigest digest;
		MessageDigest digest30;
		try {
			digest = MessageDigest.getInstance("MD5");
			digest30 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		for (String table : SchemaKeyspace.ALL_FOR_DIGEST) {
			ReadCommand cmd = SchemaKeyspace.getReadCommandForTableSchema(table);
			try (ReadExecutionController executionController = cmd.executionController();PartitionIterator schema = cmd.executeInternal(executionController)) {
				while (schema.hasNext()) {
					try (RowIterator partition = schema.next()) {
						if (!(SchemaKeyspace.isSystemKeyspaceSchemaPartition(partition.partitionKey()))) {
							RowIterators.digest(partition, digest, digest30, columnsToExclude);
						}
					}
				} 
			}
		}
		return Pair.create(UUID.nameUUIDFromBytes(digest.digest()), UUID.nameUUIDFromBytes(digest30.digest()));
	}

	private static ColumnFamilyStore getSchemaCFS(String schemaTableName) {
		return Keyspace.open(SchemaConstants.SCHEMA_KEYSPACE_NAME).getColumnFamilyStore(schemaTableName);
	}

	private static ReadCommand getReadCommandForTableSchema(String schemaTableName) {
		ColumnFamilyStore cfs = SchemaKeyspace.getSchemaCFS(schemaTableName);
		return PartitionRangeReadCommand.allDataRead(cfs.metadata, FBUtilities.nowInSeconds());
	}

	public static Collection<Mutation> convertSchemaToMutations() {
		Map<DecoratedKey, Mutation> mutationMap = new HashMap<>();
		for (String table : SchemaKeyspace.ALL)
			SchemaKeyspace.convertSchemaToMutations(mutationMap, table);

		return mutationMap.values();
	}

	private static void convertSchemaToMutations(Map<DecoratedKey, Mutation> mutationMap, String schemaTableName) {
		ReadCommand cmd = SchemaKeyspace.getReadCommandForTableSchema(schemaTableName);
		try (ReadExecutionController executionController = cmd.executionController();UnfilteredPartitionIterator iter = cmd.executeLocally(executionController)) {
			while (iter.hasNext()) {
				try (UnfilteredRowIterator partition = iter.next()) {
					if (SchemaKeyspace.isSystemKeyspaceSchemaPartition(partition.partitionKey()))
						continue;

					DecoratedKey key = partition.partitionKey();
					Mutation mutation = mutationMap.get(key);
					if (mutation == null) {
						mutation = new Mutation(SchemaConstants.SCHEMA_KEYSPACE_NAME, key);
						mutationMap.put(key, mutation);
					}
					mutation.add(SchemaKeyspace.makeUpdateForSchema(partition, cmd.columnFilter()));
				}
			} 
		}
	}

	private static PartitionUpdate makeUpdateForSchema(UnfilteredRowIterator partition, ColumnFilter filter) {
		if ((DatabaseDescriptor.isCDCEnabled()) || (!(SchemaKeyspace.TABLES_WITH_CDC_ADDED.contains(partition.metadata().cfName))))
			return PartitionUpdate.fromIterator(partition, filter);

		ColumnFilter.Builder builder = ColumnFilter.allColumnsBuilder(partition.metadata());
		for (ColumnDefinition column : filter.fetchedColumns()) {
			if (!(column.name.toString().equals("cdc")))
				builder.add(column);

		}
		return PartitionUpdate.fromIterator(partition, builder.build());
	}

	private static boolean isSystemKeyspaceSchemaPartition(DecoratedKey partitionKey) {
		return SchemaConstants.isLocalSystemKeyspace(UTF8Type.instance.compose(partitionKey.getKey()));
	}

	private static DecoratedKey decorate(CFMetaData metadata, Object value) {
		return metadata.decorateKey(((AbstractType) (metadata.getKeyValidator())).decompose(value));
	}

	public static Mutation.SimpleBuilder makeCreateKeyspaceMutation(String name, KeyspaceParams params, long timestamp) {
		Mutation.SimpleBuilder builder = Mutation.simpleBuilder(SchemaKeyspace.Keyspaces.ksName, SchemaKeyspace.decorate(SchemaKeyspace.Keyspaces, name)).timestamp(timestamp);
		builder.update(SchemaKeyspace.Keyspaces).row().add(DURABLE_WRITES.toString(), params.durableWrites).add(REPLICATION.toString(), params.replication.asMap());
		return builder;
	}

	public static Mutation.SimpleBuilder makeCreateKeyspaceMutation(KeyspaceMetadata keyspace, long timestamp) {
		Mutation.SimpleBuilder builder = SchemaKeyspace.makeCreateKeyspaceMutation(keyspace.name, keyspace.params, timestamp);
		keyspace.tables.forEach(( table) -> SchemaKeyspace.addTableToSchemaMutation(table, true, builder));
		keyspace.views.forEach(( view) -> SchemaKeyspace.addViewToSchemaMutation(view, true, builder));
		keyspace.types.forEach(( type) -> SchemaKeyspace.addTypeToSchemaMutation(type, builder));
		keyspace.functions.udfs().forEach(( udf) -> SchemaKeyspace.addFunctionToSchemaMutation(udf, builder));
		keyspace.functions.udas().forEach(( uda) -> SchemaKeyspace.addAggregateToSchemaMutation(uda, builder));
		return builder;
	}

	public static Mutation.SimpleBuilder makeDropKeyspaceMutation(KeyspaceMetadata keyspace, long timestamp) {
		Mutation.SimpleBuilder builder = Mutation.simpleBuilder(SchemaConstants.SCHEMA_KEYSPACE_NAME, SchemaKeyspace.decorate(SchemaKeyspace.Keyspaces, keyspace.name)).timestamp(timestamp);
		for (CFMetaData schemaTable : SchemaKeyspace.ALL_TABLE_METADATA)
			builder.update(schemaTable).delete();

		return builder;
	}

	public static Mutation.SimpleBuilder makeCreateTypeMutation(KeyspaceMetadata keyspace, UserType type, long timestamp) {
		Mutation.SimpleBuilder builder = SchemaKeyspace.makeCreateKeyspaceMutation(keyspace.name, keyspace.params, timestamp);
		SchemaKeyspace.addTypeToSchemaMutation(type, builder);
		return builder;
	}

	static void addTypeToSchemaMutation(UserType type, Mutation.SimpleBuilder mutation) {
		mutation.update(SchemaKeyspace.Types).row(type.getNameAsString()).add("field_names", type.fieldNames().stream().map(FieldIdentifier::toString).collect(Collectors.toList())).add("field_types", type.fieldTypes().stream().map(AbstractType::asCQL3Type).map(CQL3Type::toString).collect(Collectors.toList()));
	}

	public static Mutation.SimpleBuilder dropTypeFromSchemaMutation(KeyspaceMetadata keyspace, UserType type, long timestamp) {
		Mutation.SimpleBuilder builder = SchemaKeyspace.makeCreateKeyspaceMutation(keyspace.name, keyspace.params, timestamp);
		builder.update(SchemaKeyspace.Types).row(type.name).delete();
		return builder;
	}

	public static Mutation.SimpleBuilder makeCreateTableMutation(KeyspaceMetadata keyspace, CFMetaData table, long timestamp) {
		Mutation.SimpleBuilder builder = SchemaKeyspace.makeCreateKeyspaceMutation(keyspace.name, keyspace.params, timestamp);
		SchemaKeyspace.addTableToSchemaMutation(table, true, builder);
		return builder;
	}

	static void addTableToSchemaMutation(CFMetaData table, boolean withColumnsAndTriggers, Mutation.SimpleBuilder builder) {
		Row.SimpleBuilder rowBuilder = builder.update(SchemaKeyspace.Tables).row(table.cfName).add("id", table.cfId).add("flags", CFMetaData.flagsToStrings(table.flags()));
		SchemaKeyspace.addTableParamsToRowBuilder(table.params, rowBuilder);
		if (withColumnsAndTriggers) {
			for (ColumnDefinition column : table.allColumns())
				SchemaKeyspace.addColumnToSchemaMutation(table, column, builder);

			for (CFMetaData.DroppedColumn column : table.getDroppedColumns().values())
				SchemaKeyspace.addDroppedColumnToSchemaMutation(table, column, builder);

			for (TriggerMetadata trigger : table.getTriggers())
				SchemaKeyspace.addTriggerToSchemaMutation(table, trigger, builder);

			for (IndexMetadata index : table.getIndexes())
				SchemaKeyspace.addIndexToSchemaMutation(table, index, builder);

		}
	}

	private static void addTableParamsToRowBuilder(TableParams params, Row.SimpleBuilder builder) {
		builder.add("bloom_filter_fp_chance", params.bloomFilterFpChance).add("comment", params.comment).add("dclocal_read_repair_chance", params.dcLocalReadRepairChance).add("default_time_to_live", params.defaultTimeToLive).add("gc_grace_seconds", params.gcGraceSeconds).add("max_index_interval", params.maxIndexInterval).add("memtable_flush_period_in_ms", params.memtableFlushPeriodInMs).add("min_index_interval", params.minIndexInterval).add("read_repair_chance", params.readRepairChance).add("speculative_retry", params.speculativeRetry.toString()).add("crc_check_chance", params.crcCheckChance).add("caching", params.caching.asMap()).add("compaction", params.compaction.asMap()).add("compression", params.compression.asMap()).add("extensions", params.extensions);
		if (DatabaseDescriptor.isCDCEnabled())
			builder.add("cdc", params.cdc);

	}

	public static Mutation.SimpleBuilder makeUpdateTableMutation(KeyspaceMetadata keyspace, CFMetaData oldTable, CFMetaData newTable, long timestamp) {
		Mutation.SimpleBuilder builder = SchemaKeyspace.makeCreateKeyspaceMutation(keyspace.name, keyspace.params, timestamp);
		SchemaKeyspace.addTableToSchemaMutation(newTable, false, builder);
		MapDifference<ByteBuffer, ColumnDefinition> columnDiff = Maps.difference(oldTable.getColumnMetadata(), newTable.getColumnMetadata());
		for (ColumnDefinition column : columnDiff.entriesOnlyOnLeft().values())
			SchemaKeyspace.dropColumnFromSchemaMutation(oldTable, column, builder);

		for (ColumnDefinition column : columnDiff.entriesOnlyOnRight().values())
			SchemaKeyspace.addColumnToSchemaMutation(newTable, column, builder);

		for (ByteBuffer name : columnDiff.entriesDiffering().keySet())
			SchemaKeyspace.addColumnToSchemaMutation(newTable, newTable.getColumnDefinition(name), builder);

		MapDifference<ByteBuffer, CFMetaData.DroppedColumn> droppedColumnDiff = Maps.difference(oldTable.getDroppedColumns(), newTable.getDroppedColumns());
		for (CFMetaData.DroppedColumn column : droppedColumnDiff.entriesOnlyOnRight().values())
			SchemaKeyspace.addDroppedColumnToSchemaMutation(newTable, column, builder);

		for (ByteBuffer name : droppedColumnDiff.entriesDiffering().keySet())
			SchemaKeyspace.addDroppedColumnToSchemaMutation(newTable, newTable.getDroppedColumns().get(name), builder);

		MapDifference<String, TriggerMetadata> triggerDiff = SchemaKeyspace.triggersDiff(oldTable.getTriggers(), newTable.getTriggers());
		for (TriggerMetadata trigger : triggerDiff.entriesOnlyOnLeft().values())
			SchemaKeyspace.dropTriggerFromSchemaMutation(oldTable, trigger, builder);

		for (TriggerMetadata trigger : triggerDiff.entriesOnlyOnRight().values())
			SchemaKeyspace.addTriggerToSchemaMutation(newTable, trigger, builder);

		MapDifference<String, IndexMetadata> indexesDiff = SchemaKeyspace.indexesDiff(oldTable.getIndexes(), newTable.getIndexes());
		for (IndexMetadata index : indexesDiff.entriesOnlyOnLeft().values())
			SchemaKeyspace.dropIndexFromSchemaMutation(oldTable, index, builder);

		for (IndexMetadata index : indexesDiff.entriesOnlyOnRight().values())
			SchemaKeyspace.addIndexToSchemaMutation(newTable, index, builder);

		for (MapDifference.ValueDifference<IndexMetadata> diff : indexesDiff.entriesDiffering().values())
			SchemaKeyspace.addUpdatedIndexToSchemaMutation(newTable, diff.rightValue(), builder);

		return builder;
	}

	private static MapDifference<String, IndexMetadata> indexesDiff(org.apache.cassandra.schema.Indexes before, org.apache.cassandra.schema.Indexes after) {
		Map<String, IndexMetadata> beforeMap = new HashMap<>();
		before.forEach(( i) -> beforeMap.put(i.name, i));
		Map<String, IndexMetadata> afterMap = new HashMap<>();
		after.forEach(( i) -> afterMap.put(i.name, i));
		return Maps.difference(beforeMap, afterMap);
	}

	private static MapDifference<String, TriggerMetadata> triggersDiff(org.apache.cassandra.schema.Triggers before, org.apache.cassandra.schema.Triggers after) {
		Map<String, TriggerMetadata> beforeMap = new HashMap<>();
		before.forEach(( t) -> beforeMap.put(t.name, t));
		Map<String, TriggerMetadata> afterMap = new HashMap<>();
		after.forEach(( t) -> afterMap.put(t.name, t));
		return Maps.difference(beforeMap, afterMap);
	}

	public static Mutation.SimpleBuilder makeDropTableMutation(KeyspaceMetadata keyspace, CFMetaData table, long timestamp) {
		Mutation.SimpleBuilder builder = SchemaKeyspace.makeCreateKeyspaceMutation(keyspace.name, keyspace.params, timestamp);
		builder.update(SchemaKeyspace.Tables).row(table.cfName).delete();
		for (ColumnDefinition column : table.allColumns())
			SchemaKeyspace.dropColumnFromSchemaMutation(table, column, builder);

		for (CFMetaData.DroppedColumn column : table.getDroppedColumns().values())
			SchemaKeyspace.dropDroppedColumnFromSchemaMutation(table, column, timestamp, builder);

		for (TriggerMetadata trigger : table.getTriggers())
			SchemaKeyspace.dropTriggerFromSchemaMutation(table, trigger, builder);

		for (IndexMetadata index : table.getIndexes())
			SchemaKeyspace.dropIndexFromSchemaMutation(table, index, builder);

		return builder;
	}

	private static void addColumnToSchemaMutation(CFMetaData table, ColumnDefinition column, Mutation.SimpleBuilder builder) {
		AbstractType<?> type = column.type;
		if (type instanceof ReversedType)
			type = ((ReversedType) (type)).baseType;

		builder.update(SchemaKeyspace.Columns).row(table.cfName, column.name.toString()).add("column_name_bytes", column.name.bytes).add("kind", column.kind.toString().toLowerCase()).add("position", column.position()).add("clustering_order", column.clusteringOrder().toString().toLowerCase()).add("type", type.asCQL3Type().toString());
	}

	private static void dropColumnFromSchemaMutation(CFMetaData table, ColumnDefinition column, Mutation.SimpleBuilder builder) {
		builder.update(SchemaKeyspace.Columns).row(table.cfName, column.name.toString()).delete();
	}

	private static void addDroppedColumnToSchemaMutation(CFMetaData table, CFMetaData.DroppedColumn column, Mutation.SimpleBuilder builder) {
		builder.update(SchemaKeyspace.DroppedColumns).row(table.cfName, column.name).add("dropped_time", new Date(TimeUnit.MICROSECONDS.toMillis(column.droppedTime))).add("type", SchemaKeyspace.expandUserTypes(column.type).asCQL3Type().toString());
	}

	private static void dropDroppedColumnFromSchemaMutation(CFMetaData table, CFMetaData.DroppedColumn column, long timestamp, Mutation.SimpleBuilder builder) {
		builder.update(SchemaKeyspace.DroppedColumns).row(table.cfName, column.name).delete();
	}

	private static void addTriggerToSchemaMutation(CFMetaData table, TriggerMetadata trigger, Mutation.SimpleBuilder builder) {
		builder.update(SchemaKeyspace.Triggers).row(table.cfName, trigger.name).add("options", Collections.singletonMap("class", trigger.classOption));
	}

	private static void dropTriggerFromSchemaMutation(CFMetaData table, TriggerMetadata trigger, Mutation.SimpleBuilder builder) {
		builder.update(SchemaKeyspace.Triggers).row(table.cfName, trigger.name).delete();
	}

	public static Mutation.SimpleBuilder makeCreateViewMutation(KeyspaceMetadata keyspace, ViewDefinition view, long timestamp) {
		Mutation.SimpleBuilder builder = SchemaKeyspace.makeCreateKeyspaceMutation(keyspace.name, keyspace.params, timestamp);
		SchemaKeyspace.addViewToSchemaMutation(view, true, builder);
		return builder;
	}

	private static void addViewToSchemaMutation(ViewDefinition view, boolean includeColumns, Mutation.SimpleBuilder builder) {
		CFMetaData table = view.metadata;
		Row.SimpleBuilder rowBuilder = builder.update(SchemaKeyspace.Views).row(view.viewName).add("include_all_columns", view.includeAllColumns).add("base_table_id", view.baseTableId).add("base_table_name", view.baseTableMetadata().cfName).add("where_clause", view.whereClause).add("id", table.cfId);
		SchemaKeyspace.addTableParamsToRowBuilder(table.params, rowBuilder);
		if (includeColumns) {
			for (ColumnDefinition column : table.allColumns())
				SchemaKeyspace.addColumnToSchemaMutation(table, column, builder);

			for (CFMetaData.DroppedColumn column : table.getDroppedColumns().values())
				SchemaKeyspace.addDroppedColumnToSchemaMutation(table, column, builder);

		}
	}

	public static Mutation.SimpleBuilder makeDropViewMutation(KeyspaceMetadata keyspace, ViewDefinition view, long timestamp) {
		Mutation.SimpleBuilder builder = SchemaKeyspace.makeCreateKeyspaceMutation(keyspace.name, keyspace.params, timestamp);
		builder.update(SchemaKeyspace.Views).row(view.viewName).delete();
		CFMetaData table = view.metadata;
		for (ColumnDefinition column : table.allColumns())
			SchemaKeyspace.dropColumnFromSchemaMutation(table, column, builder);

		for (IndexMetadata index : table.getIndexes())
			SchemaKeyspace.dropIndexFromSchemaMutation(table, index, builder);

		return builder;
	}

	public static Mutation.SimpleBuilder makeUpdateViewMutation(Mutation.SimpleBuilder builder, ViewDefinition oldView, ViewDefinition newView) {
		SchemaKeyspace.addViewToSchemaMutation(newView, false, builder);
		MapDifference<ByteBuffer, ColumnDefinition> columnDiff = Maps.difference(oldView.metadata.getColumnMetadata(), newView.metadata.getColumnMetadata());
		for (ColumnDefinition column : columnDiff.entriesOnlyOnLeft().values())
			SchemaKeyspace.dropColumnFromSchemaMutation(oldView.metadata, column, builder);

		for (ColumnDefinition column : columnDiff.entriesOnlyOnRight().values())
			SchemaKeyspace.addColumnToSchemaMutation(newView.metadata, column, builder);

		for (ByteBuffer name : columnDiff.entriesDiffering().keySet())
			SchemaKeyspace.addColumnToSchemaMutation(newView.metadata, newView.metadata.getColumnDefinition(name), builder);

		MapDifference<ByteBuffer, CFMetaData.DroppedColumn> droppedColumnDiff = Maps.difference(oldView.metadata.getDroppedColumns(), oldView.metadata.getDroppedColumns());
		for (CFMetaData.DroppedColumn column : droppedColumnDiff.entriesOnlyOnRight().values())
			SchemaKeyspace.addDroppedColumnToSchemaMutation(oldView.metadata, column, builder);

		for (ByteBuffer name : droppedColumnDiff.entriesDiffering().keySet())
			SchemaKeyspace.addDroppedColumnToSchemaMutation(newView.metadata, newView.metadata.getDroppedColumns().get(name), builder);

		return builder;
	}

	private static void addIndexToSchemaMutation(CFMetaData table, IndexMetadata index, Mutation.SimpleBuilder builder) {
		builder.update(SchemaKeyspace.Indexes).row(table.cfName, index.name).add("kind", index.kind.toString()).add("options", index.options);
	}

	private static void dropIndexFromSchemaMutation(CFMetaData table, IndexMetadata index, Mutation.SimpleBuilder builder) {
		builder.update(SchemaKeyspace.Indexes).row(table.cfName, index.name).delete();
	}

	private static void addUpdatedIndexToSchemaMutation(CFMetaData table, IndexMetadata index, Mutation.SimpleBuilder builder) {
		SchemaKeyspace.addIndexToSchemaMutation(table, index, builder);
	}

	public static Mutation.SimpleBuilder makeCreateFunctionMutation(KeyspaceMetadata keyspace, UDFunction function, long timestamp) {
		Mutation.SimpleBuilder builder = SchemaKeyspace.makeCreateKeyspaceMutation(keyspace.name, keyspace.params, timestamp);
		SchemaKeyspace.addFunctionToSchemaMutation(function, builder);
		return builder;
	}

	static void addFunctionToSchemaMutation(UDFunction function, Mutation.SimpleBuilder builder) {
		builder.update(SchemaKeyspace.Functions).row(function.name().name, SchemaKeyspace.functionArgumentsList(function)).add("body", function.body()).add("language", function.language()).add("return_type", function.returnType().asCQL3Type().toString()).add("called_on_null_input", function.isCalledOnNullInput()).add("argument_names", function.argNames().stream().map(( c) -> SchemaKeyspace.bbToString(c.bytes)).collect(Collectors.toList()));
	}

	private static String bbToString(ByteBuffer bb) {
		try {
			return ByteBufferUtil.string(bb);
		} catch (CharacterCodingException e) {
			throw new RuntimeException(e);
		}
	}

	private static List<String> functionArgumentsList(AbstractFunction fun) {
		return fun.argTypes().stream().map(AbstractType::asCQL3Type).map(CQL3Type::toString).collect(Collectors.toList());
	}

	public static Mutation.SimpleBuilder makeDropFunctionMutation(KeyspaceMetadata keyspace, UDFunction function, long timestamp) {
		Mutation.SimpleBuilder builder = SchemaKeyspace.makeCreateKeyspaceMutation(keyspace.name, keyspace.params, timestamp);
		builder.update(SchemaKeyspace.Functions).row(function.name().name, SchemaKeyspace.functionArgumentsList(function)).delete();
		return builder;
	}

	public static Mutation.SimpleBuilder makeCreateAggregateMutation(KeyspaceMetadata keyspace, UDAggregate aggregate, long timestamp) {
		Mutation.SimpleBuilder builder = SchemaKeyspace.makeCreateKeyspaceMutation(keyspace.name, keyspace.params, timestamp);
		SchemaKeyspace.addAggregateToSchemaMutation(aggregate, builder);
		return builder;
	}

	static void addAggregateToSchemaMutation(UDAggregate aggregate, Mutation.SimpleBuilder builder) {
		builder.update(SchemaKeyspace.Aggregates).row(aggregate.name().name, SchemaKeyspace.functionArgumentsList(aggregate)).add("return_type", aggregate.returnType().asCQL3Type().toString()).add("state_func", aggregate.stateFunction().name().name).add("state_type", aggregate.stateType().asCQL3Type().toString()).add("final_func", ((aggregate.finalFunction()) != null ? aggregate.finalFunction().name().name : null)).add("initcond", ((aggregate.initialCondition()) != null ? aggregate.stateType().freeze().asCQL3Type().toCQLLiteral(aggregate.initialCondition(), ProtocolVersion.CURRENT) : null));
	}

	public static Mutation.SimpleBuilder makeDropAggregateMutation(KeyspaceMetadata keyspace, UDAggregate aggregate, long timestamp) {
		Mutation.SimpleBuilder builder = SchemaKeyspace.makeCreateKeyspaceMutation(keyspace.name, keyspace.params, timestamp);
		builder.update(SchemaKeyspace.Aggregates).row(aggregate.name().name, SchemaKeyspace.functionArgumentsList(aggregate)).delete();
		return builder;
	}

	public static org.apache.cassandra.schema.Keyspaces fetchNonSystemKeyspaces() {
		return SchemaKeyspace.fetchKeyspacesWithout(SchemaConstants.LOCAL_SYSTEM_KEYSPACE_NAMES);
	}

	private static org.apache.cassandra.schema.Keyspaces fetchKeyspacesWithout(Set<String> excludedKeyspaceNames) {
		String query = String.format("SELECT keyspace_name FROM %s.%s", SchemaConstants.SCHEMA_KEYSPACE_NAME, SchemaKeyspace.KEYSPACES);
		org.apache.cassandra.schema.Keyspaces.Builder keyspaces = org.apache.cassandra.schema.Keyspaces.builder();
		for (UntypedResultSet.Row row : SchemaKeyspace.query(query)) {
			String keyspaceName = row.getString("keyspace_name");
			if (!(excludedKeyspaceNames.contains(keyspaceName)))
				keyspaces.add(SchemaKeyspace.fetchKeyspace(keyspaceName));

		}
		return keyspaces.build();
	}

	private static org.apache.cassandra.schema.Keyspaces fetchKeyspacesOnly(Set<String> includedKeyspaceNames) {
		String query = String.format("SELECT keyspace_name FROM %s.%s WHERE keyspace_name IN ?", SchemaConstants.SCHEMA_KEYSPACE_NAME, SchemaKeyspace.KEYSPACES);
		org.apache.cassandra.schema.Keyspaces.Builder keyspaces = org.apache.cassandra.schema.Keyspaces.builder();
		for (UntypedResultSet.Row row : SchemaKeyspace.query(query, new ArrayList<>(includedKeyspaceNames)))
			keyspaces.add(SchemaKeyspace.fetchKeyspace(row.getString("keyspace_name")));

		return keyspaces.build();
	}

	private static KeyspaceMetadata fetchKeyspace(String keyspaceName) {
		KeyspaceParams params = SchemaKeyspace.fetchKeyspaceParams(keyspaceName);
		org.apache.cassandra.schema.Types types = SchemaKeyspace.fetchTypes(keyspaceName);
		org.apache.cassandra.schema.Views views = SchemaKeyspace.fetchViews(keyspaceName, types);
		org.apache.cassandra.schema.Functions functions = SchemaKeyspace.fetchFunctions(keyspaceName, types);
		return null;
	}

	private static KeyspaceParams fetchKeyspaceParams(String keyspaceName) {
		String query = String.format("SELECT * FROM %s.%s WHERE keyspace_name = ?", SchemaConstants.SCHEMA_KEYSPACE_NAME, SchemaKeyspace.KEYSPACES);
		UntypedResultSet.Row row = SchemaKeyspace.query(query, keyspaceName).one();
		boolean durableWrites = row.getBoolean(DURABLE_WRITES.toString());
		Map<String, String> replication = row.getFrozenTextMap(REPLICATION.toString());
		return KeyspaceParams.create(durableWrites, replication);
	}

	private static org.apache.cassandra.schema.Types fetchTypes(String keyspaceName) {
		String query = String.format("SELECT * FROM %s.%s WHERE keyspace_name = ?", SchemaConstants.SCHEMA_KEYSPACE_NAME, SchemaKeyspace.TYPES);
		org.apache.cassandra.schema.Types.RawBuilder types = org.apache.cassandra.schema.Types.rawBuilder(keyspaceName);
		for (UntypedResultSet.Row row : SchemaKeyspace.query(query, keyspaceName)) {
			String name = row.getString("type_name");
			List<String> fieldNames = row.getFrozenList("field_names", UTF8Type.instance);
			List<String> fieldTypes = row.getFrozenList("field_types", UTF8Type.instance);
			types.add(name, fieldNames, fieldTypes);
		}
		return types.build();
	}

	private static org.apache.cassandra.schema.Tables fetchTables(String keyspaceName, org.apache.cassandra.schema.Types types) {
		String query = String.format("SELECT table_name FROM %s.%s WHERE keyspace_name = ?", SchemaConstants.SCHEMA_KEYSPACE_NAME, SchemaKeyspace.TABLES);
		org.apache.cassandra.schema.Tables.Builder tables = org.apache.cassandra.schema.Tables.builder();
		for (UntypedResultSet.Row row : SchemaKeyspace.query(query, keyspaceName)) {
			String tableName = row.getString("table_name");
			try {
				tables.add(SchemaKeyspace.fetchTable(keyspaceName, tableName, types));
			} catch (SchemaKeyspace.MissingColumns exc) {
				if (!(SchemaKeyspace.IGNORE_CORRUPTED_SCHEMA_TABLES)) {
					SchemaKeyspace.logger.error(("No columns found for table {}.{} in {}.{}.  This may be due to " + ((("corruption or concurrent dropping and altering of a table.  If this table " + "is supposed to be dropped, restart cassandra with -Dcassandra.ignore_corrupted_schema_tables=true ") + "and run the following query: \"DELETE FROM {}.{} WHERE keyspace_name = \'{}\' AND table_name = \'{}\';\".") + "If the table is not supposed to be dropped, restore {}.{} sstables from backups.")), keyspaceName, tableName, SchemaConstants.SCHEMA_KEYSPACE_NAME, SchemaKeyspace.COLUMNS, SchemaConstants.SCHEMA_KEYSPACE_NAME, SchemaKeyspace.TABLES, keyspaceName, tableName, SchemaConstants.SCHEMA_KEYSPACE_NAME, SchemaKeyspace.COLUMNS);
					throw exc;
				}
			}
		}
		return tables.build();
	}

	private static CFMetaData fetchTable(String keyspaceName, String tableName, org.apache.cassandra.schema.Types types) {
		String query = String.format("SELECT * FROM %s.%s WHERE keyspace_name = ? AND table_name = ?", SchemaConstants.SCHEMA_KEYSPACE_NAME, SchemaKeyspace.TABLES);
		UntypedResultSet rows = SchemaKeyspace.query(query, keyspaceName, tableName);
		if (rows.isEmpty())
			throw new RuntimeException(String.format("%s:%s not found in the schema definitions keyspace.", keyspaceName, tableName));

		UntypedResultSet.Row row = rows.one();
		UUID id = row.getUUID("id");
		Set<CFMetaData.Flag> flags = CFMetaData.flagsFromStrings(row.getFrozenSet("flags", UTF8Type.instance));
		boolean isSuper = flags.contains(SUPER);
		boolean isCounter = flags.contains(COUNTER);
		boolean isDense = flags.contains(DENSE);
		boolean isCompound = flags.contains(COMPOUND);
		List<ColumnDefinition> columns = SchemaKeyspace.fetchColumns(keyspaceName, tableName, types);
		if (!(columns.stream().anyMatch(ColumnDefinition::isPartitionKey))) {
			String msg = String.format("Table %s.%s did not have any partition key columns in the schema tables", keyspaceName, tableName);
			throw new AssertionError(msg);
		}
		Map<ByteBuffer, CFMetaData.DroppedColumn> droppedColumns = SchemaKeyspace.fetchDroppedColumns(keyspaceName, tableName);
		org.apache.cassandra.schema.Indexes indexes = SchemaKeyspace.fetchIndexes(keyspaceName, tableName);
		org.apache.cassandra.schema.Triggers triggers = SchemaKeyspace.fetchTriggers(keyspaceName, tableName);
		return CFMetaData.create(keyspaceName, tableName, id, isDense, isCompound, isSuper, isCounter, false, columns, DatabaseDescriptor.getPartitioner()).params(SchemaKeyspace.createTableParamsFromRow(row)).droppedColumns(droppedColumns).indexes(indexes).triggers(triggers);
	}

	public static TableParams createTableParamsFromRow(UntypedResultSet.Row row) {
		return TableParams.builder().bloomFilterFpChance(row.getDouble("bloom_filter_fp_chance")).caching(CachingParams.fromMap(row.getFrozenTextMap("caching"))).comment(row.getString("comment")).compaction(CompactionParams.fromMap(row.getFrozenTextMap("compaction"))).compression(CompressionParams.fromMap(row.getFrozenTextMap("compression"))).dcLocalReadRepairChance(row.getDouble("dclocal_read_repair_chance")).defaultTimeToLive(row.getInt("default_time_to_live")).extensions(row.getFrozenMap("extensions", UTF8Type.instance, BytesType.instance)).gcGraceSeconds(row.getInt("gc_grace_seconds")).maxIndexInterval(row.getInt("max_index_interval")).memtableFlushPeriodInMs(row.getInt("memtable_flush_period_in_ms")).minIndexInterval(row.getInt("min_index_interval")).readRepairChance(row.getDouble("read_repair_chance")).crcCheckChance(row.getDouble("crc_check_chance")).speculativeRetry(SpeculativeRetryParam.fromString(row.getString("speculative_retry"))).cdc((row.has("cdc") ? row.getBoolean("cdc") : false)).build();
	}

	private static List<ColumnDefinition> fetchColumns(String keyspace, String table, org.apache.cassandra.schema.Types types) {
		String query = String.format("SELECT * FROM %s.%s WHERE keyspace_name = ? AND table_name = ?", SchemaConstants.SCHEMA_KEYSPACE_NAME, SchemaKeyspace.COLUMNS);
		UntypedResultSet columnRows = SchemaKeyspace.query(query, keyspace, table);
		if (columnRows.isEmpty())
			throw new SchemaKeyspace.MissingColumns(((("Columns not found in schema table for " + keyspace) + ".") + table));

		List<ColumnDefinition> columns = new ArrayList<>();
		columnRows.forEach(( row) -> columns.add(SchemaKeyspace.createColumnFromRow(row, types)));
		return columns;
	}

	public static ColumnDefinition createColumnFromRow(UntypedResultSet.Row row, org.apache.cassandra.schema.Types types) {
		String keyspace = row.getString("keyspace_name");
		String table = row.getString("table_name");
		ColumnDefinition.Kind kind = ColumnDefinition.Kind.valueOf(row.getString("kind").toUpperCase());
		int position = row.getInt("position");
		ColumnDefinition.ClusteringOrder order = ColumnDefinition.ClusteringOrder.valueOf(row.getString("clustering_order").toUpperCase());
		AbstractType<?> type = CQLTypeParser.parse(keyspace, row.getString("type"), types);
		if (order == (DESC))
			type = ReversedType.getInstance(type);

		ColumnIdentifier name = ColumnIdentifier.getInterned(type, row.getBytes("column_name_bytes"), row.getString("column_name"));
		return new ColumnDefinition(keyspace, table, name, type, position, kind);
	}

	private static Map<ByteBuffer, CFMetaData.DroppedColumn> fetchDroppedColumns(String keyspace, String table) {
		String query = String.format("SELECT * FROM %s.%s WHERE keyspace_name = ? AND table_name = ?", SchemaConstants.SCHEMA_KEYSPACE_NAME, SchemaKeyspace.DROPPED_COLUMNS);
		Map<ByteBuffer, CFMetaData.DroppedColumn> columns = new HashMap<>();
		for (UntypedResultSet.Row row : SchemaKeyspace.query(query, keyspace, table)) {
			CFMetaData.DroppedColumn column = SchemaKeyspace.createDroppedColumnFromRow(row);
			columns.put(UTF8Type.instance.decompose(column.name), column);
		}
		return columns;
	}

	private static CFMetaData.DroppedColumn createDroppedColumnFromRow(UntypedResultSet.Row row) {
		String keyspace = row.getString("keyspace_name");
		String name = row.getString("column_name");
		AbstractType<?> type = CQLTypeParser.parse(keyspace, row.getString("type"), org.apache.cassandra.schema.Types.none());
		long droppedTime = TimeUnit.MILLISECONDS.toMicros(row.getLong("dropped_time"));
		return new CFMetaData.DroppedColumn(name, type, droppedTime);
	}

	private static org.apache.cassandra.schema.Indexes fetchIndexes(String keyspace, String table) {
		String query = String.format("SELECT * FROM %s.%s WHERE keyspace_name = ? AND table_name = ?", SchemaConstants.SCHEMA_KEYSPACE_NAME, SchemaKeyspace.INDEXES);
		org.apache.cassandra.schema.Indexes.Builder indexes = org.apache.cassandra.schema.Indexes.builder();
		SchemaKeyspace.query(query, keyspace, table).forEach(( row) -> indexes.add(SchemaKeyspace.createIndexMetadataFromRow(row)));
		return indexes.build();
	}

	private static IndexMetadata createIndexMetadataFromRow(UntypedResultSet.Row row) {
		String name = row.getString("index_name");
		IndexMetadata.Kind type = valueOf(row.getString("kind"));
		Map<String, String> options = row.getFrozenTextMap("options");
		return IndexMetadata.fromSchemaMetadata(name, type, options);
	}

	private static org.apache.cassandra.schema.Triggers fetchTriggers(String keyspace, String table) {
		String query = String.format("SELECT * FROM %s.%s WHERE keyspace_name = ? AND table_name = ?", SchemaConstants.SCHEMA_KEYSPACE_NAME, SchemaKeyspace.TRIGGERS);
		org.apache.cassandra.schema.Triggers.Builder triggers = org.apache.cassandra.schema.Triggers.builder();
		SchemaKeyspace.query(query, keyspace, table).forEach(( row) -> triggers.add(SchemaKeyspace.createTriggerFromRow(row)));
		return triggers.build();
	}

	private static TriggerMetadata createTriggerFromRow(UntypedResultSet.Row row) {
		String name = row.getString("trigger_name");
		String classOption = row.getFrozenTextMap("options").get("class");
		return new TriggerMetadata(name, classOption);
	}

	private static org.apache.cassandra.schema.Views fetchViews(String keyspaceName, org.apache.cassandra.schema.Types types) {
		String query = String.format("SELECT view_name FROM %s.%s WHERE keyspace_name = ?", SchemaConstants.SCHEMA_KEYSPACE_NAME, SchemaKeyspace.VIEWS);
		org.apache.cassandra.schema.Views.Builder views = org.apache.cassandra.schema.Views.builder();
		for (UntypedResultSet.Row row : SchemaKeyspace.query(query, keyspaceName))
			views.add(SchemaKeyspace.fetchView(keyspaceName, row.getString("view_name"), types));

		return views.build();
	}

	private static ViewDefinition fetchView(String keyspaceName, String viewName, org.apache.cassandra.schema.Types types) {
		String query = String.format("SELECT * FROM %s.%s WHERE keyspace_name = ? AND view_name = ?", SchemaConstants.SCHEMA_KEYSPACE_NAME, SchemaKeyspace.VIEWS);
		UntypedResultSet rows = SchemaKeyspace.query(query, keyspaceName, viewName);
		if (rows.isEmpty())
			throw new RuntimeException(String.format("%s:%s not found in the schema definitions keyspace.", keyspaceName, viewName));

		UntypedResultSet.Row row = rows.one();
		UUID id = row.getUUID("id");
		UUID baseTableId = row.getUUID("base_table_id");
		String baseTableName = row.getString("base_table_name");
		boolean includeAll = row.getBoolean("include_all_columns");
		String whereClause = row.getString("where_clause");
		List<ColumnDefinition> columns = SchemaKeyspace.fetchColumns(keyspaceName, viewName, types);
		Map<ByteBuffer, CFMetaData.DroppedColumn> droppedColumns = SchemaKeyspace.fetchDroppedColumns(keyspaceName, viewName);
		CFMetaData cfm = CFMetaData.create(keyspaceName, viewName, id, false, true, false, false, true, columns, DatabaseDescriptor.getPartitioner()).params(SchemaKeyspace.createTableParamsFromRow(row)).droppedColumns(droppedColumns);
		String rawSelect = View.buildSelectStatement(baseTableName, columns, whereClause);
		SelectStatement.RawStatement rawStatement = ((SelectStatement.RawStatement) (QueryProcessor.parseStatement(rawSelect)));
		return new ViewDefinition(keyspaceName, viewName, baseTableId, baseTableName, includeAll, rawStatement, whereClause, cfm);
	}

	private static org.apache.cassandra.schema.Functions fetchFunctions(String keyspaceName, org.apache.cassandra.schema.Types types) {
		org.apache.cassandra.schema.Functions udfs = SchemaKeyspace.fetchUDFs(keyspaceName, types);
		org.apache.cassandra.schema.Functions udas = SchemaKeyspace.fetchUDAs(keyspaceName, udfs, types);
		return org.apache.cassandra.schema.Functions.builder().add(udfs).add(udas).build();
	}

	private static org.apache.cassandra.schema.Functions fetchUDFs(String keyspaceName, org.apache.cassandra.schema.Types types) {
		String query = String.format("SELECT * FROM %s.%s WHERE keyspace_name = ?", SchemaConstants.SCHEMA_KEYSPACE_NAME, SchemaKeyspace.FUNCTIONS);
		org.apache.cassandra.schema.Functions.Builder functions = org.apache.cassandra.schema.Functions.builder();
		for (UntypedResultSet.Row row : SchemaKeyspace.query(query, keyspaceName))
			functions.add(SchemaKeyspace.createUDFFromRow(row, types));

		return functions.build();
	}

	private static UDFunction createUDFFromRow(UntypedResultSet.Row row, org.apache.cassandra.schema.Types types) {
		String ksName = row.getString("keyspace_name");
		String functionName = row.getString("function_name");
		FunctionName name = new FunctionName(ksName, functionName);
		List<ColumnIdentifier> argNames = new ArrayList<>();
		for (String arg : row.getFrozenList("argument_names", UTF8Type.instance))
			argNames.add(new ColumnIdentifier(arg, true));

		List<AbstractType<?>> argTypes = new ArrayList<>();
		for (String type : row.getFrozenList("argument_types", UTF8Type.instance))
			argTypes.add(CQLTypeParser.parse(ksName, type, types));

		AbstractType<?> returnType = CQLTypeParser.parse(ksName, row.getString("return_type"), types);
		String language = row.getString("language");
		String body = row.getString("body");
		boolean calledOnNullInput = row.getBoolean("called_on_null_input");
		org.apache.cassandra.cql3.functions.Function existing = Schema.instance.findFunction(name, argTypes).orElse(null);
		if (existing instanceof UDFunction) {
			UDFunction udf = ((UDFunction) (existing));
			if ((((((udf.argNames().equals(argNames)) && (udf.returnType().equals(returnType))) && (!(udf.isAggregate()))) && (udf.language().equals(language))) && (udf.body().equals(body))) && ((udf.isCalledOnNullInput()) == calledOnNullInput)) {
				SchemaKeyspace.logger.trace("Skipping duplicate compilation of already existing UDF {}", name);
				return udf;
			}
		}
		try {
			return UDFunction.create(name, argNames, argTypes, returnType, calledOnNullInput, language, body);
		} catch (InvalidRequestException e) {
			SchemaKeyspace.logger.error(String.format("Cannot load function '%s' from schema: this function won't be available (on this node)", name), e);
			return UDFunction.createBrokenFunction(name, argNames, argTypes, returnType, calledOnNullInput, language, body, e);
		}
	}

	private static org.apache.cassandra.schema.Functions fetchUDAs(String keyspaceName, org.apache.cassandra.schema.Functions udfs, org.apache.cassandra.schema.Types types) {
		String query = String.format("SELECT * FROM %s.%s WHERE keyspace_name = ?", SchemaConstants.SCHEMA_KEYSPACE_NAME, SchemaKeyspace.AGGREGATES);
		org.apache.cassandra.schema.Functions.Builder aggregates = org.apache.cassandra.schema.Functions.builder();
		for (UntypedResultSet.Row row : SchemaKeyspace.query(query, keyspaceName))
			aggregates.add(SchemaKeyspace.createUDAFromRow(row, udfs, types));

		return aggregates.build();
	}

	private static UDAggregate createUDAFromRow(UntypedResultSet.Row row, org.apache.cassandra.schema.Functions functions, org.apache.cassandra.schema.Types types) {
		String ksName = row.getString("keyspace_name");
		String functionName = row.getString("aggregate_name");
		FunctionName name = new FunctionName(ksName, functionName);
		List<AbstractType<?>> argTypes = row.getFrozenList("argument_types", UTF8Type.instance).stream().map(( t) -> CQLTypeParser.parse(ksName, t, types)).collect(Collectors.toList());
		AbstractType<?> returnType = CQLTypeParser.parse(ksName, row.getString("return_type"), types);
		FunctionName stateFunc = new FunctionName(ksName, row.getString("state_func"));
		FunctionName finalFunc = (row.has("final_func")) ? new FunctionName(ksName, row.getString("final_func")) : null;
		AbstractType<?> stateType = (row.has("state_type")) ? CQLTypeParser.parse(ksName, row.getString("state_type"), types) : null;
		ByteBuffer initcond = (row.has("initcond")) ? Terms.asBytes(ksName, row.getString("initcond"), stateType) : null;
		try {
			return UDAggregate.create(functions, name, argTypes, returnType, stateFunc, finalFunc, stateType, initcond);
		} catch (InvalidRequestException reason) {
			return UDAggregate.createBroken(name, argTypes, returnType, initcond, reason);
		}
	}

	private static UntypedResultSet query(String query, Object... variables) {
		return QueryProcessor.executeInternal(query, variables);
	}

	public static synchronized void reloadSchemaAndAnnounceVersion() {
		org.apache.cassandra.schema.Keyspaces before = Schema.instance.getReplicatedKeyspaces();
		org.apache.cassandra.schema.Keyspaces after = SchemaKeyspace.fetchNonSystemKeyspaces();
		SchemaKeyspace.mergeSchema(before, after);
		Schema.instance.updateVersionAndAnnounce();
	}

	public static synchronized void mergeSchemaAndAnnounceVersion(Collection<Mutation> mutations) throws ConfigurationException {
		SchemaKeyspace.mergeSchema(mutations);
		Schema.instance.updateVersionAndAnnounce();
	}

	public static synchronized void mergeSchema(Collection<Mutation> mutations) {
		Set<String> affectedKeyspaces = mutations.stream().map(( m) -> UTF8Type.instance.compose(m.key().getKey())).collect(Collectors.toSet());
		org.apache.cassandra.schema.Keyspaces before = Schema.instance.getKeyspaces(affectedKeyspaces);
		mutations.forEach(Mutation::apply);
		if (SchemaKeyspace.FLUSH_SCHEMA_TABLES)
			SchemaKeyspace.flush();

		org.apache.cassandra.schema.Keyspaces after = SchemaKeyspace.fetchKeyspacesOnly(affectedKeyspaces);
		SchemaKeyspace.mergeSchema(before, after);
	}

	private static synchronized void mergeSchema(org.apache.cassandra.schema.Keyspaces before, org.apache.cassandra.schema.Keyspaces after) {
	}

	private static void updateKeyspace(String keyspaceName, KeyspaceMetadata keyspaceBefore, KeyspaceMetadata keyspaceAfter) {
		Map<Pair<FunctionName, List<String>>, UDFunction> udfsBefore = new HashMap<>();
		keyspaceBefore.functions.udfs().forEach(( f) -> udfsBefore.put(Pair.create(f.name(), SchemaKeyspace.functionArgumentsList(f)), f));
		Map<Pair<FunctionName, List<String>>, UDFunction> udfsAfter = new HashMap<>();
		keyspaceAfter.functions.udfs().forEach(( f) -> udfsAfter.put(Pair.create(f.name(), SchemaKeyspace.functionArgumentsList(f)), f));
		MapDifference<Pair<FunctionName, List<String>>, UDFunction> udfsDiff = Maps.difference(udfsBefore, udfsAfter);
		Map<Pair<FunctionName, List<String>>, UDAggregate> udasBefore = new HashMap<>();
		keyspaceBefore.functions.udas().forEach(( f) -> udasBefore.put(Pair.create(f.name(), SchemaKeyspace.functionArgumentsList(f)), f));
		Map<Pair<FunctionName, List<String>>, UDAggregate> udasAfter = new HashMap<>();
		keyspaceAfter.functions.udas().forEach(( f) -> udasAfter.put(Pair.create(f.name(), SchemaKeyspace.functionArgumentsList(f)), f));
		MapDifference<Pair<FunctionName, List<String>>, UDAggregate> udasDiff = Maps.difference(udasBefore, udasAfter);
		if (!(keyspaceBefore.params.equals(keyspaceAfter.params)))
			Schema.instance.updateKeyspace(keyspaceName, keyspaceAfter.params);

		udasDiff.entriesOnlyOnLeft().values().forEach(Schema.instance::dropAggregate);
		udfsDiff.entriesOnlyOnLeft().values().forEach(Schema.instance::dropFunction);
		udfsDiff.entriesOnlyOnRight().values().forEach(Schema.instance::addFunction);
		udasDiff.entriesOnlyOnRight().values().forEach(Schema.instance::addAggregate);
		for (MapDifference.ValueDifference<UDFunction> diff : udfsDiff.entriesDiffering().values())
			Schema.instance.updateFunction(diff.rightValue());

		for (MapDifference.ValueDifference<UDAggregate> diff : udasDiff.entriesDiffering().values())
			Schema.instance.updateAggregate(diff.rightValue());

	}

	private static AbstractType<?> expandUserTypes(AbstractType<?> original) {
		if (original instanceof UserType)
			return new TupleType(SchemaKeyspace.expandUserTypes(((UserType) (original)).fieldTypes()));

		if (original instanceof TupleType)
			return new TupleType(SchemaKeyspace.expandUserTypes(((TupleType) (original)).allTypes()));

		if (original instanceof ListType<?>)
			return ListType.getInstance(SchemaKeyspace.expandUserTypes(((ListType<?>) (original)).getElementsType()), original.isMultiCell());

		if (original instanceof MapType<?, ?>) {
			MapType<?, ?> mt = ((MapType<?, ?>) (original));
			return MapType.getInstance(SchemaKeyspace.expandUserTypes(mt.getKeysType()), SchemaKeyspace.expandUserTypes(mt.getValuesType()), mt.isMultiCell());
		}
		if (original instanceof SetType<?>)
			return SetType.getInstance(SchemaKeyspace.expandUserTypes(((SetType<?>) (original)).getElementsType()), original.isMultiCell());

		if (original instanceof ReversedType<?>)
			return ReversedType.getInstance(SchemaKeyspace.expandUserTypes(((ReversedType) (original)).baseType));

		if (original instanceof CompositeType)
			return CompositeType.getInstance(SchemaKeyspace.expandUserTypes(original.getComponents()));

		return original;
	}

	private static List<AbstractType<?>> expandUserTypes(List<AbstractType<?>> types) {
		return types.stream().map(SchemaKeyspace::expandUserTypes).collect(Collectors.toList());
	}

	private static class MissingColumns extends RuntimeException {
		MissingColumns(String message) {
			super(message);
		}
	}
}

