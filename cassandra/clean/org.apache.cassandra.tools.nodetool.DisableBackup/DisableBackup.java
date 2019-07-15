import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "disablebackup", description = "Disable incremental backup")
public class DisableBackup extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        probe.setIncrementalBackupsEnabled(false);
    }
}
