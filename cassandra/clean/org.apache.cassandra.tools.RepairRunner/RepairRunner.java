import org.apache.cassandra.tools.*;


import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.concurrent.locks.Condition;

import org.apache.cassandra.service.StorageServiceMBean;
import org.apache.cassandra.utils.concurrent.SimpleCondition;
import org.apache.cassandra.utils.progress.ProgressEvent;
import org.apache.cassandra.utils.progress.ProgressEventType;
import org.apache.cassandra.utils.progress.jmx.JMXNotificationProgressListener;

public class RepairRunner extends JMXNotificationProgressListener
{
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

    private final PrintStream out;
    private final StorageServiceMBean ssProxy;
    private final String keyspace;
    private final Map<String, String> options;
    private final Condition condition = new SimpleCondition();

    private int cmd;
    private volatile boolean hasNotificationLost;
    private volatile Exception error;

    public RepairRunner(PrintStream out, StorageServiceMBean ssProxy, String keyspace, Map<String, String> options)
    {
        this.out = out;
        this.ssProxy = ssProxy;
        this.keyspace = keyspace;
        this.options = options;
    }

    public void run() throws Exception
    {
        cmd = ssProxy.repairAsync(keyspace, options);
        if (cmd <= 0)
        {
            // repairAsync can only return 0 for replication factor 1.
            String message = String.format("[%s] Replication factor is 1. No repair is needed for keyspace '%s'", format.format(System.currentTimeMillis()), keyspace);
            out.println(message);
        }
        else
        {
            condition.await();
            if (error != null)
            {
                throw error;
            }
            if (hasNotificationLost)
            {
                out.println(String.format("There were some lost notification(s). You should check server log for repair status of keyspace %s", keyspace));
            }
        }
    }

    @Override
    public boolean isInterestedIn(String tag)
    {
        return tag.equals("repair:" + cmd);
    }

    @Override
    public void handleNotificationLost(long timestamp, String message)
    {
        hasNotificationLost = true;
    }

    @Override
    public void handleConnectionClosed(long timestamp, String message)
    {
        handleConnectionFailed(timestamp, message);
    }

    @Override
    public void handleConnectionFailed(long timestamp, String message)
    {
        error = new IOException(String.format("[%s] JMX connection closed. You should check server log for repair status of keyspace %s"
                                               + "(Subsequent keyspaces are not going to be repaired).",
                                  format.format(timestamp), keyspace));
        condition.signalAll();
    }

    @Override
    public void progress(String tag, ProgressEvent event)
    {
        ProgressEventType type = event.getType();
        String message = String.format("[%s] %s", format.format(System.currentTimeMillis()), event.getMessage());
        if (type == ProgressEventType.PROGRESS)
        {
            message = message + " (progress: " + (int)event.getProgressPercentage() + "%)";
        }
        out.println(message);
        if (type == ProgressEventType.ERROR)
        {
            error = new RuntimeException("Repair job has failed with the error message: " + message);
        }
        if (type == ProgressEventType.COMPLETE)
        {
            condition.signalAll();
        }
    }
}
