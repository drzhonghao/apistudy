

import java.net.InetAddress;
import org.apache.cassandra.gms.Gossiper;
import org.apache.cassandra.net.IVerbHandler;
import org.apache.cassandra.net.MessageIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GossipShutdownVerbHandler implements IVerbHandler {
	private static final Logger logger = LoggerFactory.getLogger(GossipShutdownVerbHandler.class);

	public void doVerb(MessageIn message, int id) {
		if (!(Gossiper.instance.isEnabled())) {
			GossipShutdownVerbHandler.logger.debug("Ignoring shutdown message from {} because gossip is disabled", message.from);
			return;
		}
	}
}

