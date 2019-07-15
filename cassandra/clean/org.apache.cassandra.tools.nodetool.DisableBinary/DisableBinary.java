import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "disablebinary", description = "Disable native transport (binary protocol)")
public class DisableBinary extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        probe.stopNativeTransport();
    }
}
