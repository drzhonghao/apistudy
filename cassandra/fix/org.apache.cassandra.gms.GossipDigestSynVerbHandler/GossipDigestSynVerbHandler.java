

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.cassandra.gms.EndpointState;
import org.apache.cassandra.gms.GossipDigest;
import org.apache.cassandra.gms.GossipDigestSyn;
import org.apache.cassandra.net.IVerbHandler;
import org.apache.cassandra.net.MessageIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GossipDigestSynVerbHandler implements IVerbHandler<GossipDigestSyn> {
	private static final Logger logger = LoggerFactory.getLogger(GossipDigestSynVerbHandler.class);

	public void doVerb(MessageIn<GossipDigestSyn> message, int id) {
		InetAddress from = message.from;
		if (GossipDigestSynVerbHandler.logger.isTraceEnabled())
			GossipDigestSynVerbHandler.logger.trace("Received a GossipDigestSynMessage from {}", from);

		GossipDigestSyn gDigestMessage = message.payload;
		if (GossipDigestSynVerbHandler.logger.isTraceEnabled()) {
			StringBuilder sb = new StringBuilder();
			GossipDigestSynVerbHandler.logger.trace("Gossip syn digests are : {}", sb);
		}
		List<GossipDigest> deltaGossipDigestList = new ArrayList<GossipDigest>();
		Map<InetAddress, EndpointState> deltaEpStateMap = new HashMap<InetAddress, EndpointState>();
		GossipDigestSynVerbHandler.logger.trace("sending {} digests and {} deltas", deltaGossipDigestList.size(), deltaEpStateMap.size());
		if (GossipDigestSynVerbHandler.logger.isTraceEnabled())
			GossipDigestSynVerbHandler.logger.trace("Sending a GossipDigestAckMessage to {}", from);

	}

	private void doSort(List<GossipDigest> gDigestList) {
		Map<InetAddress, GossipDigest> epToDigestMap = new HashMap<InetAddress, GossipDigest>();
		for (GossipDigest gDigest : gDigestList) {
		}
		List<GossipDigest> diffDigests = new ArrayList<GossipDigest>(gDigestList.size());
		for (GossipDigest gDigest : gDigestList) {
		}
		gDigestList.clear();
		Collections.sort(diffDigests);
		int size = diffDigests.size();
		for (int i = size - 1; i >= 0; --i) {
		}
	}
}

