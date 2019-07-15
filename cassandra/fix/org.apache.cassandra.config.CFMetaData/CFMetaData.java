

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.cassandra.auth.DataResource;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.ReadRepairDecision;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.config.SchemaConstants;
import org.apache.cassandra.cql3.CQLStatement;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.cql3.QueryProcessor;
import org.apache.cassandra.cql3.SuperColumnCompatibility;
import org.apache.cassandra.cql3.statements.CFStatement;
import org.apache.cassandra.cql3.statements.CreateTableStatement;
import org.apache.cassandra.cql3.statements.ParsedStatement;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Columns;
import org.apache.cassandra.db.CompactTables;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.Directories;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.PartitionColumns;
import org.apache.cassandra.db.Serializers;
import org.apache.cassandra.db.compaction.AbstractCompactionStrategy;
import org.apache.cassandra.db.filter.ColumnFilter;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.CollectionType;
import org.apache.cassandra.db.marshal.CompositeType;
import org.apache.cassandra.db.marshal.CounterColumnType;
import org.apache.cassandra.db.marshal.MapType;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.cassandra.db.view.View;
import org.apache.cassandra.db.view.ViewManager;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.schema.CachingParams;
import org.apache.cassandra.schema.CompactionParams;
import org.apache.cassandra.schema.CompressionParams;
import org.apache.cassandra.schema.IndexMetadata;
import org.apache.cassandra.schema.Indexes;
import org.apache.cassandra.schema.KeyspaceMetadata;
import org.apache.cassandra.schema.SpeculativeRetryParam;
import org.apache.cassandra.schema.TableParams;
import org.apache.cassandra.schema.Triggers;
import org.apache.cassandra.schema.Types;
import org.apache.cassandra.utils.AbstractIterator;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.Pair;
import org.apache.cassandra.utils.UUIDGen;
import org.apache.cassandra.utils.UUIDSerializer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.github.jamm.Unmetered;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.config.ColumnDefinition.Kind.CLUSTERING;
import static org.apache.cassandra.config.ColumnDefinition.Kind.PARTITION_KEY;
import static org.apache.cassandra.config.ColumnDefinition.Kind.REGULAR;
import static org.apache.cassandra.config.ColumnDefinition.Kind.STATIC;


@Unmetered
public final class CFMetaData {
	public enum Flag {

		SUPER,
		COUNTER,
		DENSE,
		COMPOUND;}

	private static final Pattern PATTERN_WORD_CHARS = Pattern.compile("\\w+");

	private static final Logger logger = LoggerFactory.getLogger(CFMetaData.class);

	public static final CFMetaData.Serializer serializer = new CFMetaData.Serializer();

	public final UUID cfId;

	public final String ksName;

	public final String cfName;

	public final Pair<String, String> ksAndCFName;

	public final byte[] ksAndCFBytes;

	private final boolean isCounter;

	private final boolean isView;

	private final boolean isIndex;

	public volatile ClusteringComparator comparator;

	public final IPartitioner partitioner;

	private volatile AbstractType<?> keyValidator;

	private final Serializers serializers;

	private volatile ImmutableSet<CFMetaData.Flag> flags;

	private volatile boolean isDense;

	private volatile boolean isCompound;

	private volatile boolean isSuper;

	public volatile TableParams params = TableParams.DEFAULT;

	private volatile Map<ByteBuffer, CFMetaData.DroppedColumn> droppedColumns = new HashMap<>();

	private volatile Triggers triggers = Triggers.none();

	private volatile Indexes indexes = Indexes.none();

	private volatile Map<ByteBuffer, ColumnDefinition> columnMetadata = new HashMap<>();

	private volatile List<ColumnDefinition> partitionKeyColumns;

	private volatile List<ColumnDefinition> clusteringColumns;

	private volatile PartitionColumns partitionColumns;

	private volatile ColumnDefinition compactValueColumn;

	public final DataResource resource;

	private volatile ColumnFilter allColumnFilter;

	private volatile ColumnDefinition superCfKeyColumn;

	private volatile ColumnDefinition superCfValueColumn;

	private volatile CFMetaData nonCompactCopy = null;

	public boolean isSuperColumnKeyColumn(ColumnDefinition cd) {
		return cd.name.equals(superCfKeyColumn.name);
	}

	public boolean isSuperColumnValueColumn(ColumnDefinition cd) {
		return cd.name.equals(superCfValueColumn.name);
	}

	public ColumnDefinition superColumnValueColumn() {
		return superCfValueColumn;
	}

	public ColumnDefinition superColumnKeyColumn() {
		return superCfKeyColumn;
	}

	public CFMetaData params(TableParams params) {
		this.params = params;
		return this;
	}

	public CFMetaData bloomFilterFpChance(double prop) {
		params = TableParams.builder(params).bloomFilterFpChance(prop).build();
		return this;
	}

	public CFMetaData caching(CachingParams prop) {
		params = TableParams.builder(params).caching(prop).build();
		return this;
	}

	public CFMetaData comment(String prop) {
		params = TableParams.builder(params).comment(prop).build();
		return this;
	}

