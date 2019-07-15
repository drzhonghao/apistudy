

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.cassandra.gms.ApplicationState;
import org.apache.cassandra.gms.VersionedValue;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.utils.CassandraVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EndpointState {
	protected static final Logger logger = LoggerFactory.getLogger(EndpointState.class);

	public static final IVersionedSerializer<EndpointState> serializer = new EndpointStateSerializer();

	private final AtomicReference<Map<ApplicationState, VersionedValue>> applicationState = null;

	private volatile long updateTimestamp;

	private volatile boolean isAlive;

	public VersionedValue getApplicationState(ApplicationState key) {
		return applicationState.get().get(key);
	}

	public Set<Map.Entry<ApplicationState, VersionedValue>> states() {
		return applicationState.get().entrySet();
	}

	public void addApplicationState(ApplicationState key, VersionedValue value) {
		addApplicationStates(Collections.singletonMap(key, value));
	}

	public void addApplicationStates(Map<ApplicationState, VersionedValue> values) {
		addApplicationStates(values.entrySet());
	}

	public void addApplicationStates(Set<Map.Entry<ApplicationState, VersionedValue>> values) {
		while (true) {
			Map<ApplicationState, VersionedValue> orig = applicationState.get();
			Map<ApplicationState, VersionedValue> copy = new EnumMap<>(orig);
			for (Map.Entry<ApplicationState, VersionedValue> value : values)
				copy.put(value.getKey(), value.getValue());

			if (applicationState.compareAndSet(orig, copy))
				return;

		} 
	}

	public long getUpdateTimestamp() {
		return updateTimestamp;
	}

	void updateTimestamp() {
		updateTimestamp = System.nanoTime();
	}

	public boolean isAlive() {
		return isAlive;
	}

	void markAlive() {
		isAlive = true;
	}

	void markDead() {
		isAlive = false;
	}

	public boolean isRpcReady() {
		VersionedValue rpcState = getApplicationState(ApplicationState.RPC_READY);
		return (rpcState != null) && (Boolean.parseBoolean(rpcState.value));
	}

	public String getStatus() {
		VersionedValue status = getApplicationState(ApplicationState.STATUS);
		if (status == null)
			return "";

		String[] pieces = status.value.split(VersionedValue.DELIMITER_STR, (-1));
		assert (pieces.length) > 0;
		return pieces[0];
	}

	@javax.annotation.Nullable
	public UUID getSchemaVersion() {
		VersionedValue applicationState = getApplicationState(ApplicationState.SCHEMA);
		return applicationState != null ? UUID.fromString(applicationState.value) : null;
	}

	@javax.annotation.Nullable
	public CassandraVersion getReleaseVersion() {
		VersionedValue applicationState = getApplicationState(ApplicationState.RELEASE_VERSION);
		return applicationState != null ? new CassandraVersion(applicationState.value) : null;
	}

	public String toString() {
		return null;
	}
}

