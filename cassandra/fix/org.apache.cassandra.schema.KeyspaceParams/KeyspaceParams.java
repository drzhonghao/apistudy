

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import java.util.Map;
import org.apache.cassandra.schema.ReplicationParams;


public final class KeyspaceParams {
	public static final boolean DEFAULT_DURABLE_WRITES = true;

	@VisibleForTesting
	public static boolean DEFAULT_LOCAL_DURABLE_WRITES = true;

	public enum Option {

		DURABLE_WRITES,
		REPLICATION;
		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	public final boolean durableWrites;

	public final ReplicationParams replication;

	public KeyspaceParams(boolean durableWrites, ReplicationParams replication) {
		this.durableWrites = durableWrites;
		this.replication = replication;
	}

	public static KeyspaceParams create(boolean durableWrites, Map<String, String> replication) {
		return new KeyspaceParams(durableWrites, ReplicationParams.fromMap(replication));
	}

	public static KeyspaceParams local() {
		return null;
	}

	public static KeyspaceParams simple(int replicationFactor) {
		return null;
	}

	public static KeyspaceParams simpleTransient(int replicationFactor) {
		return null;
	}

	public static KeyspaceParams nts(Object... args) {
		return null;
	}

	public void validate(String name) {
		replication.validate(name);
	}

	@Override
	public boolean equals(Object o) {
		if ((this) == o)
			return true;

		if (!(o instanceof KeyspaceParams))
			return false;

		KeyspaceParams p = ((KeyspaceParams) (o));
		return ((durableWrites) == (p.durableWrites)) && (replication.equals(p.replication));
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(durableWrites, replication);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add(KeyspaceParams.Option.DURABLE_WRITES.toString(), durableWrites).add(KeyspaceParams.Option.REPLICATION.toString(), replication).toString();
	}
}