	public CFMetaData compaction(CompactionParams prop) {
		params = TableParams.builder(params).compaction(prop).build();
		return this;
	}

	public CFMetaData compression(CompressionParams prop) {
		params = TableParams.builder(params).compression(prop).build();
		return this;
	}

	public CFMetaData dcLocalReadRepairChance(double prop) {
		params = TableParams.builder(params).dcLocalReadRepairChance(prop).build();
		return this;
	}

	public CFMetaData defaultTimeToLive(int prop) {
		params = TableParams.builder(params).defaultTimeToLive(prop).build();
		return this;
	}

	public CFMetaData gcGraceSeconds(int prop) {
		params = TableParams.builder(params).gcGraceSeconds(prop).build();
		return this;
	}

	public CFMetaData maxIndexInterval(int prop) {
		params = TableParams.builder(params).maxIndexInterval(prop).build();
		return this;
	}

	public CFMetaData memtableFlushPeriod(int prop) {
		params = TableParams.builder(params).memtableFlushPeriodInMs(prop).build();
		return this;
	}

	public CFMetaData minIndexInterval(int prop) {
		params = TableParams.builder(params).minIndexInterval(prop).build();
		return this;
	}

	public CFMetaData readRepairChance(double prop) {
		params = TableParams.builder(params).readRepairChance(prop).build();
		return this;
	}

	public CFMetaData crcCheckChance(double prop) {
		params = TableParams.builder(params).crcCheckChance(prop).build();
		return this;
	}

	public CFMetaData speculativeRetry(SpeculativeRetryParam prop) {
		params = TableParams.builder(params).speculativeRetry(prop).build();
		return this;
	}

	public CFMetaData extensions(Map<String, ByteBuffer> extensions) {
		params = TableParams.builder(params).extensions(extensions).build();
		return this;
	}

	public CFMetaData droppedColumns(Map<ByteBuffer, CFMetaData.DroppedColumn> cols) {
		droppedColumns = cols;
		return this;
	}

	public CFMetaData triggers(Triggers prop) {
		triggers = prop;
		return this;
	}

	public CFMetaData indexes(Indexes indexes) {
		this.indexes = indexes;
		return this;
	}

	private CFMetaData(String keyspace, String name, UUID cfId, boolean isSuper, boolean isCounter, boolean isDense, boolean isCompound, boolean isView, List<ColumnDefinition> partitionKeyColumns, List<ColumnDefinition> clusteringColumns, PartitionColumns partitionColumns, IPartitioner partitioner, ColumnDefinition superCfKeyColumn, ColumnDefinition superCfValueColumn) {
		this.cfId = cfId;
		this.ksName = keyspace;
		this.cfName = name;
		ksAndCFName = Pair.create(keyspace, name);
		byte[] ksBytes = FBUtilities.toWriteUTFBytes(ksName);
		byte[] cfBytes = FBUtilities.toWriteUTFBytes(cfName);
		ksAndCFBytes = Arrays.copyOf(ksBytes, ((ksBytes.length) + (cfBytes.length)));
		System.arraycopy(cfBytes, 0, ksAndCFBytes, ksBytes.length, cfBytes.length);
		this.isDense = (isSuper) ? isDense || (SuperColumnCompatibility.recalculateIsDense(partitionColumns.regulars)) : isDense;
		this.isCompound = isCompound;
		this.isSuper = isSuper;
		this.isCounter = isCounter;
		this.isView = isView;
		EnumSet<CFMetaData.Flag> flags = EnumSet.noneOf(CFMetaData.Flag.class);
		if (isSuper)
			flags.add(CFMetaData.Flag.SUPER);

		if (isCounter)
			flags.add(CFMetaData.Flag.COUNTER);

		if (isDense)
			flags.add(CFMetaData.Flag.DENSE);

		if (isCompound)
			flags.add(CFMetaData.Flag.COMPOUND);

		this.flags = Sets.immutableEnumSet(flags);
		isIndex = cfName.contains(".");
		assert partitioner != null : "This assertion failure is probably due to accessing Schema.instance " + "from client-mode tools - See CASSANDRA-8143.";
		this.partitioner = partitioner;
		assert (isCQLTable()) || (!(clusteringColumns.isEmpty())) : String.format("For table %s.%s, isDense=%b, isCompound=%b, clustering=%s", ksName, cfName, isDense, isCompound, clusteringColumns);
		assert !(partitionKeyColumns.isEmpty()) : String.format("Have no partition keys for table %s.%s", ksName, cfName);
		this.partitionKeyColumns = partitionKeyColumns;
		this.clusteringColumns = clusteringColumns;
		this.partitionColumns = partitionColumns;
		this.superCfKeyColumn = superCfKeyColumn;
		this.superCfValueColumn = superCfValueColumn;
		rebuild();
		this.resource = DataResource.table(ksName, cfName);
		serializers = null;
	}

