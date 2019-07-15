

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.IMutation;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.db.partitions.AbstractBTreePartition;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.db.rows.SerializationHelper;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.schema.KeyspaceMetadata;
import org.apache.cassandra.schema.KeyspaceParams;
import org.apache.cassandra.schema.TableParams;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.commons.lang3.StringUtils;

import static org.apache.cassandra.db.rows.SerializationHelper.Flag.FROM_REMOTE;
import static org.apache.cassandra.net.MessagingService.Verb.MUTATION;


public class Mutation implements IMutation {
	public static final Mutation.MutationSerializer serializer = new Mutation.MutationSerializer();

	public static final String FORWARD_TO = "FWD_TO";

	public static final String FORWARD_FROM = "FWD_FRM";

	private final String keyspaceName;

	private final DecoratedKey key;

	private final Map<UUID, PartitionUpdate> modifications;

	public final long createdAt = System.currentTimeMillis();

	public final AtomicLong viewLockAcquireStart = new AtomicLong(0);

	private boolean cdcEnabled = false;

	public Mutation(String keyspaceName, DecoratedKey key) {
		this(keyspaceName, key, new HashMap<>());
	}

	public Mutation(PartitionUpdate update) {
		this(update.metadata().ksName, update.partitionKey(), Collections.singletonMap(update.metadata().cfId, update));
	}

	protected Mutation(String keyspaceName, DecoratedKey key, Map<UUID, PartitionUpdate> modifications) {
		this.keyspaceName = keyspaceName;
		this.key = key;
		this.modifications = modifications;
		for (PartitionUpdate pu : modifications.values())
			cdcEnabled |= pu.metadata().params.cdc;

	}

	public Mutation copy() {
		return new Mutation(keyspaceName, key, new HashMap<>(modifications));
	}

	public Mutation without(Set<UUID> cfIds) {
		if (cfIds.isEmpty())
			return this;

		Mutation copy = copy();
		copy.modifications.keySet().removeAll(cfIds);
		copy.cdcEnabled = false;
		for (PartitionUpdate pu : modifications.values())
			copy.cdcEnabled |= pu.metadata().params.cdc;

		return copy;
	}

	public Mutation without(UUID cfId) {
		return without(Collections.singleton(cfId));
	}

	public String getKeyspaceName() {
		return keyspaceName;
	}

	public Collection<UUID> getColumnFamilyIds() {
		return modifications.keySet();
	}

	public DecoratedKey key() {
		return key;
	}

	public Collection<PartitionUpdate> getPartitionUpdates() {
		return modifications.values();
	}

	public PartitionUpdate getPartitionUpdate(UUID cfId) {
		return modifications.get(cfId);
	}

	public Mutation add(PartitionUpdate update) {
		assert update != null;
		assert (update.partitionKey().getPartitioner()) == (key.getPartitioner());
		cdcEnabled |= update.metadata().params.cdc;
		PartitionUpdate prev = modifications.put(update.metadata().cfId, update);
		if (prev != null)
			throw new IllegalArgumentException(((("Table " + (update.metadata().cfName)) + " already has modifications in this mutation: ") + prev));

		return this;
	}

	public PartitionUpdate get(CFMetaData cfm) {
		return modifications.get(cfm.cfId);
	}

	public boolean isEmpty() {
		return modifications.isEmpty();
	}

	public static Mutation merge(List<Mutation> mutations) {
		assert !(mutations.isEmpty());
		if ((mutations.size()) == 1)
			return mutations.get(0);

		Set<UUID> updatedTables = new HashSet<>();
		String ks = null;
		DecoratedKey key = null;
		for (Mutation mutation : mutations) {
			updatedTables.addAll(mutation.modifications.keySet());
			if ((ks != null) && (!(ks.equals(mutation.keyspaceName))))
				throw new IllegalArgumentException();

			if ((key != null) && (!(key.equals(mutation.key))))
				throw new IllegalArgumentException();

			ks = mutation.keyspaceName;
			key = mutation.key;
		}
		List<PartitionUpdate> updates = new ArrayList<>(mutations.size());
		Map<UUID, PartitionUpdate> modifications = new HashMap<>(updatedTables.size());
		for (UUID table : updatedTables) {
			for (Mutation mutation : mutations) {
				PartitionUpdate upd = mutation.modifications.get(table);
				if (upd != null)
					updates.add(upd);

			}
			if (updates.isEmpty())
				continue;

			modifications.put(table, ((updates.size()) == 1 ? updates.get(0) : PartitionUpdate.merge(updates)));
			updates.clear();
		}
		return new Mutation(ks, key, modifications);
	}

	public CompletableFuture<?> applyFuture() {
		Keyspace ks = Keyspace.open(keyspaceName);
		return null;
	}

	public void apply(boolean durableWrites, boolean isDroppable) {
	}

	public void apply(boolean durableWrites) {
		apply(durableWrites, true);
	}

