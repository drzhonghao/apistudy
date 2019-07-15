

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.core.UserType;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.antlr.runtime.RecognitionException;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.cql3.CQLFragmentParser;
import org.apache.cassandra.cql3.CQLStatement;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.cql3.CqlParser;
import org.apache.cassandra.cql3.QueryOptions;
import org.apache.cassandra.cql3.UpdateParameters;
import org.apache.cassandra.cql3.functions.UDHelper;
import org.apache.cassandra.cql3.statements.CFStatement;
import org.apache.cassandra.cql3.statements.CreateTableStatement;
import org.apache.cassandra.cql3.statements.CreateTypeStatement;
import org.apache.cassandra.cql3.statements.ModificationStatement;
import org.apache.cassandra.cql3.statements.ParsedStatement;
import org.apache.cassandra.cql3.statements.UpdateStatement;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.Directories;
import org.apache.cassandra.db.Directories.DataDirectory;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.PartitionColumns;
import org.apache.cassandra.db.partitions.Partition;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.Murmur3Partitioner;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.exceptions.RequestValidationException;
import org.apache.cassandra.exceptions.SyntaxException;
import org.apache.cassandra.io.sstable.format.SSTableFormat;
import org.apache.cassandra.schema.KeyspaceMetadata;
import org.apache.cassandra.schema.KeyspaceParams;
import org.apache.cassandra.schema.TableParams;
import org.apache.cassandra.schema.Tables;
import org.apache.cassandra.schema.Types;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.Pair;


public class StressCQLSSTableWriter implements Closeable {
	public static final ByteBuffer UNSET_VALUE = ByteBufferUtil.UNSET_BYTE_BUFFER;

	static {
		DatabaseDescriptor.clientInitialization(false);
		if ((DatabaseDescriptor.getPartitioner()) == null)
			DatabaseDescriptor.setPartitionerUnsafe(Murmur3Partitioner.instance);

	}

	private final UpdateStatement insert = null;

	private final List<ColumnSpecification> boundNames = null;

	private final List<TypeCodec> typeCodecs = null;

	private final ColumnFamilyStore cfs = null;

	public static StressCQLSSTableWriter.Builder builder() {
		return new StressCQLSSTableWriter.Builder();
	}

	public StressCQLSSTableWriter addRow(Object... values) throws IOException, InvalidRequestException {
		return addRow(Arrays.asList(values));
	}

