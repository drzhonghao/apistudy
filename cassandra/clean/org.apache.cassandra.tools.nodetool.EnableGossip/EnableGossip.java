import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "enablegossip", description = "Reenable gossip")
public class EnableGossip extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        probe.startGossiping();
    }
}
