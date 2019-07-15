import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import java.io.IOException;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "resetlocalschema", description = "Reset node's local schema and resync")
public class ResetLocalSchema extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        try
        {
            probe.resetLocalSchema();
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
