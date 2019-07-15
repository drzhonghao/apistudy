import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "statusbackup", description = "Status of incremental backup")
public class StatusBackup extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        System.out.println(
                probe.isIncrementalBackupsEnabled()
                ? "running"
                : "not running");
    }
}
