import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "decommission", description = "Decommission the *node I am connecting to*")
public class Decommission extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        try
        {
            probe.decommission();
        } catch (InterruptedException e)
        {
            throw new RuntimeException("Error decommissioning node", e);
        } catch (UnsupportedOperationException e)
        {
            throw new IllegalStateException("Unsupported operation: " + e.getMessage(), e);
        }
    }
}
