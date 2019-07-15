import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "disablegossip", description = "Disable gossip (effectively marking the node down)")
public class DisableGossip extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        probe.stopGossiping();
    }
}
