import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import java.util.List;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "rangekeysample", description = "Shows the sampled keys held across all keyspaces")
public class RangeKeySample extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        System.out.println("RangeKeySample: ");
        List<String> tokenStrings = probe.sampleKeyRange();
        for (String tokenString : tokenStrings)
        {
            System.out.println("\t" + tokenString);
        }
    }
}
