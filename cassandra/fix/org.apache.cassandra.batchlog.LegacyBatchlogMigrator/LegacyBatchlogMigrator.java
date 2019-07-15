

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.cassandra.batchlog.Batch;
import org.apache.cassandra.batchlog.BatchlogManager;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.SchemaConstants;
import org.apache.cassandra.cql3.UntypedResultSet;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.ConsistencyLevel;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.IMutation;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.SystemKeyspace;
import org.apache.cassandra.db.WriteType;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.UUIDType;
import org.apache.cassandra.db.partitions.AbstractBTreePartition;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.exceptions.WriteFailureException;
import org.apache.cassandra.exceptions.WriteTimeoutException;
import org.apache.cassandra.io.util.BufferedDataOutputStreamPlus;
import org.apache.cassandra.io.util.DataInputBuffer;
import org.apache.cassandra.io.util.DataOutputBuffer;
import org.apache.cassandra.io.util.RebufferingInputStream;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.service.AbstractWriteResponseHandler;
import org.apache.cassandra.service.WriteResponseHandler;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.UUIDGen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.cql3.UntypedResultSet.Row.fromInternalRow;
import static org.apache.cassandra.net.MessagingService.Verb.MUTATION;


public final class LegacyBatchlogMigrator {
	private static final Logger logger = LoggerFactory.getLogger(LegacyBatchlogMigrator.class);

	private LegacyBatchlogMigrator() {
	}

	@SuppressWarnings("deprecation")
	public static void migrate() {
		ColumnFamilyStore store = Keyspace.open(SchemaConstants.SYSTEM_KEYSPACE_NAME).getColumnFamilyStore(SystemKeyspace.LEGACY_BATCHLOG);
		if (store.isEmpty())
			return;

		LegacyBatchlogMigrator.logger.info("Migrating legacy batchlog to new storage");
		int convertedBatches = 0;
		String query = String.format("SELECT id, data, written_at, version FROM %s.%s", SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.LEGACY_BATCHLOG);
		if (convertedBatches > 0)
			Keyspace.openAndGetStore(SystemKeyspace.LegacyBatchlog).truncateBlocking();

	}

	@SuppressWarnings("deprecation")
	public static boolean isLegacyBatchlogMutation(Mutation mutation) {
		return (mutation.getKeyspaceName().equals(SchemaConstants.SYSTEM_KEYSPACE_NAME)) && ((mutation.getPartitionUpdate(SystemKeyspace.LegacyBatchlog.cfId)) != null);
	}

	@SuppressWarnings("deprecation")
	public static void handleLegacyMutation(Mutation mutation) {
		PartitionUpdate update = mutation.getPartitionUpdate(SystemKeyspace.LegacyBatchlog.cfId);
		LegacyBatchlogMigrator.logger.trace("Applying legacy batchlog mutation {}", update);
		update.forEach(( row) -> LegacyBatchlogMigrator.apply(fromInternalRow(update.metadata(), update.partitionKey(), row), (-1)));
	}

	private static boolean apply(UntypedResultSet.Row row, long counter) {
		UUID id = row.getUUID("id");
		long timestamp = ((id.version()) == 1) ? UUIDGen.unixTimestamp(id) : row.getLong("written_at");
		int version = (row.has("version")) ? row.getInt("version") : MessagingService.VERSION_12;
		if ((id.version()) != 1)
			id = UUIDGen.getTimeUUID(timestamp, counter);

		LegacyBatchlogMigrator.logger.trace("Converting mutation at {}", timestamp);
		try (DataInputBuffer in = new DataInputBuffer(row.getBytes("data"), false)) {
			int numMutations = in.readInt();
			List<Mutation> mutations = new ArrayList<>(numMutations);
			for (int i = 0; i < numMutations; i++)
				mutations.add(Mutation.serializer.deserialize(in, version));

			BatchlogManager.store(Batch.createLocal(id, TimeUnit.MILLISECONDS.toMicros(timestamp), mutations));
			return true;
		} catch (Throwable t) {
			LegacyBatchlogMigrator.logger.error("Failed to convert mutation {} at timestamp {}", id, timestamp, t);
			return false;
		}
	}

	public static void syncWriteToBatchlog(WriteResponseHandler<?> handler, Batch batch, Collection<InetAddress> endpoints) throws WriteFailureException, WriteTimeoutException {
		for (InetAddress target : endpoints) {
			LegacyBatchlogMigrator.logger.trace("Sending legacy batchlog store request {} to {} for {} mutations", batch.id, target, batch.size());
			int targetVersion = MessagingService.instance().getVersion(target);
			MessagingService.instance().sendRR(LegacyBatchlogMigrator.getStoreMutation(batch, targetVersion).createMessage(MUTATION), target, handler, false);
		}
	}

	public static void asyncRemoveFromBatchlog(Collection<InetAddress> endpoints, UUID uuid, long queryStartNanoTime) {
		AbstractWriteResponseHandler<IMutation> handler = new WriteResponseHandler<>(endpoints, Collections.<InetAddress>emptyList(), ConsistencyLevel.ANY, Keyspace.open(SchemaConstants.SYSTEM_KEYSPACE_NAME), null, WriteType.SIMPLE, queryStartNanoTime);
		Mutation mutation = LegacyBatchlogMigrator.getRemoveMutation(uuid);
		for (InetAddress target : endpoints) {
			LegacyBatchlogMigrator.logger.trace("Sending legacy batchlog remove request {} to {}", uuid, target);
			MessagingService.instance().sendRR(mutation.createMessage(MUTATION), target, handler, false);
		}
	}

	static void store(Batch batch, int version) {
		LegacyBatchlogMigrator.getStoreMutation(batch, version).apply();
	}

	@SuppressWarnings("deprecation")
	static Mutation getStoreMutation(Batch batch, int version) {
		PartitionUpdate.SimpleBuilder builder = PartitionUpdate.simpleBuilder(SystemKeyspace.LegacyBatchlog, batch.id);
		return builder.buildAsMutation();
	}

	@SuppressWarnings("deprecation")
	private static Mutation getRemoveMutation(UUID uuid) {
		return new Mutation(PartitionUpdate.fullPartitionDelete(SystemKeyspace.LegacyBatchlog, UUIDType.instance.decompose(uuid), FBUtilities.timestampMicros(), FBUtilities.nowInSeconds()));
	}

	private static ByteBuffer getSerializedMutations(int version, Collection<Mutation> mutations) {
		try (DataOutputBuffer buf = new DataOutputBuffer()) {
			buf.writeInt(mutations.size());
			for (Mutation mutation : mutations)
				Mutation.serializer.serialize(mutation, buf, version);

			return buf.buffer();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

