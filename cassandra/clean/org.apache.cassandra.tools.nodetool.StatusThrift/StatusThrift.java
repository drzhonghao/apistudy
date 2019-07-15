import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "statusthrift", description = "Status of thrift server")
public class StatusThrift extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        System.out.println(
                probe.isThriftServerRunning()
                ? "running"
                : "not running");
    }
}
