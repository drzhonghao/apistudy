import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "resumehandoff", description = "Resume hints delivery process")
public class ResumeHandoff extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        probe.resumeHintsDelivery();
    }
}
