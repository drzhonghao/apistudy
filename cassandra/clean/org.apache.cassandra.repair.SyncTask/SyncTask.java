import org.apache.cassandra.repair.SyncStat;
import org.apache.cassandra.repair.TreeResponse;
import org.apache.cassandra.repair.*;


import java.util.List;

import com.google.common.util.concurrent.AbstractFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.dht.Range;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.tracing.Tracing;
import org.apache.cassandra.utils.MerkleTrees;

/**
 * SyncTask will calculate the difference of MerkleTree between two nodes
 * and perform necessary operation to repair replica.
 */
public abstract class SyncTask extends AbstractFuture<SyncStat> implements Runnable
{
    private static Logger logger = LoggerFactory.getLogger(SyncTask.class);

    protected final RepairJobDesc desc;
    protected final TreeResponse r1;
    protected final TreeResponse r2;

    protected volatile SyncStat stat;

    public SyncTask(RepairJobDesc desc, TreeResponse r1, TreeResponse r2)
    {
        this.desc = desc;
        this.r1 = r1;
        this.r2 = r2;
    }

    /**
     * Compares trees, and triggers repairs for any ranges that mismatch.
     */
    public void run()
    {
        // compare trees, and collect differences
        List<Range<Token>> differences = MerkleTrees.difference(r1.trees, r2.trees);

        stat = new SyncStat(new NodePair(r1.endpoint, r2.endpoint), differences.size());

        // choose a repair method based on the significance of the difference
        String format = String.format("[repair #%s] Endpoints %s and %s %%s for %s", desc.sessionId, r1.endpoint, r2.endpoint, desc.columnFamily);
        if (differences.isEmpty())
        {
            logger.info(String.format(format, "are consistent"));
            Tracing.traceRepair("Endpoint {} is consistent with {} for {}", r1.endpoint, r2.endpoint, desc.columnFamily);
            set(stat);
            return;
        }

        // non-0 difference: perform streaming repair
        logger.info(String.format(format, "have " + differences.size() + " range(s) out of sync"));
        Tracing.traceRepair("Endpoint {} has {} range(s) out of sync with {} for {}", r1.endpoint, differences.size(), r2.endpoint, desc.columnFamily);
        startSync(differences);
    }

    public SyncStat getCurrentStat()
    {
        return stat;
    }

    protected abstract void startSync(List<Range<Token>> differences);
}