	private void rebuild() {
		this.nonCompactCopy = null;
		if (isCompactTable()) {
			this.compactValueColumn = (isSuper()) ? SuperColumnCompatibility.getCompactValueColumn(partitionColumns) : CompactTables.getCompactValueColumn(partitionColumns);
		}
		Map<ByteBuffer, ColumnDefinition> newColumnMetadata = Maps.newHashMapWithExpectedSize((((partitionKeyColumns.size()) + (clusteringColumns.size())) + (partitionColumns.size())));
		if ((isSuper()) && (isDense())) {
			CompactTables.DefaultNames defaultNames = SuperColumnCompatibility.columnNameGenerator(partitionKeyColumns, clusteringColumns, partitionColumns);
			if ((superCfKeyColumn) == null) {
			}
			if ((superCfValueColumn) == null) {
			}
			for (ColumnDefinition def : partitionKeyColumns)
				newColumnMetadata.put(def.name.bytes, def);

			newColumnMetadata.put(clusteringColumns.get(0).name.bytes, clusteringColumns.get(0));
			newColumnMetadata.put(superCfKeyColumn.name.bytes, SuperColumnCompatibility.getSuperCfSschemaRepresentation(superCfKeyColumn));
			newColumnMetadata.put(superCfValueColumn.name.bytes, superCfValueColumn);
			newColumnMetadata.put(compactValueColumn.name.bytes, compactValueColumn);
			clusteringColumns = Arrays.asList(clusteringColumns().get(0));
			partitionColumns = PartitionColumns.of(compactValueColumn);
		}else {
			for (ColumnDefinition def : partitionKeyColumns)
				newColumnMetadata.put(def.name.bytes, def);

			for (ColumnDefinition def : clusteringColumns)
				newColumnMetadata.put(def.name.bytes, def);

			for (ColumnDefinition def : partitionColumns)
				newColumnMetadata.put(def.name.bytes, def);

		}
		this.columnMetadata = newColumnMetadata;
		List<AbstractType<?>> keyTypes = CFMetaData.extractTypes(partitionKeyColumns);
		this.keyValidator = ((keyTypes.size()) == 1) ? keyTypes.get(0) : CompositeType.getInstance(keyTypes);
		if (isSuper())
			this.comparator = new ClusteringComparator(clusteringColumns.get(0).type);
		else
			this.comparator = new ClusteringComparator(CFMetaData.extractTypes(clusteringColumns));

	}

	public Indexes getIndexes() {
		return indexes;
	}

	public ColumnFilter getAllColumnFilter() {
		return allColumnFilter;
	}

	public static CFMetaData create(String ksName, String name, UUID cfId, boolean isDense, boolean isCompound, boolean isSuper, boolean isCounter, boolean isView, List<ColumnDefinition> columns, IPartitioner partitioner) {
		List<ColumnDefinition> partitions = new ArrayList<>();
		List<ColumnDefinition> clusterings = new ArrayList<>();
		PartitionColumns.Builder builder = PartitionColumns.builder();
		for (ColumnDefinition column : columns) {
			switch (column.kind) {
				case PARTITION_KEY :
					partitions.add(column);
					break;
				case CLUSTERING :
					clusterings.add(column);
					break;
				default :
					builder.add(column);
					break;
			}
		}
		Collections.sort(partitions);
		Collections.sort(clusterings);
		return new CFMetaData(ksName, name, cfId, isSuper, isCounter, isDense, isCompound, isView, partitions, clusterings, builder.build(), partitioner, null, null);
	}

	public static List<AbstractType<?>> extractTypes(Iterable<ColumnDefinition> clusteringColumns) {
		List<AbstractType<?>> types = new ArrayList<>();
		for (ColumnDefinition def : clusteringColumns)
			types.add(def.type);

		return types;
	}

	public Set<CFMetaData.Flag> flags() {
		return flags;
	}

	public static CFMetaData createFake(String keyspace, String name) {
		return CFMetaData.Builder.create(keyspace, name).addPartitionKey("key", BytesType.instance).build();
	}

	public Triggers getTriggers() {
		return triggers;
	}

	public static CFMetaData compile(String cql, String keyspace) {
		CFStatement parsed = ((CFStatement) (QueryProcessor.parseStatement(cql)));
		parsed.prepareKeyspace(keyspace);
		CreateTableStatement statement = ((CreateTableStatement) (((CreateTableStatement.RawStatement) (parsed)).prepare(Types.none()).statement));
		return null;
	}

	public static UUID generateLegacyCfId(String ksName, String cfName) {
		return UUID.nameUUIDFromBytes(ArrayUtils.addAll(ksName.getBytes(), cfName.getBytes()));
	}

	public CFMetaData reloadIndexMetadataProperties(CFMetaData parent) {
		TableParams.Builder indexParams = TableParams.builder(parent.params);
		if (parent.params.caching.cacheKeys())
			indexParams.caching(CachingParams.CACHE_KEYS);
		else
			indexParams.caching(CachingParams.CACHE_NOTHING);

		indexParams.readRepairChance(0.0).dcLocalReadRepairChance(0.0).gcGraceSeconds(0);
		return params(indexParams.build());
	}

