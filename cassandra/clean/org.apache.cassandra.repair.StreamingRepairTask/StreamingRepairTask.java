import org.apache.cassandra.repair.*;


import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.db.SystemKeyspace;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.repair.messages.SyncComplete;
import org.apache.cassandra.repair.messages.SyncRequest;
import org.apache.cassandra.service.ActiveRepairService;
import org.apache.cassandra.streaming.StreamEvent;
import org.apache.cassandra.streaming.StreamEventHandler;
import org.apache.cassandra.streaming.StreamPlan;
import org.apache.cassandra.streaming.StreamState;

/**
 * StreamingRepairTask performs data streaming between two remote replica which neither is not repair coordinator.
 * Task will send {@link SyncComplete} message back to coordinator upon streaming completion.
 */
public class StreamingRepairTask implements Runnable, StreamEventHandler
{
    private static final Logger logger = LoggerFactory.getLogger(StreamingRepairTask.class);

    private final RepairJobDesc desc;
    private final SyncRequest request;
    private final long repairedAt;

    public StreamingRepairTask(RepairJobDesc desc, SyncRequest request, long repairedAt)
    {
        this.desc = desc;
        this.request = request;
        this.repairedAt = repairedAt;
    }

    public void run()
    {
        InetAddress dest = request.dst;
        InetAddress preferred = SystemKeyspace.getPreferredIP(dest);
        logger.info("[streaming task #{}] Performing streaming repair of {} ranges with {}", desc.sessionId, request.ranges.size(), request.dst);
        boolean isIncremental = false;
        if (desc.parentSessionId != null)
        {
            ActiveRepairService.ParentRepairSession prs = ActiveRepairService.instance.getParentRepairSession(desc.parentSessionId);
            isIncremental = prs.isIncremental;
        }
        new StreamPlan("Repair", repairedAt, 1, false, isIncremental, false).listeners(this)
                                            .flushBeforeTransfer(true)
                                            // request ranges from the remote node
                                            .requestRanges(dest, preferred, desc.keyspace, request.ranges, desc.columnFamily)
                                            // send ranges to the remote node
                                            .transferRanges(dest, preferred, desc.keyspace, request.ranges, desc.columnFamily)
                                            .execute();
    }

    public void handleStreamEvent(StreamEvent event)
    {
        // Nothing to do here, all we care about is the final success or failure and that's handled by
        // onSuccess and onFailure
    }

    /**
     * If we succeeded on both stream in and out, reply back to coordinator
     */
    public void onSuccess(StreamState state)
    {
        logger.info("[repair #{}] streaming task succeed, returning response to {}", desc.sessionId, request.initiator);
        MessagingService.instance().sendOneWay(new SyncComplete(desc, request.src, request.dst, true).createMessage(), request.initiator);
    }

    /**
     * If we failed on either stream in or out, reply fail to coordinator
     */
    public void onFailure(Throwable t)
    {
        MessagingService.instance().sendOneWay(new SyncComplete(desc, request.src, request.dst, false).createMessage(), request.initiator);
    }
}
