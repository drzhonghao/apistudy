

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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.SortedSet;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.config.SchemaConstants;
import org.apache.cassandra.cql3.CQLStatement;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.cql3.QueryOptions;
import org.apache.cassandra.cql3.QueryProcessor;
import org.apache.cassandra.cql3.UpdateParameters;
import org.apache.cassandra.cql3.functions.UDHelper;
import org.apache.cassandra.cql3.statements.CFStatement;
import org.apache.cassandra.cql3.statements.CreateTableStatement;
import org.apache.cassandra.cql3.statements.CreateTypeStatement;
import org.apache.cassandra.cql3.statements.ModificationStatement;
import org.apache.cassandra.cql3.statements.ParsedStatement;
import org.apache.cassandra.cql3.statements.UpdateStatement;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.PartitionColumns;
import org.apache.cassandra.db.SystemKeyspace;
import org.apache.cassandra.db.partitions.Partition;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.Murmur3Partitioner;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.exceptions.SyntaxException;
import org.apache.cassandra.io.sstable.format.SSTableFormat;
import org.apache.cassandra.schema.Functions;
import org.apache.cassandra.schema.KeyspaceMetadata;
import org.apache.cassandra.schema.KeyspaceParams;
import org.apache.cassandra.schema.SchemaKeyspace;
import org.apache.cassandra.schema.Tables;
import org.apache.cassandra.schema.Types;
import org.apache.cassandra.schema.Views;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.Pair;


public class CQLSSTableWriter implements Closeable {
	public static final ByteBuffer UNSET_VALUE = ByteBufferUtil.UNSET_BYTE_BUFFER;

	static {
		DatabaseDescriptor.clientInitialization(false);
		if ((DatabaseDescriptor.getPartitioner()) == null)
			DatabaseDescriptor.setPartitionerUnsafe(Murmur3Partitioner.instance);

	}

	private final UpdateStatement insert = null;

	private final List<ColumnSpecification> boundNames = null;

	private final List<TypeCodec> typeCodecs = null;

	public static CQLSSTableWriter.Builder builder() {
		return new CQLSSTableWriter.Builder();
	}

	public CQLSSTableWriter addRow(Object... values) throws IOException, InvalidRequestException {
		return addRow(Arrays.asList(values));
	}

