import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "enablehandoff", description = "Reenable future hints storing on the current node")
public class EnableHandoff extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        probe.enableHintedHandoff();
    }
}
