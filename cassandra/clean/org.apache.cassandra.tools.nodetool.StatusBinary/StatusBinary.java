import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "statusbinary", description = "Status of native transport (binary protocol)")
public class StatusBinary extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        System.out.println(
                probe.isNativeTransportRunning()
                ? "running"
                : "not running");
    }
}