	public CFMetaData asNonCompact() {
		assert isCompactTable() : "Can't get non-compact version of a CQL table";
		if ((nonCompactCopy) == null) {
			nonCompactCopy = CFMetaData.copyOpts(new CFMetaData(ksName, cfName, cfId, false, isCounter, false, true, isView, CFMetaData.copy(partitionKeyColumns), CFMetaData.copy(clusteringColumns), CFMetaData.copy(partitionColumns), partitioner, superCfKeyColumn, superCfValueColumn), this);
		}
		return nonCompactCopy;
	}

	public CFMetaData copy() {
		return copy(cfId);
	}

	public CFMetaData copy(UUID newCfId) {
		return CFMetaData.copyOpts(new CFMetaData(ksName, cfName, newCfId, isSuper(), isCounter(), isDense(), isCompound(), isView(), CFMetaData.copy(partitionKeyColumns), CFMetaData.copy(clusteringColumns), CFMetaData.copy(partitionColumns), partitioner, superCfKeyColumn, superCfValueColumn), this);
	}

	public CFMetaData copy(IPartitioner partitioner) {
		return CFMetaData.copyOpts(new CFMetaData(ksName, cfName, cfId, isSuper, isCounter, isDense, isCompound, isView, CFMetaData.copy(partitionKeyColumns), CFMetaData.copy(clusteringColumns), CFMetaData.copy(partitionColumns), partitioner, superCfKeyColumn, superCfValueColumn), this);
	}

	private static List<ColumnDefinition> copy(List<ColumnDefinition> l) {
		List<ColumnDefinition> copied = new ArrayList<>(l.size());
		for (ColumnDefinition cd : l)
			copied.add(cd.copy());

		return copied;
	}

	private static PartitionColumns copy(PartitionColumns columns) {
		PartitionColumns.Builder newColumns = PartitionColumns.builder();
		for (ColumnDefinition cd : columns)
			newColumns.add(cd.copy());

		return newColumns.build();
	}

	@com.google.common.annotations.VisibleForTesting
	public static CFMetaData copyOpts(CFMetaData newCFMD, CFMetaData oldCFMD) {
		return newCFMD.params(oldCFMD.params).droppedColumns(new HashMap<>(oldCFMD.droppedColumns)).triggers(oldCFMD.triggers).indexes(oldCFMD.indexes);
	}

	public String indexColumnFamilyName(IndexMetadata info) {
		return ((cfName) + (Directories.SECONDARY_INDEX_NAME_SEPARATOR)) + (info.name);
	}

	public boolean isIndex() {
		return isIndex;
	}

	public DecoratedKey decorateKey(ByteBuffer key) {
		return partitioner.decorateKey(key);
	}

	public Map<ByteBuffer, ColumnDefinition> getColumnMetadata() {
		return columnMetadata;
	}

	public String getParentColumnFamilyName() {
		return isIndex ? cfName.substring(0, cfName.indexOf('.')) : null;
	}

	public ReadRepairDecision newReadRepairDecision() {
		double chance = ThreadLocalRandom.current().nextDouble();
		if ((params.readRepairChance) > chance)
			return ReadRepairDecision.GLOBAL;

		if ((params.dcLocalReadRepairChance) > chance)
			return ReadRepairDecision.DC_LOCAL;

		return ReadRepairDecision.NONE;
	}

	public AbstractType<?> getColumnDefinitionNameComparator(ColumnDefinition.Kind kind) {
		return ((isSuper()) && (kind == (REGULAR))) || ((isStaticCompactTable()) && (kind == (STATIC))) ? thriftColumnNameType() : UTF8Type.instance;
	}

	public AbstractType<?> getKeyValidator() {
		return keyValidator;
	}

	public Collection<ColumnDefinition> allColumns() {
		return columnMetadata.values();
	}

	private Iterator<ColumnDefinition> nonPkColumnIterator() {
		return null;
	}

	public Iterator<ColumnDefinition> allColumnsInSelectOrder() {
		return new AbstractIterator<ColumnDefinition>() {
			private final Iterator<ColumnDefinition> partitionKeyIter = partitionKeyColumns.iterator();

			private final Iterator<ColumnDefinition> clusteringIter = (isStaticCompactTable()) ? Collections.<ColumnDefinition>emptyIterator() : clusteringColumns.iterator();

			private final Iterator<ColumnDefinition> otherColumns = nonPkColumnIterator();

			protected ColumnDefinition computeNext() {
				if (partitionKeyIter.hasNext())
					return partitionKeyIter.next();

				if (clusteringIter.hasNext())
					return clusteringIter.next();

				return otherColumns.hasNext() ? otherColumns.next() : endOfData();
			}
		};
	}

	public Iterable<ColumnDefinition> primaryKeyColumns() {
		return Iterables.concat(partitionKeyColumns, clusteringColumns);
	}

	public List<ColumnDefinition> partitionKeyColumns() {
		return partitionKeyColumns;
	}

	public List<ColumnDefinition> clusteringColumns() {
		return clusteringColumns;
	}

	public PartitionColumns partitionColumns() {
		return partitionColumns;
	}

