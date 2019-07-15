import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;
import io.airlift.command.Option;
import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;
import org.apache.cassandra.tools.nodetool.stats.CompactionHistoryHolder;
import org.apache.cassandra.tools.nodetool.stats.CompactionHistoryPrinter;
import org.apache.cassandra.tools.nodetool.stats.StatsHolder;
import org.apache.cassandra.tools.nodetool.stats.StatsPrinter;

@Command(name = "compactionhistory", description = "Print history of compaction")
public class CompactionHistory extends NodeToolCmd
{
    @Option(title = "format",
            name = {"-F", "--format"},
            description = "Output format (json, yaml)")
    private String outputFormat = "";

    @Override
    public void execute(NodeProbe probe)
    {
        if (!outputFormat.isEmpty() && !"json".equals(outputFormat) && !"yaml".equals(outputFormat))
        {
            throw new IllegalArgumentException("arguments for -F are json,yaml only.");
        }
        StatsHolder data = new CompactionHistoryHolder(probe);
        StatsPrinter printer = CompactionHistoryPrinter.from(outputFormat);
        printer.print(data, System.out);
    }
}
