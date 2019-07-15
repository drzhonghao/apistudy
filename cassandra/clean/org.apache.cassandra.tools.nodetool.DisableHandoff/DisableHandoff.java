import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "disablehandoff", description = "Disable storing hinted handoffs")
public class DisableHandoff extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        probe.disableHintedHandoff();
    }
}
