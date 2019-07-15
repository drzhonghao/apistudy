import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "gettraceprobability", description = "Print the current trace probability value")
public class GetTraceProbability extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        System.out.println("Current trace probability: " + probe.getTraceProbability());
    }
}
