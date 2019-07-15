

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import org.apache.cassandra.gms.EndpointState;
import org.apache.cassandra.gms.GossipDigest;
import org.apache.cassandra.io.IVersionedSerializer;


public class GossipDigestAck {
	public static final IVersionedSerializer<GossipDigestAck> serializer = new GossipDigestAckSerializer();

	final List<GossipDigest> gDigestList;

	final Map<InetAddress, EndpointState> epStateMap;

	GossipDigestAck(List<GossipDigest> gDigestList, Map<InetAddress, EndpointState> epStateMap) {
		this.gDigestList = gDigestList;
		this.epStateMap = epStateMap;
	}

	List<GossipDigest> getGossipDigestList() {
		return gDigestList;
	}

	Map<InetAddress, EndpointState> getEndpointStateMap() {
		return epStateMap;
	}
}

