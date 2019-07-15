import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "enablethrift", description = "Reenable thrift server")
public class EnableThrift extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        probe.startThriftServer();
    }
}
