import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "gossipinfo", description = "Shows the gossip information for the cluster")
public class GossipInfo extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        System.out.println(probe.getGossipInfo());
    }
}
