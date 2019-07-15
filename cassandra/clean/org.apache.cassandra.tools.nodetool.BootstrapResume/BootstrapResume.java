import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import java.io.IOError;
import java.io.IOException;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "resume", description = "Resume bootstrap streaming")
public class BootstrapResume extends NodeToolCmd
{
    @Override
    protected void execute(NodeProbe probe)
    {
        try
        {
            probe.resumeBootstrap(System.out);
        }
        catch (IOException e)
        {
            throw new IOError(e);
        }
    }
}
