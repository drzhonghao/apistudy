import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "getinterdcstreamthroughput", description = "Print the Mb/s throughput cap for inter-datacenter streaming in the system")
public class GetInterDCStreamThroughput extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        System.out.println("Current inter-datacenter stream throughput: " + probe.getInterDCStreamThroughput() + " Mb/s");
    }
}
