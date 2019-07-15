import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Arguments;
import io.airlift.command.Command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "disableautocompaction", description = "Disable autocompaction for the given keyspace and table")
public class DisableAutoCompaction extends NodeToolCmd
{
    @Arguments(usage = "[<keyspace> <tables>...]", description = "The keyspace followed by one or many tables")
    private List<String> args = new ArrayList<>();

    @Override
    public void execute(NodeProbe probe)
    {
        List<String> keyspaces = parseOptionalKeyspace(args, probe);
        String[] tablenames = parseOptionalTables(args);

        for (String keyspace : keyspaces)
        {
            try
            {
                probe.disableAutoCompaction(keyspace, tablenames);
            } catch (IOException e)
            {
                throw new RuntimeException("Error occurred during disabling auto-compaction", e);
            }
        }
    }
}
