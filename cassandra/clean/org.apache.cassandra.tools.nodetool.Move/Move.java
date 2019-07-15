import org.apache.cassandra.tools.nodetool.*;


import static org.apache.commons.lang3.StringUtils.EMPTY;
import io.airlift.command.Arguments;
import io.airlift.command.Command;

import java.io.IOException;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "move", description = "Move node on the token ring to a new token")
public class Move extends NodeToolCmd
{
    @Arguments(usage = "<new token>", description = "The new token.", required = true)
    private String newToken = EMPTY;

    @Override
    public void execute(NodeProbe probe)
    {
        try
        {
            probe.move(newToken);
        } catch (IOException e)
        {
            throw new RuntimeException("Error during moving node", e);
        }
    }
}
