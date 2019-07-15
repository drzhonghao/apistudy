import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "enablebackup", description = "Enable incremental backup")
public class EnableBackup extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        probe.setIncrementalBackupsEnabled(true);
    }
}
