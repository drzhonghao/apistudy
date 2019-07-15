import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "drain", description = "Drain the node (stop accepting writes and flush all tables)")
public class Drain extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        try
        {
            probe.drain();
        } catch (IOException | InterruptedException | ExecutionException e)
        {
            throw new RuntimeException("Error occurred during flushing", e);
        }
    }
}
