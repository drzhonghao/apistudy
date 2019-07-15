import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "invalidatekeycache", description = "Invalidate the key cache")
public class InvalidateKeyCache extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        probe.invalidateKeyCache();
    }
}