	public ColumnDefinition compactValueColumn() {
		return compactValueColumn;
	}

	public ClusteringComparator getKeyValidatorAsClusteringComparator() {
		boolean isCompound = (keyValidator) instanceof CompositeType;
		List<AbstractType<?>> types = (isCompound) ? ((CompositeType) (keyValidator)).types : Collections.<AbstractType<?>>singletonList(keyValidator);
		return new ClusteringComparator(types);
	}

	public static ByteBuffer serializePartitionKey(ClusteringPrefix keyAsClustering) {
		if ((keyAsClustering.size()) == 1)
			return keyAsClustering.get(0);

		ByteBuffer[] values = new ByteBuffer[keyAsClustering.size()];
		for (int i = 0; i < (keyAsClustering.size()); i++)
			values[i] = keyAsClustering.get(i);

		return CompositeType.build(values);
	}

	public Map<ByteBuffer, CFMetaData.DroppedColumn> getDroppedColumns() {
		return droppedColumns;
	}

	public ColumnDefinition getDroppedColumnDefinition(ByteBuffer name) {
		return getDroppedColumnDefinition(name, false);
	}

	public ColumnDefinition getDroppedColumnDefinition(ByteBuffer name, boolean isStatic) {
		CFMetaData.DroppedColumn dropped = droppedColumns.get(name);
		if (dropped == null)
			return null;

		AbstractType<?> type = ((dropped.type) == null) ? BytesType.instance : dropped.type;
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if ((this) == o)
			return true;

		if (!(o instanceof CFMetaData))
			return false;

		CFMetaData other = ((CFMetaData) (o));
		return ((((((((((Objects.equal(cfId, other.cfId)) && (Objects.equal(flags, other.flags))) && (Objects.equal(ksName, other.ksName))) && (Objects.equal(cfName, other.cfName))) && (Objects.equal(params, other.params))) && (Objects.equal(comparator, other.comparator))) && (Objects.equal(keyValidator, other.keyValidator))) && (Objects.equal(columnMetadata, other.columnMetadata))) && (Objects.equal(droppedColumns, other.droppedColumns))) && (Objects.equal(triggers, other.triggers))) && (Objects.equal(indexes, other.indexes));
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(29, 1597).append(cfId).append(ksName).append(cfName).append(flags).append(comparator).append(params).append(keyValidator).append(columnMetadata).append(droppedColumns).append(triggers).append(indexes).toHashCode();
	}

	@com.google.common.annotations.VisibleForTesting
	public boolean apply(CFMetaData cfm) throws ConfigurationException {
		CFMetaData.logger.debug("applying {} to {}", cfm, this);
		validateCompatibility(cfm);
		partitionKeyColumns = cfm.partitionKeyColumns;
		clusteringColumns = cfm.clusteringColumns;
		boolean changeAffectsStatements = !(partitionColumns.equals(cfm.partitionColumns));
		partitionColumns = cfm.partitionColumns;
		superCfKeyColumn = cfm.superCfKeyColumn;
		superCfValueColumn = cfm.superCfValueColumn;
		isDense = cfm.isDense;
		isCompound = cfm.isCompound;
		isSuper = cfm.isSuper;
		flags = cfm.flags;
		rebuild();
		params = cfm.params;
		if (!(cfm.droppedColumns.isEmpty()))
			droppedColumns = cfm.droppedColumns;

		triggers = cfm.triggers;
		changeAffectsStatements |= !(indexes.equals(cfm.indexes));
		indexes = cfm.indexes;
		CFMetaData.logger.debug("application result is {}", this);
		return changeAffectsStatements;
	}

	public void validateCompatibility(CFMetaData cfm) throws ConfigurationException {
		if (!(cfm.ksName.equals(ksName)))
			throw new ConfigurationException(String.format("Keyspace mismatch (found %s; expected %s)", cfm.ksName, ksName));

		if (!(cfm.cfName.equals(cfName)))
			throw new ConfigurationException(String.format("Column family mismatch (found %s; expected %s)", cfm.cfName, cfName));

		if (!(cfm.cfId.equals(cfId)))
			throw new ConfigurationException(String.format("Column family ID mismatch (found %s; expected %s)", cfm.cfId, cfId));

	}

	public static Class<? extends AbstractCompactionStrategy> createCompactionStrategy(String className) throws ConfigurationException {
		className = (className.contains(".")) ? className : "org.apache.cassandra.db.compaction." + className;
		Class<AbstractCompactionStrategy> strategyClass = FBUtilities.classForName(className, "compaction strategy");
		if (!(AbstractCompactionStrategy.class.isAssignableFrom(strategyClass)))
			throw new ConfigurationException(String.format("Specified compaction strategy class (%s) is not derived from AbstractCompactionStrategy", className));

		return strategyClass;
	}

	public static AbstractCompactionStrategy createCompactionStrategyInstance(ColumnFamilyStore cfs, CompactionParams compactionParams) {
		try {
			Constructor<? extends AbstractCompactionStrategy> constructor = compactionParams.klass().getConstructor(ColumnFamilyStore.class, Map.class);
			return constructor.newInstance(cfs, compactionParams.options());
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
			throw new RuntimeException(e);
		}
	}

