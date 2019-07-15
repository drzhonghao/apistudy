

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.cassandra.auth.AuthenticatedUser;
import org.apache.cassandra.auth.DataResource;
import org.apache.cassandra.auth.IAuthorizer;
import org.apache.cassandra.auth.IResource;
import org.apache.cassandra.auth.Permission;
import org.apache.cassandra.auth.RoleResource;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.config.SchemaConstants;
import org.apache.cassandra.cql3.CFName;
import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.statements.CFProperties;
import org.apache.cassandra.cql3.statements.CFStatement;
import org.apache.cassandra.cql3.statements.ParsedStatement;
import org.apache.cassandra.cql3.statements.SchemaAlteringStatement;
import org.apache.cassandra.cql3.statements.TableAttributes;
import org.apache.cassandra.db.CompactTables;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.CounterColumnType;
import org.apache.cassandra.db.marshal.EmptyType;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.cassandra.db.marshal.UserType;
import org.apache.cassandra.exceptions.AlreadyExistsException;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.exceptions.RequestExecutionException;
import org.apache.cassandra.exceptions.RequestValidationException;
import org.apache.cassandra.exceptions.UnauthorizedException;
import org.apache.cassandra.schema.KeyspaceMetadata;
import org.apache.cassandra.schema.TableParams;
import org.apache.cassandra.schema.Types;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.service.MigrationManager;
import org.apache.cassandra.service.QueryState;
import org.apache.cassandra.transport.Event;

import static org.apache.cassandra.config.CFMetaData.Builder.create;
import static org.apache.cassandra.transport.Event.SchemaChange.Change.CREATED;
import static org.apache.cassandra.transport.Event.SchemaChange.Target.TABLE;


public class CreateTableStatement extends SchemaAlteringStatement {
	private static final Pattern PATTERN_WORD_CHARS = Pattern.compile("\\w+");

	private List<AbstractType<?>> keyTypes;

	private List<AbstractType<?>> clusteringTypes;

	private final Map<ByteBuffer, AbstractType> multicellColumns = new HashMap<>();

	private final List<ColumnIdentifier> keyAliases = new ArrayList<>();

	private final List<ColumnIdentifier> columnAliases = new ArrayList<>();

	private boolean isDense;

	private boolean isCompound;

	private boolean hasCounters;

	private final Map<ColumnIdentifier, AbstractType> columns = new TreeMap<>(( o1, o2) -> o1.bytes.compareTo(o2.bytes));

	private final Set<ColumnIdentifier> staticColumns;

	private final TableParams params;

	private final boolean ifNotExists;

	private final UUID id;

	public CreateTableStatement(CFName name, TableParams params, boolean ifNotExists, Set<ColumnIdentifier> staticColumns, UUID id) {
		super(name);
		this.params = params;
		this.ifNotExists = ifNotExists;
		this.staticColumns = staticColumns;
		this.id = id;
	}

	public void checkAccess(ClientState state) throws InvalidRequestException, UnauthorizedException {
		state.hasKeyspaceAccess(keyspace(), Permission.CREATE);
	}

	public void validate(ClientState state) {
	}

	public Event.SchemaChange announceMigration(QueryState queryState, boolean isLocalOnly) throws RequestValidationException {
		try {
			MigrationManager.announceNewColumnFamily(getCFMetaData(), isLocalOnly);
			return new Event.SchemaChange(CREATED, TABLE, keyspace(), columnFamily());
		} catch (AlreadyExistsException e) {
			if (ifNotExists)
				return null;

			throw e;
		}
	}

