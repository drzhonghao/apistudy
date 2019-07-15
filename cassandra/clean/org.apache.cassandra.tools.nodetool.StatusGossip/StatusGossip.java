import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "statusgossip", description = "Status of gossip")
public class StatusGossip extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        System.out.println(
                probe.isGossipRunning()
                ? "running"
                : "not running");
    }
}
