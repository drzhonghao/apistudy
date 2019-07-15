import org.apache.cassandra.tools.nodetool.*;


import java.io.IOError;
import java.io.IOException;

import io.airlift.command.Command;
import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool;

@Command(name = "replaybatchlog", description = "Kick off batchlog replay and wait for finish")
public class ReplayBatchlog extends NodeTool.NodeToolCmd
{
    protected void execute(NodeProbe probe)
    {
        try
        {
            probe.replayBatchlog();
        }
        catch (IOException e)
        {
            throw new IOError(e);
        }
    }
}