	public void apply() {
		apply(Keyspace.open(keyspaceName).getMetadata().params.durableWrites);
	}

	public void applyUnsafe() {
		apply(false);
	}

	public MessageOut<Mutation> createMessage() {
		return createMessage(MUTATION);
	}

	public MessageOut<Mutation> createMessage(MessagingService.Verb verb) {
		return new MessageOut<>(verb, this, Mutation.serializer);
	}

	public long getTimeout() {
		return DatabaseDescriptor.getWriteRpcTimeout();
	}

	public int smallestGCGS() {
		int gcgs = Integer.MAX_VALUE;
		for (PartitionUpdate update : getPartitionUpdates())
			gcgs = Math.min(gcgs, update.metadata().params.gcGraceSeconds);

		return gcgs;
	}

	public boolean trackedByCDC() {
		return cdcEnabled;
	}

	public String toString() {
		return toString(false);
	}

	public String toString(boolean shallow) {
		StringBuilder buff = new StringBuilder("Mutation(");
		buff.append("keyspace='").append(keyspaceName).append('\'');
		buff.append(", key='").append(ByteBufferUtil.bytesToHex(key.getKey())).append('\'');
		buff.append(", modifications=[");
		if (shallow) {
			List<String> cfnames = new ArrayList<>(modifications.size());
			for (UUID cfid : modifications.keySet()) {
				CFMetaData cfm = Schema.instance.getCFMetaData(cfid);
				cfnames.add((cfm == null ? "-dropped-" : cfm.cfName));
			}
			buff.append(StringUtils.join(cfnames, ", "));
		}else {
			buff.append("\n  ").append(StringUtils.join(modifications.values(), "\n  ")).append('\n');
		}
		return buff.append("])").toString();
	}

	public static Mutation.SimpleBuilder simpleBuilder(String keyspaceName, DecoratedKey partitionKey) {
		return null;
	}

	public interface SimpleBuilder {
		public Mutation.SimpleBuilder timestamp(long timestamp);

		public Mutation.SimpleBuilder ttl(int ttl);

		public PartitionUpdate.SimpleBuilder update(CFMetaData metadata);

		public PartitionUpdate.SimpleBuilder update(String tableName);

		public Mutation build();
	}

	public static class MutationSerializer implements IVersionedSerializer<Mutation> {
		public void serialize(Mutation mutation, DataOutputPlus out, int version) throws IOException {
			if (version < (MessagingService.VERSION_20))
				out.writeUTF(mutation.getKeyspaceName());

			int size = mutation.modifications.size();
			if (version < (MessagingService.VERSION_30)) {
				ByteBufferUtil.writeWithShortLength(mutation.key().getKey(), out);
				out.writeInt(size);
			}else {
				out.writeUnsignedVInt(size);
			}
			assert size > 0;
			for (Map.Entry<UUID, PartitionUpdate> entry : mutation.modifications.entrySet())
				PartitionUpdate.serializer.serialize(entry.getValue(), out, version);

		}

		public Mutation deserialize(DataInputPlus in, int version, SerializationHelper.Flag flag) throws IOException {
			if (version < (MessagingService.VERSION_20))
				in.readUTF();

			ByteBuffer key = null;
			int size;
			if (version < (MessagingService.VERSION_30)) {
				key = ByteBufferUtil.readWithShortLength(in);
				size = in.readInt();
			}else {
				size = ((int) (in.readUnsignedVInt()));
			}
			assert size > 0;
			PartitionUpdate update = PartitionUpdate.serializer.deserialize(in, version, flag, key);
			if (size == 1)
				return new Mutation(update);

			Map<UUID, PartitionUpdate> modifications = new HashMap<>(size);
			DecoratedKey dk = update.partitionKey();
			modifications.put(update.metadata().cfId, update);
			for (int i = 1; i < size; ++i) {
				update = PartitionUpdate.serializer.deserialize(in, version, flag, dk);
				modifications.put(update.metadata().cfId, update);
			}
			return new Mutation(update.metadata().ksName, dk, modifications);
		}

		public Mutation deserialize(DataInputPlus in, int version) throws IOException {
			return deserialize(in, version, FROM_REMOTE);
		}

		public long serializedSize(Mutation mutation, int version) {
			int size = 0;
			if (version < (MessagingService.VERSION_20))
				size += TypeSizes.sizeof(mutation.getKeyspaceName());

			if (version < (MessagingService.VERSION_30)) {
				int keySize = mutation.key().getKey().remaining();
				size += (TypeSizes.sizeof(((short) (keySize)))) + keySize;
				size += TypeSizes.sizeof(mutation.modifications.size());
			}else {
				size += TypeSizes.sizeofUnsignedVInt(mutation.modifications.size());
			}
			for (Map.Entry<UUID, PartitionUpdate> entry : mutation.modifications.entrySet())
				size += PartitionUpdate.serializer.serializedSize(entry.getValue(), version);

			return size;
		}
	}
}

