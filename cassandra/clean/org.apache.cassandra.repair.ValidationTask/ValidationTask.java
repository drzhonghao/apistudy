import org.apache.cassandra.repair.*;


import java.net.InetAddress;

import com.google.common.util.concurrent.AbstractFuture;

import org.apache.cassandra.exceptions.RepairException;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.repair.messages.ValidationRequest;
import org.apache.cassandra.utils.MerkleTrees;

/**
 * ValidationTask sends {@link ValidationRequest} to a replica.
 * When a replica sends back message, task completes.
 */
public class ValidationTask extends AbstractFuture<TreeResponse> implements Runnable
{
    private final RepairJobDesc desc;
    private final InetAddress endpoint;
    private final int gcBefore;

    public ValidationTask(RepairJobDesc desc, InetAddress endpoint, int gcBefore)
    {
        this.desc = desc;
        this.endpoint = endpoint;
        this.gcBefore = gcBefore;
    }

    /**
     * Send ValidationRequest to replica
     */
    public void run()
    {
        ValidationRequest request = new ValidationRequest(desc, gcBefore);
        MessagingService.instance().sendOneWay(request.createMessage(), endpoint);
    }

    /**
     * Receive MerkleTrees from replica node.
     *
     * @param trees MerkleTrees that is sent from replica. Null if validation failed on replica node.
     */
    public void treesReceived(MerkleTrees trees)
    {
        if (trees == null)
        {
            setException(new RepairException(desc, "Validation failed in " + endpoint));
        }
        else
        {
            set(new TreeResponse(endpoint, trees));
        }
    }
}
