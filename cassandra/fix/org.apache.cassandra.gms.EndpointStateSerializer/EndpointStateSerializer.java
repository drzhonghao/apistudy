

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.cassandra.gms.ApplicationState;
import org.apache.cassandra.gms.VersionedValue;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;


class EndpointStateSerializer implements IVersionedSerializer<EndpointState> {
	public void serialize(EndpointState epState, DataOutputPlus out, int version) throws IOException {
		Set<Map.Entry<ApplicationState, VersionedValue>> states = epState.states();
		out.writeInt(states.size());
		for (Map.Entry<ApplicationState, VersionedValue> state : states) {
			VersionedValue value = state.getValue();
			out.writeInt(state.getKey().ordinal());
			VersionedValue.serializer.serialize(value, out, version);
		}
	}

	public EndpointState deserialize(DataInputPlus in, int version) throws IOException {
		int appStateSize = in.readInt();
		Map<ApplicationState, VersionedValue> states = new EnumMap<>(ApplicationState.class);
		for (int i = 0; i < appStateSize; ++i) {
			int key = in.readInt();
			VersionedValue value = VersionedValue.serializer.deserialize(in, version);
		}
		return null;
	}

	public long serializedSize(EndpointState epState, int version) {
		Set<Map.Entry<ApplicationState, VersionedValue>> states = epState.states();
		for (Map.Entry<ApplicationState, VersionedValue> state : states) {
			VersionedValue value = state.getValue();
		}
		return 0l;
	}
}

