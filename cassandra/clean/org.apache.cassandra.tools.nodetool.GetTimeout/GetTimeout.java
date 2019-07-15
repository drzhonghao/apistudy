import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Arguments;
import io.airlift.command.Command;

import java.util.ArrayList;
import java.util.List;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

import static com.google.common.base.Preconditions.checkArgument;

@Command(name = "gettimeout", description = "Print the timeout of the given type in ms")
public class GetTimeout extends NodeToolCmd
{
    public static final String TIMEOUT_TYPES = "read, range, write, counterwrite, cascontention, truncate, streamingsocket, misc (general rpc_timeout_in_ms)";

    @Arguments(usage = "<timeout_type>", description = "The timeout type, one of (" + TIMEOUT_TYPES + ")")
    private List<String> args = new ArrayList<>();

    @Override
    public void execute(NodeProbe probe)
    {
        checkArgument(args.size() == 1, "gettimeout requires a timeout type, one of (" + TIMEOUT_TYPES + ")");
        try
        {
            System.out.println("Current timeout for type " + args.get(0) + ": " + probe.getTimeout(args.get(0)) + " ms");
        } catch (Exception e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }

    }
}
