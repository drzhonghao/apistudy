import org.apache.cassandra.service.*;


import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cassandra.concurrent.Stage;
import org.apache.cassandra.concurrent.StageManager;
import org.apache.cassandra.db.ReadResponse;
import org.apache.cassandra.net.IAsyncCallback;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.utils.WrappedRunnable;

public class AsyncRepairCallback implements IAsyncCallback<ReadResponse>
{
    private final DataResolver repairResolver;
    private final int blockfor;
    protected final AtomicInteger received = new AtomicInteger(0);

    public AsyncRepairCallback(DataResolver repairResolver, int blockfor)
    {
        this.repairResolver = repairResolver;
        this.blockfor = blockfor;
    }

    public void response(MessageIn<ReadResponse> message)
    {
        repairResolver.preprocess(message);
        if (received.incrementAndGet() == blockfor)
        {
            StageManager.getStage(Stage.READ_REPAIR).execute(new WrappedRunnable()
            {
                protected void runMayThrow()
                {
                    repairResolver.compareResponses();
                }
            });
        }
    }

    public boolean isLatencyForSnitch()
    {
        return true;
    }
}
