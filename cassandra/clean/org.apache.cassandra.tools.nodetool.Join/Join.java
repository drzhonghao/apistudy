import org.apache.cassandra.tools.nodetool.*;


import static com.google.common.base.Preconditions.checkState;
import io.airlift.command.Command;

import java.io.IOException;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "join", description = "Join the ring")
public class Join extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        checkState(!probe.isJoined(), "This node has already joined the ring.");

        try
        {
            probe.joinRing();
        } catch (IOException e)
        {
            throw new RuntimeException("Error during joining the ring", e);
        }
    }
}