	public ColumnDefinition getColumnDefinition(ColumnIdentifier name) {
		return getColumnDefinition(name.bytes);
	}

	public ColumnDefinition getColumnDefinition(ByteBuffer name) {
		return columnMetadata.get(name);
	}

	public static boolean isNameValid(String name) {
		return (((name != null) && (!(name.isEmpty()))) && ((name.length()) <= (SchemaConstants.NAME_LENGTH))) && (CFMetaData.PATTERN_WORD_CHARS.matcher(name).matches());
	}

	public CFMetaData validate() throws ConfigurationException {
		rebuild();
		if (!(CFMetaData.isNameValid(ksName)))
			throw new ConfigurationException(String.format("Keyspace name must not be empty, more than %s characters long, or contain non-alphanumeric-underscore characters (got \"%s\")", SchemaConstants.NAME_LENGTH, ksName));

		if (!(CFMetaData.isNameValid(cfName)))
			throw new ConfigurationException(String.format("ColumnFamily name must not be empty, more than %s characters long, or contain non-alphanumeric-underscore characters (got \"%s\")", SchemaConstants.NAME_LENGTH, cfName));

		params.validate();
		for (int i = 0; i < (comparator.size()); i++) {
			if ((comparator.subtype(i)) instanceof CounterColumnType)
				throw new ConfigurationException("CounterColumnType is not a valid comparator");

		}
		if ((keyValidator) instanceof CounterColumnType)
			throw new ConfigurationException("CounterColumnType is not a valid key validator");

		if (isCounter()) {
			for (ColumnDefinition def : partitionColumns())
				if ((!((def.type) instanceof CounterColumnType)) && ((!(isSuper())) || (isSuperColumnValueColumn(def))))
					throw new ConfigurationException((("Cannot add a non counter column (" + def) + ") in a counter column family"));


		}else {
			for (ColumnDefinition def : allColumns())
				if ((def.type) instanceof CounterColumnType)
					throw new ConfigurationException((("Cannot add a counter column (" + (def.name)) + ") in a non counter column family"));


		}
		if ((!(indexes.isEmpty())) && (isSuper()))
			throw new ConfigurationException("Secondary indexes are not supported on super column families");

		KeyspaceMetadata ksm = Schema.instance.getKSMetaData(ksName);
		Set<String> indexNames = (ksm == null) ? new HashSet<>() : ksm.existingIndexNames(cfName);
		for (IndexMetadata index : indexes) {
			if (indexNames.contains(index.name))
				throw new ConfigurationException(("Duplicate index name " + (index.name)));

			indexNames.add(index.name);
		}
		return this;
	}

	public AbstractType<?> thriftColumnNameType() {
		if (isSuper()) {
			ColumnDefinition def = compactValueColumn();
			assert (def != null) && ((def.type) instanceof MapType);
			return ((MapType) (def.type)).nameComparator();
		}
		assert isStaticCompactTable();
		return clusteringColumns.get(0).type;
	}

	public CFMetaData addColumnDefinition(ColumnDefinition def) throws ConfigurationException {
		if (columnMetadata.containsKey(def.name.bytes))
			throw new ConfigurationException(String.format("Cannot add column %s, a column with the same name already exists", def.name));

		return addOrReplaceColumnDefinition(def);
	}

	public CFMetaData addOrReplaceColumnDefinition(ColumnDefinition def) {
		switch (def.kind) {
			case PARTITION_KEY :
				partitionKeyColumns.set(def.position(), def);
				break;
			case CLUSTERING :
				clusteringColumns.set(def.position(), def);
				break;
			case REGULAR :
			case STATIC :
				PartitionColumns.Builder builder = PartitionColumns.builder();
				for (ColumnDefinition column : partitionColumns)
					if (!(column.name.equals(def.name)))
						builder.add(column);


				builder.add(def);
				partitionColumns = builder.build();
				if (isDense())
					this.compactValueColumn = def;

				break;
		}
		this.columnMetadata.put(def.name.bytes, def);
		return this;
	}

	public boolean removeColumnDefinition(ColumnDefinition def) {
		assert !(def.isPartitionKey());
		boolean removed = (columnMetadata.remove(def.name.bytes)) != null;
		if (removed)
			partitionColumns = partitionColumns.without(def);

		return removed;
	}

	public void recordColumnDrop(ColumnDefinition def, long timeMicros) {
		droppedColumns.put(def.name.bytes, new CFMetaData.DroppedColumn(def.name.toString(), def.type, timeMicros));
	}

	public boolean isCQLTable() {
		return ((!(isSuper())) && (!(isDense()))) && (isCompound());
	}

	public boolean isCompactTable() {
		return !(isCQLTable());
	}

	public boolean isStaticCompactTable() {
		return ((!(isSuper())) && (!(isDense()))) && (!(isCompound()));
	}

	public boolean isThriftCompatible() {
		return isCompactTable();
	}

