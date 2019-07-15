import org.apache.cassandra.tools.nodetool.stats.TableStatsHolder;
import org.apache.cassandra.tools.nodetool.stats.StatsPrinter;
import org.apache.cassandra.tools.nodetool.stats.TableStatsPrinter;
import org.apache.cassandra.tools.nodetool.*;


import java.util.*;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;
import org.apache.cassandra.tools.nodetool.stats.*;

@Command(name = "tablestats", description = "Print statistics on tables")
public class TableStats extends NodeToolCmd
{
    @Arguments(usage = "[<keyspace.table>...]", description = "List of tables (or keyspace) names")
    private List<String> tableNames = new ArrayList<>();

    @Option(name = "-i", description = "Ignore the list of tables and display the remaining tables")
    private boolean ignore = false;

    @Option(title = "human_readable",
            name = {"-H", "--human-readable"},
            description = "Display bytes in human readable form, i.e. KiB, MiB, GiB, TiB")
    private boolean humanReadable = false;

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

        StatsHolder holder = new TableStatsHolder(probe, humanReadable, ignore, tableNames);
        // print out the keyspace and table statistics
        StatsPrinter printer = TableStatsPrinter.from(outputFormat);
        printer.print(holder, System.out);
    }

}
