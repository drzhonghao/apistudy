import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "statushandoff", description = "Status of storing future hints on the current node")
public class StatusHandoff extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        System.out.println(String.format("Hinted handoff is %s",
                probe.isHandoffEnabled()
                ? "running"
                : "not running"));

        for (String dc : probe.getHintedHandoffDisabledDCs())
            System.out.println(String.format("Data center %s is disabled", dc));
    }
}
