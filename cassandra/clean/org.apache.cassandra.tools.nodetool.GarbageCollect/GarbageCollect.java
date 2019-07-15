import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;

import java.util.ArrayList;
import java.util.List;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "garbagecollect", description = "Remove deleted data from one or more tables")
public class GarbageCollect extends NodeToolCmd
{
    @Arguments(usage = "[<keyspace> <tables>...]", description = "The keyspace followed by one or many tables")
    private List<String> args = new ArrayList<>();

    @Option(title = "granularity",
        name = {"-g", "--granularity"},
        allowedValues = {"ROW", "CELL"},
        description = "Granularity of garbage removal. ROW (default) removes deleted partitions and rows, CELL also removes overwritten or deleted cells.")
    private String tombstoneOption = "ROW";

    @Option(title = "jobs",
            name = {"-j", "--jobs"},
            description = "Number of sstables to cleanup simultanously, set to 0 to use all available compaction threads")
    private int jobs = 2;

    @Override
    public void execute(NodeProbe probe)
    {
        List<String> keyspaces = parseOptionalKeyspace(args, probe);
        String[] tableNames = parseOptionalTables(args);

        for (String keyspace : keyspaces)
        {
            try
            {
                probe.garbageCollect(System.out, tombstoneOption, jobs, keyspace, tableNames);
            } catch (Exception e)
            {
                throw new RuntimeException("Error occurred during garbage collection", e);
            }
        }
    }
}
