import org.apache.cassandra.tools.nodetool.*;


import java.util.ArrayList;
import java.util.List;

import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;
import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool;

@Command(name = "relocatesstables", description = "Relocates sstables to the correct disk")
public class RelocateSSTables extends NodeTool.NodeToolCmd
{
    @Arguments(usage = "<keyspace> <table>", description = "The keyspace and table name")
    private List<String> args = new ArrayList<>();

    @Option(title = "jobs",
            name = {"-j", "--jobs"},
            description = "Number of sstables to relocate simultanously, set to 0 to use all available compaction threads")
    private int jobs = 2;

    @Override
    public void execute(NodeProbe probe)
    {
        List<String> keyspaces = parseOptionalKeyspace(args, probe);
        String[] cfnames = parseOptionalTables(args);
        try
        {
            for (String keyspace : keyspaces)
                probe.relocateSSTables(jobs, keyspace, cfnames);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Got error while relocating", e);
        }
    }
}
