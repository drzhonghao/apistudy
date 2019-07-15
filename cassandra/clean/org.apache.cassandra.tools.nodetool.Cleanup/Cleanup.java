import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Arguments;
import io.airlift.command.Command;

import java.util.ArrayList;
import java.util.List;

import io.airlift.command.Option;
import org.apache.cassandra.config.SchemaConstants;
import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "cleanup", description = "Triggers the immediate cleanup of keys no longer belonging to a node. By default, clean all keyspaces")
public class Cleanup extends NodeToolCmd
{
    @Arguments(usage = "[<keyspace> <tables>...]", description = "The keyspace followed by one or many tables")
    private List<String> args = new ArrayList<>();

    @Option(title = "jobs",
            name = {"-j", "--jobs"},
            description = "Number of sstables to cleanup simultanously, set to 0 to use all available compaction threads")
    private int jobs = 2;

    @Override
    public void execute(NodeProbe probe)
    {
        List<String> keyspaces = parseOptionalKeyspace(args, probe, KeyspaceSet.NON_LOCAL_STRATEGY);
        String[] tableNames = parseOptionalTables(args);

        for (String keyspace : keyspaces)
        {
            if (SchemaConstants.isLocalSystemKeyspace(keyspace))
                continue;

            try
            {
                probe.forceKeyspaceCleanup(System.out, jobs, keyspace, tableNames);
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error occurred during cleanup", e);
            }
        }
    }
}
