import org.apache.cassandra.tools.nodetool.*;


import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.airlift.command.Command;
import io.airlift.command.Option;

import org.apache.cassandra.db.compaction.CompactionManagerMBean;
import org.apache.cassandra.db.compaction.OperationType;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;
import org.apache.cassandra.tools.nodetool.formatter.TableBuilder;

import static java.lang.String.format;

@Command(name = "compactionstats", description = "Print statistics on compactions")
public class CompactionStats extends NodeToolCmd
{
    @Option(title = "human_readable",
            name = {"-H", "--human-readable"},
            description = "Display bytes in human readable form, i.e. KiB, MiB, GiB, TiB")
    private boolean humanReadable = false;

    @Override
    public void execute(NodeProbe probe)
    {
        CompactionManagerMBean cm = probe.getCompactionManagerProxy();
        Map<String, Map<String, Integer>> pendingTaskNumberByTable =
            (Map<String, Map<String, Integer>>) probe.getCompactionMetric("PendingTasksByTableName");
        int numTotalPendingTask = 0;
        for (Entry<String, Map<String, Integer>> ksEntry : pendingTaskNumberByTable.entrySet())
        {
            for (Entry<String, Integer> tableEntry : ksEntry.getValue().entrySet())
                numTotalPendingTask += tableEntry.getValue();
        }
        System.out.println("pending tasks: " + numTotalPendingTask);
        for (Entry<String, Map<String, Integer>> ksEntry : pendingTaskNumberByTable.entrySet())
        {
            String ksName = ksEntry.getKey();
            for (Entry<String, Integer> tableEntry : ksEntry.getValue().entrySet())
            {
                String tableName = tableEntry.getKey();
                int pendingTaskCount = tableEntry.getValue();

                System.out.println("- " + ksName + '.' + tableName + ": " + pendingTaskCount);
            }
        }
        System.out.println();
        reportCompactionTable(cm.getCompactions(), probe.getCompactionThroughput(), humanReadable);
    }

    public static void reportCompactionTable(List<Map<String,String>> compactions, int compactionThroughput, boolean humanReadable)
    {
        if (!compactions.isEmpty())
        {
            long remainingBytes = 0;
            TableBuilder table = new TableBuilder();

            table.add("id", "compaction type", "keyspace", "table", "completed", "total", "unit", "progress");
            for (Map<String, String> c : compactions)
            {
                long total = Long.parseLong(c.get("total"));
                long completed = Long.parseLong(c.get("completed"));
                String taskType = c.get("taskType");
                String keyspace = c.get("keyspace");
                String columnFamily = c.get("columnfamily");
                String completedStr = humanReadable ? FileUtils.stringifyFileSize(completed) : Long.toString(completed);
                String totalStr = humanReadable ? FileUtils.stringifyFileSize(total) : Long.toString(total);
                String unit = c.get("unit");
                String percentComplete = total == 0 ? "n/a" : new DecimalFormat("0.00").format((double) completed / total * 100) + "%";
                String id = c.get("compactionId");
                table.add(id, taskType, keyspace, columnFamily, completedStr, totalStr, unit, percentComplete);
                if (taskType.equals(OperationType.COMPACTION.toString()))
                    remainingBytes += total - completed;
            }
            table.printTo(System.out);

            String remainingTime = "n/a";
            if (compactionThroughput != 0)
            {
                long remainingTimeInSecs = remainingBytes / (1024L * 1024L * compactionThroughput);
                remainingTime = format("%dh%02dm%02ds", remainingTimeInSecs / 3600, (remainingTimeInSecs % 3600) / 60, (remainingTimeInSecs % 60));
            }
            System.out.printf("%25s%10s%n", "Active compaction remaining time : ", remainingTime);
        }
    }

}
