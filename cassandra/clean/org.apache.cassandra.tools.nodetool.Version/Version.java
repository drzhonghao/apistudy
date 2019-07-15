import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "version", description = "Print cassandra version")
public class Version extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        System.out.println("ReleaseVersion: " + probe.getReleaseVersion());
    }
}
