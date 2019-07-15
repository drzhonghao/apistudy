import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "getcompactionthroughput", description = "Print the MB/s throughput cap for compaction in the system")
public class GetCompactionThroughput extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        System.out.println("Current compaction throughput: " + probe.getCompactionThroughput() + " MB/s");
    }
}
