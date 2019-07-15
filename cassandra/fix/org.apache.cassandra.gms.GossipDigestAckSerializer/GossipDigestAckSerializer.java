

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.cassandra.gms.EndpointState;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.net.CompactEndpointSerializationHelper;


class GossipDigestAckSerializer implements IVersionedSerializer<GossipDigestAck> {
	public void serialize(GossipDigestAck gDigestAckMessage, DataOutputPlus out, int version) throws IOException {
		out.writeInt(gDigestAckMessage.epStateMap.size());
		for (Map.Entry<InetAddress, EndpointState> entry : gDigestAckMessage.epStateMap.entrySet()) {
			InetAddress ep = entry.getKey();
			CompactEndpointSerializationHelper.serialize(ep, out);
			EndpointState.serializer.serialize(entry.getValue(), out, version);
		}
	}

	public GossipDigestAck deserialize(DataInputPlus in, int version) throws IOException {
		int size = in.readInt();
		Map<InetAddress, EndpointState> epStateMap = new HashMap<InetAddress, EndpointState>(size);
		for (int i = 0; i < size; ++i) {
			InetAddress ep = CompactEndpointSerializationHelper.deserialize(in);
			EndpointState epState = EndpointState.serializer.deserialize(in, version);
			epStateMap.put(ep, epState);
		}
		return null;
	}

	public long serializedSize(GossipDigestAck ack, int version) {
		for (Map.Entry<InetAddress, EndpointState> entry : ack.epStateMap.entrySet()) {
		}
		return 0l;
	}
}