	protected void grantPermissionsToCreator(QueryState state) {
		try {
			IResource resource = DataResource.table(keyspace(), columnFamily());
			DatabaseDescriptor.getAuthorizer().grant(AuthenticatedUser.SYSTEM_USER, resource.applicablePermissions(), resource, RoleResource.role(state.getClientState().getUser().getName()));
		} catch (RequestExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	public CFMetaData.Builder metadataBuilder() {
		CFMetaData.Builder builder = create(keyspace(), columnFamily(), isDense, isCompound, hasCounters);
		builder.withId(id);
		for (int i = 0; i < (keyAliases.size()); i++)
			builder.addPartitionKey(keyAliases.get(i), keyTypes.get(i));

		for (int i = 0; i < (columnAliases.size()); i++)
			builder.addClusteringColumn(columnAliases.get(i), clusteringTypes.get(i));

		boolean isStaticCompact = (!(isDense)) && (!(isCompound));
		for (Map.Entry<ColumnIdentifier, AbstractType> entry : columns.entrySet()) {
			ColumnIdentifier name = entry.getKey();
			if ((staticColumns.contains(name)) || isStaticCompact)
				builder.addStaticColumn(name, entry.getValue());
			else
				builder.addRegularColumn(name, entry.getValue());

		}
		boolean isCompactTable = (isDense) || (!(isCompound));
		if (isCompactTable) {
			CompactTables.DefaultNames names = CompactTables.defaultNameGenerator(builder.usedColumnNames());
			if (isStaticCompact) {
				builder.addClusteringColumn(names.defaultClusteringName(), UTF8Type.instance);
				builder.addRegularColumn(names.defaultCompactValueName(), (hasCounters ? CounterColumnType.instance : BytesType.instance));
			}else
				if ((isDense) && (!(builder.hasRegulars()))) {
					builder.addRegularColumn(names.defaultCompactValueName(), EmptyType.instance);
				}

		}
		return builder;
	}

	public CFMetaData getCFMetaData() {
		return metadataBuilder().build().params(params);
	}

	public TableParams params() {
		return params;
	}

	public static class RawStatement extends CFStatement {
		private final Map<ColumnIdentifier, CQL3Type.Raw> definitions = new HashMap<>();

		public final CFProperties properties = new CFProperties();

		private final List<List<ColumnIdentifier>> keyAliases = new ArrayList<>();

		private final List<ColumnIdentifier> columnAliases = new ArrayList<>();

		private final Set<ColumnIdentifier> staticColumns = new HashSet<>();

		private final Multiset<ColumnIdentifier> definedNames = HashMultiset.create(1);

		private final boolean ifNotExists;

		public RawStatement(CFName name, boolean ifNotExists) {
			super(name);
			this.ifNotExists = ifNotExists;
		}

		public ParsedStatement.Prepared prepare(ClientState clientState) throws RequestValidationException {
			KeyspaceMetadata ksm = Schema.instance.getKSMetaData(keyspace());
			if (ksm == null)
				throw new ConfigurationException(String.format("Keyspace %s doesn't exist", keyspace()));

			return prepare(ksm.types);
		}

		public ParsedStatement.Prepared prepare(Types udts) throws RequestValidationException {
			if (!(CreateTableStatement.PATTERN_WORD_CHARS.matcher(columnFamily()).matches()))
				throw new InvalidRequestException(String.format("\"%s\" is not a valid table name (must be alphanumeric character or underscore only: [a-zA-Z_0-9]+)", columnFamily()));

			if ((columnFamily().length()) > (SchemaConstants.NAME_LENGTH))
				throw new InvalidRequestException(String.format("Table names shouldn\'t be more than %s characters long (got \"%s\")", SchemaConstants.NAME_LENGTH, columnFamily()));

			for (Multiset.Entry<ColumnIdentifier> entry : definedNames.entrySet())
				if ((entry.getCount()) > 1)
					throw new InvalidRequestException(String.format("Multiple definition of identifier %s", entry.getElement()));


			properties.validate();
			TableParams params = properties.properties.asNewTableParams();
			CreateTableStatement stmt = new CreateTableStatement(cfName, params, ifNotExists, staticColumns, properties.properties.getId());
			for (Map.Entry<ColumnIdentifier, CQL3Type.Raw> entry : definitions.entrySet()) {
				ColumnIdentifier id = entry.getKey();
				CQL3Type pt = entry.getValue().prepare(keyspace(), udts);
				if (pt.getType().isMultiCell())
					stmt.multicellColumns.put(id.bytes, pt.getType());

				if (entry.getValue().isCounter())
					stmt.hasCounters = true;

				if ((pt.getType().isUDT()) && (pt.getType().isMultiCell())) {
					for (AbstractType<?> innerType : ((UserType) (pt.getType())).fieldTypes()) {
						if (innerType.isMultiCell()) {
							assert innerType.isCollection();
							throw new InvalidRequestException("Non-frozen UDTs with nested non-frozen collections are not supported");
						}
					}
				}
				stmt.columns.put(id, pt.getType());
			}
			if (keyAliases.isEmpty())
				throw new InvalidRequestException("No PRIMARY KEY specifed (exactly one required)");

			if ((keyAliases.size()) > 1)
				throw new InvalidRequestException("Multiple PRIMARY KEYs specifed (exactly one required)");

			if ((stmt.hasCounters) && ((params.defaultTimeToLive) > 0))
				throw new InvalidRequestException("Cannot set default_time_to_live on a table with counters");

			List<ColumnIdentifier> kAliases = keyAliases.get(0);
			stmt.keyTypes = new ArrayList<>(kAliases.size());
			for (ColumnIdentifier alias : kAliases) {
				stmt.keyAliases.add(alias);
				AbstractType<?> t = getTypeAndRemove(stmt.columns, alias);
				if ((t.asCQL3Type().getType()) instanceof CounterColumnType)
					throw new InvalidRequestException(String.format("counter type is not supported for PRIMARY KEY part %s", alias));

				if (t.asCQL3Type().getType().referencesDuration())
					throw new InvalidRequestException(String.format("duration type is not supported for PRIMARY KEY part %s", alias));

				if (staticColumns.contains(alias))
					throw new InvalidRequestException(String.format("Static column %s cannot be part of the PRIMARY KEY", alias));

				stmt.keyTypes.add(t);
			}
			stmt.clusteringTypes = new ArrayList<>(columnAliases.size());
			for (ColumnIdentifier t : columnAliases) {
				stmt.columnAliases.add(t);
				AbstractType<?> type = getTypeAndRemove(stmt.columns, t);
				if ((type.asCQL3Type().getType()) instanceof CounterColumnType)
					throw new InvalidRequestException(String.format("counter type is not supported for PRIMARY KEY part %s", t));

				if (type.asCQL3Type().getType().referencesDuration())
					throw new InvalidRequestException(String.format("duration type is not supported for PRIMARY KEY part %s", t));

				if (staticColumns.contains(t))
					throw new InvalidRequestException(String.format("Static column %s cannot be part of the PRIMARY KEY", t));

				stmt.clusteringTypes.add(type);
			}
			if (stmt.hasCounters) {
				for (AbstractType<?> type : stmt.columns.values())
					if (!(type.isCounter()))
						throw new InvalidRequestException("Cannot mix counter and non counter columns in the same table");


			}
			return new ParsedStatement.Prepared(stmt);
		}

		private AbstractType<?> getTypeAndRemove(Map<ColumnIdentifier, AbstractType> columns, ColumnIdentifier t) throws InvalidRequestException {
			AbstractType type = columns.get(t);
			if (type == null)
				throw new InvalidRequestException(String.format("Unknown definition %s referenced in PRIMARY KEY", t));

			if (type.isMultiCell()) {
				if (type.isCollection())
					throw new InvalidRequestException(String.format("Invalid non-frozen collection type for PRIMARY KEY component %s", t));
				else
					throw new InvalidRequestException(String.format("Invalid non-frozen user-defined type for PRIMARY KEY component %s", t));

			}
			columns.remove(t);
			return null;
		}

		public void addDefinition(ColumnIdentifier def, CQL3Type.Raw type, boolean isStatic) {
			definedNames.add(def);
			definitions.put(def, type);
			if (isStatic)
				staticColumns.add(def);

		}

		public void addKeyAliases(List<ColumnIdentifier> aliases) {
			keyAliases.add(aliases);
		}

		public void addColumnAlias(ColumnIdentifier alias) {
			columnAliases.add(alias);
		}
	}
}

