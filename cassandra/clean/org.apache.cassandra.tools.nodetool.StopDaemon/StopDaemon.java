import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;
import org.apache.cassandra.utils.JVMStabilityInspector;

@Command(name = "stopdaemon", description = "Stop cassandra daemon")
public class StopDaemon extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        try
        {
            DatabaseDescriptor.toolInitialization();
            probe.stopCassandraDaemon();
        } catch (Exception e)
        {
            JVMStabilityInspector.inspectThrowable(e);
            // ignored
        }
    }
}
