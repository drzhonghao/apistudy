import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "enablebinary", description = "Reenable native transport (binary protocol)")
public class EnableBinary extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        probe.startNativeTransport();
    }
}
