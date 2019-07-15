import org.apache.cassandra.tools.nodetool.stats.StatsPrinter;
import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import io.airlift.command.Option;
import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;
import org.apache.cassandra.tools.nodetool.stats.TpStatsHolder;
import org.apache.cassandra.tools.nodetool.stats.TpStatsPrinter;
import org.apache.cassandra.tools.nodetool.stats.*;


@Command(name = "tpstats", description = "Print usage statistics of thread pools")
public class TpStats extends NodeToolCmd
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

        StatsHolder data = new TpStatsHolder(probe);
        StatsPrinter printer = TpStatsPrinter.from(outputFormat);
        printer.print(data, System.out);
    }
}