	public boolean hasStaticColumns() {
		return !(partitionColumns.statics.isEmpty());
	}

	public boolean hasCollectionColumns() {
		for (ColumnDefinition def : partitionColumns())
			if (((def.type) instanceof CollectionType) && (def.type.isMultiCell()))
				return true;


		return false;
	}

	public boolean hasComplexColumns() {
		for (ColumnDefinition def : partitionColumns())
			if (def.isComplex())
				return true;


		return false;
	}

	public boolean hasDroppedCollectionColumns() {
		for (CFMetaData.DroppedColumn def : getDroppedColumns().values())
			if (((def.type) instanceof CollectionType) && (def.type.isMultiCell()))
				return true;


		return false;
	}

	public boolean isSuper() {
		return isSuper;
	}

	public boolean isCounter() {
		return isCounter;
	}

	public boolean isDense() {
		return isDense;
	}

	public boolean isCompound() {
		return isCompound;
	}

	public boolean isView() {
		return isView;
	}

	public boolean enforceStrictLiveness() {
		return (isView) && (Keyspace.open(ksName).viewManager.getByName(cfName).enforceStrictLiveness());
	}

	public Serializers serializers() {
		return serializers;
	}

	public AbstractType<?> makeLegacyDefaultValidator() {
		if (isCounter())
			return CounterColumnType.instance;
		else
			if (isCompactTable())
				return isSuper() ? ((MapType) (compactValueColumn().type)).valueComparator() : compactValueColumn().type;
			else
				return BytesType.instance;


	}

	public static Set<CFMetaData.Flag> flagsFromStrings(Set<String> strings) {
		return strings.stream().map(String::toUpperCase).map(CFMetaData.Flag::valueOf).collect(Collectors.toSet());
	}

	public static Set<String> flagsToStrings(Set<CFMetaData.Flag> flags) {
		return flags.stream().map(CFMetaData.Flag::toString).map(String::toLowerCase).collect(Collectors.toSet());
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("cfId", cfId).append("ksName", ksName).append("cfName", cfName).append("flags", flags).append("params", params).append("comparator", comparator).append("partitionColumns", partitionColumns).append("partitionKeyColumns", partitionKeyColumns).append("clusteringColumns", clusteringColumns).append("keyValidator", keyValidator).append("columnMetadata", columnMetadata.values()).append("droppedColumns", droppedColumns).append("triggers", triggers).append("indexes", indexes).toString();
	}

	public static class Builder {
		private final String keyspace;

		private final String table;

		private final boolean isDense;

		private final boolean isCompound;

		private final boolean isSuper;

		private final boolean isCounter;

		private final boolean isView;

		private Optional<IPartitioner> partitioner;

		private UUID tableId;

		private final List<Pair<ColumnIdentifier, AbstractType>> partitionKeys = new ArrayList<>();

		private final List<Pair<ColumnIdentifier, AbstractType>> clusteringColumns = new ArrayList<>();

		private final List<Pair<ColumnIdentifier, AbstractType>> staticColumns = new ArrayList<>();

		private final List<Pair<ColumnIdentifier, AbstractType>> regularColumns = new ArrayList<>();

		private Builder(String keyspace, String table, boolean isDense, boolean isCompound, boolean isSuper, boolean isCounter, boolean isView) {
			this.keyspace = keyspace;
			this.table = table;
			this.isDense = isDense;
			this.isCompound = isCompound;
			this.isSuper = isSuper;
			this.isCounter = isCounter;
			this.isView = isView;
			this.partitioner = Optional.empty();
		}

		public static CFMetaData.Builder create(String keyspace, String table) {
			return CFMetaData.Builder.create(keyspace, table, false, true, false);
		}

		public static CFMetaData.Builder create(String keyspace, String table, boolean isDense, boolean isCompound, boolean isCounter) {
			return CFMetaData.Builder.create(keyspace, table, isDense, isCompound, false, isCounter);
		}

		public static CFMetaData.Builder create(String keyspace, String table, boolean isDense, boolean isCompound, boolean isSuper, boolean isCounter) {
			return new CFMetaData.Builder(keyspace, table, isDense, isCompound, isSuper, isCounter, false);
		}

		public static CFMetaData.Builder createView(String keyspace, String table) {
			return new CFMetaData.Builder(keyspace, table, false, true, false, false, true);
		}

		public static CFMetaData.Builder createDense(String keyspace, String table, boolean isCompound, boolean isCounter) {
			return CFMetaData.Builder.create(keyspace, table, true, isCompound, isCounter);
		}

		public static CFMetaData.Builder createSuper(String keyspace, String table, boolean isCounter) {
			return CFMetaData.Builder.create(keyspace, table, true, true, true, isCounter);
		}

		public CFMetaData.Builder withPartitioner(IPartitioner partitioner) {
			this.partitioner = Optional.ofNullable(partitioner);
			return this;
		}

		public CFMetaData.Builder withId(UUID tableId) {
			this.tableId = tableId;
			return this;
		}

		public CFMetaData.Builder addPartitionKey(String name, AbstractType type) {
			return addPartitionKey(ColumnIdentifier.getInterned(name, false), type);
		}

