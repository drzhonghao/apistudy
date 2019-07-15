import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "invalidatecountercache", description = "Invalidate the counter cache")
public class InvalidateCounterCache extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        probe.invalidateCounterCache();
    }
}
