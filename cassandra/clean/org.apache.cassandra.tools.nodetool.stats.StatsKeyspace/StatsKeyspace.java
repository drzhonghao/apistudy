import org.apache.cassandra.tools.nodetool.stats.*;


import java.util.ArrayList;
import java.util.List;

import org.apache.cassandra.db.ColumnFamilyStoreMBean;
import org.apache.cassandra.metrics.CassandraMetricsRegistry;
import org.apache.cassandra.tools.NodeProbe;

public class StatsKeyspace
{
    public List<StatsTable> tables = new ArrayList<>();
    private final NodeProbe probe;

    public String name;
    public long readCount;
    public long writeCount;
    public int pendingFlushes;
    private double totalReadTime;
    private double totalWriteTime;

    public StatsKeyspace(NodeProbe probe, String keyspaceName)
    {
        this.probe = probe;
        this.name = keyspaceName;
    }

    public void add(ColumnFamilyStoreMBean table)
    {
        String tableName = table.getTableName();
        long tableWriteCount = ((CassandraMetricsRegistry.JmxTimerMBean) probe.getColumnFamilyMetric(name, tableName, "WriteLatency")).getCount();
        long tableReadCount = ((CassandraMetricsRegistry.JmxTimerMBean) probe.getColumnFamilyMetric(name, tableName, "ReadLatency")).getCount();

        if (tableReadCount > 0)
        {
            readCount += tableReadCount;
            totalReadTime += (long) probe.getColumnFamilyMetric(name, tableName, "ReadTotalLatency");
        }
        if (tableWriteCount > 0)
        {
            writeCount += tableWriteCount;
            totalWriteTime += (long) probe.getColumnFamilyMetric(name, tableName, "WriteTotalLatency");
        }
        pendingFlushes += (long) probe.getColumnFamilyMetric(name, tableName, "PendingFlushes");
    }

    public double readLatency()
    {
        return readCount > 0
               ? totalReadTime / readCount / 1000
               : Double.NaN;
    }

    public double writeLatency()
    {
        return writeCount > 0
               ? totalWriteTime / writeCount / 1000
               : Double.NaN;
    }
}
