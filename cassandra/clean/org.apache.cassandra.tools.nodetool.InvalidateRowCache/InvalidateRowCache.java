import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "invalidaterowcache", description = "Invalidate the row cache")
public class InvalidateRowCache extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        probe.invalidateRowCache();
    }
}
