import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Arguments;
import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "setinterdcstreamthroughput", description = "Set the Mb/s throughput cap for inter-datacenter streaming in the system, or 0 to disable throttling")
public class SetInterDCStreamThroughput extends NodeToolCmd
{
    @Arguments(title = "inter_dc_stream_throughput", usage = "<value_in_mb>", description = "Value in Mb, 0 to disable throttling", required = true)
    private Integer interDCStreamThroughput = null;

    @Override
    public void execute(NodeProbe probe)
    {
        probe.setInterDCStreamThroughput(interDCStreamThroughput);
    }
}
