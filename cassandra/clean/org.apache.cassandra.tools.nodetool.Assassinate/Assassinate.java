import org.apache.cassandra.tools.nodetool.*;


import static org.apache.commons.lang3.StringUtils.EMPTY;
import io.airlift.command.Arguments;
import io.airlift.command.Command;

import java.net.UnknownHostException;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "assassinate", description = "Forcefully remove a dead node without re-replicating any data.  Use as a last resort if you cannot removenode")
public class Assassinate extends NodeToolCmd
{
    @Arguments(title = "ip address", usage = "<ip_address>", description = "IP address of the endpoint to assassinate", required = true)
    private String endpoint = EMPTY;

    @Override
    public void execute(NodeProbe probe)
    {
        try
        {
            probe.assassinateEndpoint(endpoint);
        }
        catch (UnknownHostException e)
        {
            throw new RuntimeException(e);
        }
    }
}
