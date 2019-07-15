import org.apache.cassandra.tools.nodetool.stats.*;


import java.util.HashMap;
import java.util.Map;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.nodetool.stats.StatsHolder;

public class TpStatsHolder implements StatsHolder
{
    public final NodeProbe probe;

    public TpStatsHolder(NodeProbe probe)
    {
        this.probe = probe;
    }

    @Override
    public Map<String, Object> convert2Map()
    {
        HashMap<String, Object> result = new HashMap<>();
        HashMap<String, Map<String, Object>> threadPools = new HashMap<>();
        HashMap<String, Object> droppedMessage = new HashMap<>();

        for (Map.Entry<String, String> tp : probe.getThreadPools().entries())
        {
            HashMap<String, Object> threadPool = new HashMap<>();
            threadPool.put("ActiveTasks", probe.getThreadPoolMetric(tp.getKey(), tp.getValue(), "ActiveTasks"));
            threadPool.put("PendingTasks", probe.getThreadPoolMetric(tp.getKey(), tp.getValue(), "PendingTasks"));
            threadPool.put("CompletedTasks", probe.getThreadPoolMetric(tp.getKey(), tp.getValue(), "CompletedTasks"));
            threadPool.put("CurrentlyBlockedTasks", probe.getThreadPoolMetric(tp.getKey(), tp.getValue(), "CurrentlyBlockedTasks"));
            threadPool.put("TotalBlockedTasks", probe.getThreadPoolMetric(tp.getKey(), tp.getValue(), "TotalBlockedTasks"));
            threadPools.put(tp.getValue(), threadPool);
        }
        result.put("ThreadPools", threadPools);

        for (Map.Entry<String, Integer> entry : probe.getDroppedMessages().entrySet())
            droppedMessage.put(entry.getKey(), entry.getValue());
        result.put("DroppedMessage", droppedMessage);

        return result;
    }
}
