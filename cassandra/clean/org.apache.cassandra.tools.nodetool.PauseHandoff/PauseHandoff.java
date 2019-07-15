import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "pausehandoff", description = "Pause hints delivery process")
public class PauseHandoff extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        probe.pauseHintsDelivery();
    }
}