		public CFMetaData.Builder addPartitionKey(ColumnIdentifier name, AbstractType type) {
			this.partitionKeys.add(Pair.create(name, type));
			return this;
		}

		public CFMetaData.Builder addClusteringColumn(String name, AbstractType type) {
			return addClusteringColumn(ColumnIdentifier.getInterned(name, false), type);
		}

		public CFMetaData.Builder addClusteringColumn(ColumnIdentifier name, AbstractType type) {
			this.clusteringColumns.add(Pair.create(name, type));
			return this;
		}

		public CFMetaData.Builder addRegularColumn(String name, AbstractType type) {
			return addRegularColumn(ColumnIdentifier.getInterned(name, false), type);
		}

		public CFMetaData.Builder addRegularColumn(ColumnIdentifier name, AbstractType type) {
			this.regularColumns.add(Pair.create(name, type));
			return this;
		}

		public boolean hasRegulars() {
			return !(this.regularColumns.isEmpty());
		}

		public CFMetaData.Builder addStaticColumn(String name, AbstractType type) {
			return addStaticColumn(ColumnIdentifier.getInterned(name, false), type);
		}

		public CFMetaData.Builder addStaticColumn(ColumnIdentifier name, AbstractType type) {
			this.staticColumns.add(Pair.create(name, type));
			return this;
		}

		public Set<String> usedColumnNames() {
			Set<String> usedNames = Sets.newHashSetWithExpectedSize(((((partitionKeys.size()) + (clusteringColumns.size())) + (staticColumns.size())) + (regularColumns.size())));
			for (Pair<ColumnIdentifier, AbstractType> p : partitionKeys)
				usedNames.add(p.left.toString());

			for (Pair<ColumnIdentifier, AbstractType> p : clusteringColumns)
				usedNames.add(p.left.toString());

			for (Pair<ColumnIdentifier, AbstractType> p : staticColumns)
				usedNames.add(p.left.toString());

			for (Pair<ColumnIdentifier, AbstractType> p : regularColumns)
				usedNames.add(p.left.toString());

			return usedNames;
		}

		public CFMetaData build() {
			if ((tableId) == null)
				tableId = UUIDGen.getTimeUUID();

			List<ColumnDefinition> partitions = new ArrayList<>(partitionKeys.size());
			List<ColumnDefinition> clusterings = new ArrayList<>(clusteringColumns.size());
			PartitionColumns.Builder builder = PartitionColumns.builder();
			for (int i = 0; i < (partitionKeys.size()); i++) {
				Pair<ColumnIdentifier, AbstractType> p = partitionKeys.get(i);
				partitions.add(new ColumnDefinition(keyspace, table, p.left, p.right, i, PARTITION_KEY));
			}
			for (int i = 0; i < (clusteringColumns.size()); i++) {
				Pair<ColumnIdentifier, AbstractType> p = clusteringColumns.get(i);
				clusterings.add(new ColumnDefinition(keyspace, table, p.left, p.right, i, CLUSTERING));
			}
			for (Pair<ColumnIdentifier, AbstractType> p : regularColumns)
				builder.add(new ColumnDefinition(keyspace, table, p.left, p.right, ColumnDefinition.NO_POSITION, REGULAR));

			for (Pair<ColumnIdentifier, AbstractType> p : staticColumns)
				builder.add(new ColumnDefinition(keyspace, table, p.left, p.right, ColumnDefinition.NO_POSITION, STATIC));

			return new CFMetaData(keyspace, table, tableId, isSuper, isCounter, isDense, isCompound, isView, partitions, clusterings, builder.build(), partitioner.orElseGet(DatabaseDescriptor::getPartitioner), null, null);
		}
	}

	public static class Serializer {
		public void serialize(CFMetaData metadata, DataOutputPlus out, int version) throws IOException {
			UUIDSerializer.serializer.serialize(metadata.cfId, out, version);
		}

		public CFMetaData deserialize(DataInputPlus in, int version) throws IOException {
			UUID cfId = UUIDSerializer.serializer.deserialize(in, version);
			return null;
		}

		public long serializedSize(CFMetaData metadata, int version) {
			return UUIDSerializer.serializer.serializedSize(metadata.cfId, version);
		}
	}

	public static class DroppedColumn {
		public final String name;

		public final AbstractType<?> type;

		public final long droppedTime;

		public DroppedColumn(String name, AbstractType<?> type, long droppedTime) {
			this.name = name;
			this.type = type;
			this.droppedTime = droppedTime;
		}

		@Override
		public boolean equals(Object o) {
			if ((this) == o)
				return true;

			if (!(o instanceof CFMetaData.DroppedColumn))
				return false;

			CFMetaData.DroppedColumn dc = ((CFMetaData.DroppedColumn) (o));
			return ((name.equals(dc.name)) && (type.equals(dc.type))) && ((droppedTime) == (dc.droppedTime));
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(name, type, droppedTime);
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this).add("name", name).add("type", type).add("droppedTime", droppedTime).toString();
		}
	}
}

