import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;

import java.util.ArrayList;
import java.util.List;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "upgradesstables", description = "Rewrite sstables (for the requested tables) that are not on the current version (thus upgrading them to said current version)")
public class UpgradeSSTable extends NodeToolCmd
{
    @Arguments(usage = "[<keyspace> <tables>...]", description = "The keyspace followed by one or many tables")
    private List<String> args = new ArrayList<>();

    @Option(title = "include_all", name = {"-a", "--include-all-sstables"}, description = "Use -a to include all sstables, even those already on the current version")
    private boolean includeAll = false;

    @Option(title = "jobs",
            name = {"-j", "--jobs"},
            description = "Number of sstables to upgrade simultanously, set to 0 to use all available compaction threads")
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
                probe.upgradeSSTables(System.out, keyspace, !includeAll, jobs, tableNames);
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error occurred during enabling auto-compaction", e);
            }
        }
    }
}