	public CQLSSTableWriter addRow(List<Object> values) throws IOException, InvalidRequestException {
		int size = Math.min(values.size(), boundNames.size());
		List<ByteBuffer> rawValues = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			Object value = values.get(i);
			rawValues.add(serialize(value, typeCodecs.get(i)));
		}
		return rawAddRow(rawValues);
	}

	public CQLSSTableWriter addRow(Map<String, Object> values) throws IOException, InvalidRequestException {
		int size = boundNames.size();
		List<ByteBuffer> rawValues = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			ColumnSpecification spec = boundNames.get(i);
			Object value = values.get(spec.name.toString());
			rawValues.add(serialize(value, typeCodecs.get(i)));
		}
		return rawAddRow(rawValues);
	}

	public CQLSSTableWriter rawAddRow(ByteBuffer... values) throws IOException, InvalidRequestException {
		return rawAddRow(Arrays.asList(values));
	}

	public CQLSSTableWriter rawAddRow(List<ByteBuffer> values) throws IOException, InvalidRequestException {
		if ((values.size()) != (boundNames.size()))
			throw new InvalidRequestException(String.format("Invalid number of arguments, expecting %d values but got %d", boundNames.size(), values.size()));

		QueryOptions options = QueryOptions.forInternalCalls(null, values);
		List<ByteBuffer> keys = insert.buildPartitionKeyNames(options);
		SortedSet<Clustering> clusterings = insert.createClustering(options);
		long now = (System.currentTimeMillis()) * 1000;
		UpdateParameters params = new UpdateParameters(insert.cfm, insert.updatedColumns(), options, insert.getTimestamp(now, options), insert.getTimeToLive(options), Collections.<DecoratedKey, Partition>emptyMap());
		return null;
	}

	public CQLSSTableWriter rawAddRow(Map<String, ByteBuffer> values) throws IOException, InvalidRequestException {
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
		if ((value == null) || (value == (CQLSSTableWriter.UNSET_VALUE)))
			return ((ByteBuffer) (value));

		return codec.serialize(value, ProtocolVersion.NEWEST_SUPPORTED);
	}

	public static class Builder {
		private File directory;

		protected SSTableFormat.Type formatType = null;

		private CreateTableStatement.RawStatement schemaStatement;

		private final List<CreateTypeStatement> typeStatements;

		private ModificationStatement.Parsed insertStatement;

		private IPartitioner partitioner;

		private boolean sorted = false;

		private long bufferSizeInMB = 128;

		protected Builder() {
			this.typeStatements = new ArrayList<>();
		}

		public CQLSSTableWriter.Builder inDirectory(String directory) {
			return inDirectory(new File(directory));
		}

		public CQLSSTableWriter.Builder inDirectory(File directory) {
			if (!(directory.exists()))
				throw new IllegalArgumentException((directory + " doesn't exists"));

			if (!(directory.canWrite()))
				throw new IllegalArgumentException((directory + " exists but is not writable"));

			this.directory = directory;
			return this;
		}

		public CQLSSTableWriter.Builder withType(String typeDefinition) throws SyntaxException {
			typeStatements.add(QueryProcessor.parseStatement(typeDefinition, CreateTypeStatement.class, "CREATE TYPE"));
			return this;
		}

		public CQLSSTableWriter.Builder forTable(String schema) {
			this.schemaStatement = QueryProcessor.parseStatement(schema, CreateTableStatement.RawStatement.class, "CREATE TABLE");
			return this;
		}

		public CQLSSTableWriter.Builder withPartitioner(IPartitioner partitioner) {
			this.partitioner = partitioner;
			return this;
		}

		public CQLSSTableWriter.Builder using(String insert) {
			this.insertStatement = QueryProcessor.parseStatement(insert, ModificationStatement.Parsed.class, "INSERT/UPDATE");
			return this;
		}

		public CQLSSTableWriter.Builder withBufferSizeInMB(int size) {
			this.bufferSizeInMB = size;
			return this;
		}

		public CQLSSTableWriter.Builder sorted() {
			this.sorted = true;
			return this;
		}

		@SuppressWarnings("resource")
		public CQLSSTableWriter build() {
			if ((directory) == null)
				throw new IllegalStateException("No ouptut directory specified, you should provide a directory with inDirectory()");

			if ((schemaStatement) == null)
				throw new IllegalStateException("Missing schema, you should provide the schema for the SSTable to create with forTable()");

			if ((insertStatement) == null)
				throw new IllegalStateException("No insert statement specified, you should provide an insert statement through using()");

			synchronized(CQLSSTableWriter.class) {
				if ((Schema.instance.getKSMetaData(SchemaConstants.SCHEMA_KEYSPACE_NAME)) == null)
					Schema.instance.load(SchemaKeyspace.metadata());

				if ((Schema.instance.getKSMetaData(SchemaConstants.SYSTEM_KEYSPACE_NAME)) == null)
					Schema.instance.load(SystemKeyspace.metadata());

				String keyspace = schemaStatement.keyspace();
				if ((Schema.instance.getKSMetaData(keyspace)) == null) {
					Schema.instance.load(KeyspaceMetadata.create(keyspace, KeyspaceParams.simple(1), Tables.none(), Views.none(), Types.none(), Functions.none()));
				}
				KeyspaceMetadata ksm = Schema.instance.getKSMetaData(keyspace);
				CFMetaData cfMetaData = ksm.tables.getNullable(schemaStatement.columnFamily());
				if (cfMetaData == null) {
					Types types = createTypes(keyspace);
					cfMetaData = createTable(types);
					Schema.instance.load(cfMetaData);
					Schema.instance.setKeyspaceMetadata(ksm.withSwapped(ksm.tables.with(cfMetaData)).withSwapped(types));
				}
				Pair<UpdateStatement, List<ColumnSpecification>> preparedInsert = prepareInsert();
			}
			return null;
		}

		private Types createTypes(String keyspace) {
			Types.RawBuilder builder = Types.rawBuilder(keyspace);
			for (CreateTypeStatement st : typeStatements)
				st.addToRawBuilder(builder);

			return builder.build();
		}

		private CFMetaData createTable(Types types) {
			CreateTableStatement statement = ((CreateTableStatement) (schemaStatement.prepare(types).statement));
			statement.validate(ClientState.forInternalCalls());
			CFMetaData cfMetaData = statement.getCFMetaData();
			if ((partitioner) != null)
				return cfMetaData.copy(partitioner);
			else
				return cfMetaData;

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
}

