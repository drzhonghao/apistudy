import org.apache.cassandra.repair.*;


import java.net.InetAddress;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.dht.Range;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.exceptions.RepairException;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.repair.messages.SyncRequest;
import org.apache.cassandra.tracing.Tracing;
import org.apache.cassandra.utils.FBUtilities;

/**
 * RemoteSyncTask sends {@link SyncRequest} to remote(non-coordinator) node
 * to repair(stream) data with other replica.
 *
 * When RemoteSyncTask receives SyncComplete from remote node, task completes.
 */
public class RemoteSyncTask extends SyncTask
{
    private static final Logger logger = LoggerFactory.getLogger(RemoteSyncTask.class);

    public RemoteSyncTask(RepairJobDesc desc, TreeResponse r1, TreeResponse r2)
    {
        super(desc, r1, r2);
    }

    protected void startSync(List<Range<Token>> differences)
    {
        InetAddress local = FBUtilities.getBroadcastAddress();
        SyncRequest request = new SyncRequest(desc, local, r1.endpoint, r2.endpoint, differences);
        String message = String.format("Forwarding streaming repair of %d ranges to %s (to be streamed with %s)", request.ranges.size(), request.src, request.dst);
        logger.info("[repair #{}] {}", desc.sessionId, message);
        Tracing.traceRepair(message);
        MessagingService.instance().sendOneWay(request.createMessage(), request.src);
    }

    public void syncComplete(boolean success)
    {
        if (success)
        {
            set(stat);
        }
        else
        {
            setException(new RepairException(desc, String.format("Sync failed between %s and %s", r1.endpoint, r2.endpoint)));
        }
    }
}
