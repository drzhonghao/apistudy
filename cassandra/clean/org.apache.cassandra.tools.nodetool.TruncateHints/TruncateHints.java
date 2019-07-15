import org.apache.cassandra.tools.nodetool.*;


import static org.apache.commons.lang3.StringUtils.EMPTY;
import io.airlift.command.Arguments;
import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "truncatehints", description = "Truncate all hints on the local node, or truncate hints for the endpoint(s) specified.")
public class TruncateHints extends NodeToolCmd
{
    @Arguments(usage = "[endpoint ... ]", description = "Endpoint address(es) to delete hints for, either ip address (\"127.0.0.1\") or hostname")
    private String endpoint = EMPTY;

    @Override
    public void execute(NodeProbe probe)
    {
        if (endpoint.isEmpty())
            probe.truncateHints();
        else
            probe.truncateHints(endpoint);
    }
}