	public StressCQLSSTableWriter addRow(List<Object> values) throws IOException, InvalidRequestException {
		int size = Math.min(values.size(), boundNames.size());
		List<ByteBuffer> rawValues = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			Object value = values.get(i);
			rawValues.add(serialize(value, typeCodecs.get(i)));
		}
		return rawAddRow(rawValues);
	}

	public StressCQLSSTableWriter addRow(Map<String, Object> values) throws IOException, InvalidRequestException {
		int size = boundNames.size();
		List<ByteBuffer> rawValues = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			ColumnSpecification spec = boundNames.get(i);
			Object value = values.get(spec.name.toString());
			rawValues.add(serialize(value, typeCodecs.get(i)));
		}
		return rawAddRow(rawValues);
	}

	public StressCQLSSTableWriter rawAddRow(ByteBuffer... values) throws IOException, InvalidRequestException {
		return rawAddRow(Arrays.asList(values));
	}

	public StressCQLSSTableWriter rawAddRow(List<ByteBuffer> values) throws IOException, InvalidRequestException {
		if ((values.size()) != (boundNames.size()))
			throw new InvalidRequestException(String.format("Invalid number of arguments, expecting %d values but got %d", boundNames.size(), values.size()));

		QueryOptions options = QueryOptions.forInternalCalls(null, values);
		List<ByteBuffer> keys = insert.buildPartitionKeyNames(options);
		SortedSet<Clustering> clusterings = insert.createClustering(options);
		long now = (System.currentTimeMillis()) * 1000;
		UpdateParameters params = new UpdateParameters(insert.cfm, insert.updatedColumns(), options, insert.getTimestamp(now, options), insert.getTimeToLive(options), Collections.<DecoratedKey, Partition>emptyMap());
		return null;
	}

	public StressCQLSSTableWriter rawAddRow(Map<String, ByteBuffer> values) throws IOException, InvalidRequestException {
		int size = Math.min(values.size(), boundNames.size());
		List<ByteBuffer> rawValues = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			ColumnSpecification spec = boundNames.get(i);
			rawValues.add(values.get(spec.name.toString()));
		}
		return rawAddRow(rawValues);
	}

	public UserType getUDType(String dataType) {
		KeyspaceMetadata ksm = Schema.instance.getKSMetaData(insert.keyspace());
		org.apache.cassandra.db.marshal.UserType userType = ksm.types.getNullable(ByteBufferUtil.bytes(dataType));
		return ((UserType) (UDHelper.driverType(userType)));
	}

	public void close() throws IOException {
	}

	private ByteBuffer serialize(Object value, TypeCodec codec) {
		if ((value == null) || (value == (StressCQLSSTableWriter.UNSET_VALUE)))
			return ((ByteBuffer) (value));

		return codec.serialize(value, ProtocolVersion.NEWEST_SUPPORTED);
	}

	public File getInnermostDirectory() {
		return cfs.getDirectories().getDirectoryForNewSSTables();
	}

	public static class Builder {
		private final List<File> directoryList;

		private ColumnFamilyStore cfs;

		protected SSTableFormat.Type formatType = null;

		private Boolean makeRangeAware = false;

		private CreateTableStatement.RawStatement schemaStatement;

		private final List<CreateTypeStatement> typeStatements;

		private UpdateStatement.ParsedInsert insertStatement;

		private IPartitioner partitioner;

		private boolean sorted = false;

		private long bufferSizeInMB = 128;

		protected Builder() {
			this.typeStatements = new ArrayList<>();
			this.directoryList = new ArrayList<>();
		}

		public StressCQLSSTableWriter.Builder inDirectory(String directory) {
			return inDirectory(new File(directory));
		}

		public StressCQLSSTableWriter.Builder inDirectory(File directory) {
			if (!(directory.exists()))
				throw new IllegalArgumentException((directory + " doesn't exists"));

			if (!(directory.canWrite()))
				throw new IllegalArgumentException((directory + " exists but is not writable"));

			directoryList.add(directory);
			return this;
		}

		public StressCQLSSTableWriter.Builder withCfs(ColumnFamilyStore cfs) {
			this.cfs = cfs;
			return this;
		}

		public StressCQLSSTableWriter.Builder withType(String typeDefinition) throws SyntaxException {
			typeStatements.add(StressCQLSSTableWriter.parseStatement(typeDefinition, CreateTypeStatement.class, "CREATE TYPE"));
			return this;
		}

		public StressCQLSSTableWriter.Builder forTable(String schema) {
			this.schemaStatement = StressCQLSSTableWriter.parseStatement(schema, CreateTableStatement.RawStatement.class, "CREATE TABLE");
			return this;
		}

		public StressCQLSSTableWriter.Builder withPartitioner(IPartitioner partitioner) {
			this.partitioner = partitioner;
			return this;
		}

		public StressCQLSSTableWriter.Builder rangeAware(boolean makeRangeAware) {
			this.makeRangeAware = makeRangeAware;
			return this;
		}

		public StressCQLSSTableWriter.Builder using(String insert) {
			this.insertStatement = StressCQLSSTableWriter.parseStatement(insert, UpdateStatement.ParsedInsert.class, "INSERT");
			return this;
		}

		public StressCQLSSTableWriter.Builder withBufferSizeInMB(int size) {
			this.bufferSizeInMB = size;
			return this;
		}

		public StressCQLSSTableWriter.Builder sorted() {
			this.sorted = true;
			return this;
		}

		@SuppressWarnings("resource")
		public StressCQLSSTableWriter build() {
			if ((directoryList.isEmpty()) && ((cfs) == null))
				throw new IllegalStateException("No output directories specified, you should provide a directory with inDirectory()");

			if (((schemaStatement) == null) && ((cfs) == null))
				throw new IllegalStateException("Missing schema, you should provide the schema for the SSTable to create with forTable()");

			if ((insertStatement) == null)
				throw new IllegalStateException("No insert statement specified, you should provide an insert statement through using()");

			synchronized(StressCQLSSTableWriter.class) {
				if ((cfs) == null)
					cfs = StressCQLSSTableWriter.Builder.createOfflineTable(schemaStatement, typeStatements, directoryList);

				if ((partitioner) == null)
					partitioner = cfs.getPartitioner();

				Pair<UpdateStatement, List<ColumnSpecification>> preparedInsert = prepareInsert();
			}
			return null;
		}

		private static void createTypes(String keyspace, List<CreateTypeStatement> typeStatements) {
			KeyspaceMetadata ksm = Schema.instance.getKSMetaData(keyspace);
			Types.RawBuilder builder = Types.rawBuilder(keyspace);
			for (CreateTypeStatement st : typeStatements)
				st.addToRawBuilder(builder);

			ksm = ksm.withSwapped(builder.build());
			Schema.instance.setKeyspaceMetadata(ksm);
		}

		public static ColumnFamilyStore createOfflineTable(String schema, List<File> directoryList) {
			return StressCQLSSTableWriter.Builder.createOfflineTable(StressCQLSSTableWriter.parseStatement(schema, CreateTableStatement.RawStatement.class, "CREATE TABLE"), Collections.EMPTY_LIST, directoryList);
		}

		public static ColumnFamilyStore createOfflineTable(CreateTableStatement.RawStatement schemaStatement, List<CreateTypeStatement> typeStatements, List<File> directoryList) {
			String keyspace = schemaStatement.keyspace();
			if ((Schema.instance.getKSMetaData(keyspace)) == null)
				Schema.instance.load(KeyspaceMetadata.create(keyspace, KeyspaceParams.simple(1)));

			StressCQLSSTableWriter.Builder.createTypes(keyspace, typeStatements);
			KeyspaceMetadata ksm = Schema.instance.getKSMetaData(keyspace);
			CFMetaData cfMetaData = ksm.tables.getNullable(schemaStatement.columnFamily());
			if (cfMetaData != null)
				return Schema.instance.getColumnFamilyStoreInstance(cfMetaData.cfId);

			CreateTableStatement statement = ((CreateTableStatement) (schemaStatement.prepare(ksm.types).statement));
			statement.validate(ClientState.forInternalCalls());
			cfMetaData = statement.metadataBuilder().withId(CFMetaData.generateLegacyCfId(keyspace, statement.columnFamily())).build().params(statement.params());
			Keyspace.setInitialized();
			Directories directories = new Directories(cfMetaData, directoryList.stream().map(Directories.DataDirectory::new).collect(Collectors.toList()));
			Keyspace ks = Keyspace.openWithoutSSTables(keyspace);
			ColumnFamilyStore cfs = ColumnFamilyStore.createColumnFamilyStore(ks, cfMetaData.cfName, cfMetaData, directories, false, false, true);
			ks.initCfCustom(cfs);
			Schema.instance.load(cfs.metadata);
			Schema.instance.setKeyspaceMetadata(ksm.withSwapped(ksm.tables.with(cfs.metadata)));
			return cfs;
		}

		private Pair<UpdateStatement, List<ColumnSpecification>> prepareInsert() {
			ParsedStatement.Prepared cqlStatement = insertStatement.prepare(ClientState.forInternalCalls());
			UpdateStatement insert = ((UpdateStatement) (cqlStatement.statement));
			insert.validate(ClientState.forInternalCalls());
			if (insert.hasConditions())
				throw new IllegalArgumentException("Conditional statements are not supported");

			if (insert.isCounter())
				throw new IllegalArgumentException("Counter update statements are not supported");

			if (cqlStatement.boundNames.isEmpty())
				throw new IllegalArgumentException("Provided insert statement has no bind variables");

			return Pair.create(insert, cqlStatement.boundNames);
		}
	}

	public static <T extends ParsedStatement> T parseStatement(String query, Class<T> klass, String type) {
		try {
			ParsedStatement stmt = CQLFragmentParser.parseAnyUnhandled(CqlParser::query, query);
			if (!(stmt.getClass().equals(klass)))
				throw new IllegalArgumentException(((("Invalid query, must be a " + type) + " statement but was: ") + (stmt.getClass())));

			return klass.cast(stmt);
		} catch (RecognitionException | RequestValidationException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}
}

