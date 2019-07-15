import org.apache.cassandra.metrics.*;


import com.codahale.metrics.Gauge;
import com.codahale.metrics.Timer;
import org.apache.cassandra.db.commitlog.AbstractCommitLogService;
import org.apache.cassandra.db.commitlog.AbstractCommitLogSegmentManager;

import static org.apache.cassandra.metrics.CassandraMetricsRegistry.Metrics;

/**
 * Metrics for commit log
 */
public class CommitLogMetrics
{
    public static final MetricNameFactory factory = new DefaultNameFactory("CommitLog");

    /** Number of completed tasks */
    public Gauge<Long> completedTasks;
    /** Number of pending tasks */
    public Gauge<Long> pendingTasks;
    /** Current size used by all the commit log segments */
    public Gauge<Long> totalCommitLogSize;
    /** Time spent waiting for a CLS to be allocated - under normal conditions this should be zero */
    public final Timer waitingOnSegmentAllocation;
    /** The time spent waiting on CL sync; for Periodic this is only occurs when the sync is lagging its sync interval */
    public final Timer waitingOnCommit;

    public CommitLogMetrics()
    {
        waitingOnSegmentAllocation = Metrics.timer(factory.createMetricName("WaitingOnSegmentAllocation"));
        waitingOnCommit = Metrics.timer(factory.createMetricName("WaitingOnCommit"));
    }

    public void attach(final AbstractCommitLogService service, final AbstractCommitLogSegmentManager segmentManager)
    {
        completedTasks = Metrics.register(factory.createMetricName("CompletedTasks"), new Gauge<Long>()
        {
            public Long getValue()
            {
                return service.getCompletedTasks();
            }
        });
        pendingTasks = Metrics.register(factory.createMetricName("PendingTasks"), new Gauge<Long>()
        {
            public Long getValue()
            {
                return service.getPendingTasks();
            }
        });
        totalCommitLogSize = Metrics.register(factory.createMetricName("TotalCommitLogSize"), new Gauge<Long>()
        {
            public Long getValue()
            {
                return segmentManager.onDiskSize();
            }
        });
    }
}
