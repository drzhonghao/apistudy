import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "disablethrift", description = "Disable thrift server")
public class DisableThrift extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        probe.stopThriftServer();
    }
}
