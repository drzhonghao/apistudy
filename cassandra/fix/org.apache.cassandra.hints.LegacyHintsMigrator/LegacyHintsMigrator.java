

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.SchemaConstants;
import org.apache.cassandra.cql3.QueryProcessor;
import org.apache.cassandra.cql3.UntypedResultSet;
import org.apache.cassandra.cql3.UntypedResultSet.Row;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.SystemKeyspace;
import org.apache.cassandra.db.compaction.CompactionManager;
import org.apache.cassandra.db.lifecycle.Tracker;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.UUIDType;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.hints.Hint;
import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.io.sstable.SSTable;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.util.DataInputBuffer;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.utils.FBUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings("deprecation")
public final class LegacyHintsMigrator {
	private static final Logger logger = LoggerFactory.getLogger(LegacyHintsMigrator.class);

	private final File hintsDirectory;

	private final long maxHintsFileSize;

	private final ColumnFamilyStore legacyHintsTable;

	private final int pageSize;

	public LegacyHintsMigrator(File hintsDirectory, long maxHintsFileSize) {
		this.hintsDirectory = hintsDirectory;
		this.maxHintsFileSize = maxHintsFileSize;
		legacyHintsTable = Keyspace.open(SchemaConstants.SYSTEM_KEYSPACE_NAME).getColumnFamilyStore(SystemKeyspace.LEGACY_HINTS);
		pageSize = LegacyHintsMigrator.calculatePageSize(legacyHintsTable);
	}

	private static int calculatePageSize(ColumnFamilyStore legacyHintsTable) {
		int size = 128;
		int meanCellCount = legacyHintsTable.getMeanColumns();
		double meanPartitionSize = legacyHintsTable.getMeanPartitionSize();
		if ((meanCellCount != 0) && (meanPartitionSize != 0)) {
			int avgHintSize = ((int) (meanPartitionSize)) / meanCellCount;
			size = Math.max(2, Math.min(size, ((512 << 10) / avgHintSize)));
		}
		return size;
	}

	public void migrate() {
		if (legacyHintsTable.isEmpty())
			return;

		LegacyHintsMigrator.logger.info("Migrating legacy hints to new storage");
		LegacyHintsMigrator.logger.info("Forcing a major compaction of {}.{} table", SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.LEGACY_HINTS);
		compactLegacyHints();
		LegacyHintsMigrator.logger.info("Writing legacy hints to the new storage");
		migrateLegacyHints();
		LegacyHintsMigrator.logger.info("Truncating {}.{} table", SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.LEGACY_HINTS);
		legacyHintsTable.truncateBlocking();
	}

	private void compactLegacyHints() {
		Collection<Descriptor> descriptors = new ArrayList<>();
		legacyHintsTable.getTracker().getUncompacting().forEach(( sstable) -> descriptors.add(sstable.descriptor));
		if (!(descriptors.isEmpty()))
			forceCompaction(descriptors);

	}

	private void forceCompaction(Collection<Descriptor> descriptors) {
		try {
			CompactionManager.instance.submitUserDefined(legacyHintsTable, descriptors, FBUtilities.nowInSeconds()).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	private void migrateLegacyHints() {
		ByteBuffer buffer = ByteBuffer.allocateDirect((256 * 1024));
		String query = String.format("SELECT DISTINCT target_id FROM %s.%s", SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.LEGACY_HINTS);
		QueryProcessor.executeInternal(query).forEach(( row) -> migrateLegacyHints(row.getUUID("target_id"), buffer));
		FileUtils.clean(buffer);
	}

	private void migrateLegacyHints(UUID hostId, ByteBuffer buffer) {
		String query = String.format(("SELECT target_id, hint_id, message_version, mutation, ttl(mutation) AS ttl, writeTime(mutation) AS write_time " + ("FROM %s.%s " + "WHERE target_id = ?")), SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.LEGACY_HINTS);
		UntypedResultSet rows = QueryProcessor.executeInternalWithPaging(query, pageSize, hostId);
		migrateLegacyHints(hostId, rows, buffer);
		LegacyHintsMigrator.deleteLegacyHintsPartition(hostId);
	}

	private void migrateLegacyHints(UUID hostId, UntypedResultSet rows, ByteBuffer buffer) {
		migrateLegacyHints(hostId, rows.iterator(), buffer);
	}

	private void migrateLegacyHints(UUID hostId, Iterator<UntypedResultSet.Row> iterator, ByteBuffer buffer) {
		do {
			migrateLegacyHintsInternal(hostId, iterator, buffer);
		} while (iterator.hasNext() );
	}

	private void migrateLegacyHintsInternal(UUID hostId, Iterator<UntypedResultSet.Row> iterator, ByteBuffer buffer) {
	}

	private static Hint convertLegacyHint(UntypedResultSet.Row row) {
		Mutation mutation = LegacyHintsMigrator.deserializeLegacyMutation(row);
		if (mutation == null)
			return null;

		long creationTime = row.getLong("write_time");
		int expirationTime = (FBUtilities.nowInSeconds()) + (row.getInt("ttl"));
		int originalGCGS = expirationTime - ((int) (TimeUnit.MILLISECONDS.toSeconds(creationTime)));
		int gcgs = Math.min(originalGCGS, mutation.smallestGCGS());
		return Hint.create(mutation, creationTime, gcgs);
	}

	private static Mutation deserializeLegacyMutation(UntypedResultSet.Row row) {
		try (DataInputBuffer dib = new DataInputBuffer(row.getBlob("mutation"), true)) {
			Mutation mutation = Mutation.serializer.deserialize(dib, row.getInt("message_version"));
			mutation.getPartitionUpdates().forEach(PartitionUpdate::validate);
			return mutation;
		} catch (IOException e) {
			LegacyHintsMigrator.logger.error("Failed to migrate a hint for {} from legacy {}.{} table", row.getUUID("target_id"), SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.LEGACY_HINTS, e);
			return null;
		} catch (MarshalException e) {
			LegacyHintsMigrator.logger.warn("Failed to validate a hint for {} from legacy {}.{} table - skipping", row.getUUID("target_id"), SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.LEGACY_HINTS, e);
			return null;
		}
	}

	private static void deleteLegacyHintsPartition(UUID hostId) {
		Mutation mutation = new Mutation(PartitionUpdate.fullPartitionDelete(SystemKeyspace.LegacyHints, UUIDType.instance.decompose(hostId), System.currentTimeMillis(), FBUtilities.nowInSeconds()));
		mutation.applyUnsafe();
	}
}

