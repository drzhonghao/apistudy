import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "getstreamthroughput", description = "Print the Mb/s throughput cap for streaming in the system")
public class GetStreamThroughput extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        System.out.println("Current stream throughput: " + probe.getStreamThroughput() + " Mb/s");
    }
}
