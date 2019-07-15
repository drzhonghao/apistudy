

import java.net.InetAddress;
import org.apache.cassandra.gms.GossipDigestAck2;
import org.apache.cassandra.gms.Gossiper;
import org.apache.cassandra.net.IVerbHandler;
import org.apache.cassandra.net.MessageIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GossipDigestAck2VerbHandler implements IVerbHandler<GossipDigestAck2> {
	private static final Logger logger = LoggerFactory.getLogger(GossipDigestAck2VerbHandler.class);

	public void doVerb(MessageIn<GossipDigestAck2> message, int id) {
		if (GossipDigestAck2VerbHandler.logger.isTraceEnabled()) {
			InetAddress from = message.from;
			GossipDigestAck2VerbHandler.logger.trace("Received a GossipDigestAck2Message from {}", from);
		}
		if (!(Gossiper.instance.isEnabled())) {
			if (GossipDigestAck2VerbHandler.logger.isTraceEnabled())
				GossipDigestAck2VerbHandler.logger.trace("Ignoring GossipDigestAck2Message because gossip is disabled");

			return;
		}
	}
}

