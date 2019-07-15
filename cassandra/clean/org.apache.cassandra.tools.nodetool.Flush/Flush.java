import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Arguments;
import io.airlift.command.Command;

import java.util.ArrayList;
import java.util.List;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "flush", description = "Flush one or more tables")
public class Flush extends NodeToolCmd
{
    @Arguments(usage = "[<keyspace> <tables>...]", description = "The keyspace followed by one or many tables")
    private List<String> args = new ArrayList<>();

    @Override
    public void execute(NodeProbe probe)
    {
        List<String> keyspaces = parseOptionalKeyspace(args, probe);
        String[] tableNames = parseOptionalTables(args);

        for (String keyspace : keyspaces)
        {
            try
            {
                probe.forceKeyspaceFlush(keyspace, tableNames);
            } catch (Exception e)
            {
                throw new RuntimeException("Error occurred during flushing", e);
            }
        }
    }
}
