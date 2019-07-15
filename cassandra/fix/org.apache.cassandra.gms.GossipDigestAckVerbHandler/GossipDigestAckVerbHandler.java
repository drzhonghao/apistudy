

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import org.apache.cassandra.gms.EndpointState;
import org.apache.cassandra.gms.GossipDigestAck;
import org.apache.cassandra.net.IVerbHandler;
import org.apache.cassandra.net.MessageIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GossipDigestAckVerbHandler implements IVerbHandler<GossipDigestAck> {
	private static final Logger logger = LoggerFactory.getLogger(GossipDigestAckVerbHandler.class);

	public void doVerb(MessageIn<GossipDigestAck> message, int id) {
		InetAddress from = message.from;
		if (GossipDigestAckVerbHandler.logger.isTraceEnabled())
			GossipDigestAckVerbHandler.logger.trace("Received a GossipDigestAckMessage from {}", from);

		GossipDigestAck gDigestAckMessage = message.payload;
		Map<InetAddress, EndpointState> deltaEpStateMap = new HashMap<InetAddress, EndpointState>();
		if (GossipDigestAckVerbHandler.logger.isTraceEnabled())
			GossipDigestAckVerbHandler.logger.trace("Sending a GossipDigestAck2Message to {}", from);

	}
}

