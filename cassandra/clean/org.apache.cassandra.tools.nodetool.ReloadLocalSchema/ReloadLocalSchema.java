import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;
import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "reloadlocalschema", description = "Reload local node schema from system tables")
public class ReloadLocalSchema extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        probe.reloadLocalSchema();
    }
}
