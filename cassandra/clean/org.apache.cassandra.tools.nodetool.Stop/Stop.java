import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;

import org.apache.cassandra.db.compaction.OperationType;
import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "stop", description = "Stop compaction")
public class Stop extends NodeToolCmd
{
    @Arguments(title = "compaction_type",
              usage = "<compaction type>",
              description = "Supported types are COMPACTION, VALIDATION, CLEANUP, SCRUB, VERIFY, INDEX_BUILD",
              required = false)
    private OperationType compactionType = OperationType.UNKNOWN;

    @Option(title = "compactionId",
           name = {"-id", "--compaction-id"},
           description = "Use -id to stop a compaction by the specified id. Ids can be found in the transaction log files whose name starts with compaction_, located in the table transactions folder.",
           required = false)
    private String compactionId = "";

    @Override
    public void execute(NodeProbe probe)
    {
        if (!compactionId.isEmpty())
            probe.stopById(compactionId);
        else
            probe.stop(compactionType.name());
    }
}
