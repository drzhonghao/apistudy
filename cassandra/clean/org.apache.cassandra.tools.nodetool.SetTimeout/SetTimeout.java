import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Arguments;
import io.airlift.command.Command;

import java.util.ArrayList;
import java.util.List;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

import static com.google.common.base.Preconditions.checkArgument;

@Command(name = "settimeout", description = "Set the specified timeout in ms, or 0 to disable timeout")
public class SetTimeout extends NodeToolCmd
{
    @Arguments(usage = "<timeout_type> <timeout_in_ms>", description = "Timeout type followed by value in ms " +
            "(0 disables socket streaming timeout). Type should be one of (" + GetTimeout.TIMEOUT_TYPES + ")",
            required = true)
    private List<String> args = new ArrayList<>();

    @Override
    public void execute(NodeProbe probe)
    {
        checkArgument(args.size() == 2, "Timeout type followed by value in ms (0 disables socket streaming timeout)." +
                " Type should be one of (" + GetTimeout.TIMEOUT_TYPES + ")");

        try
        {
            String type = args.get(0);
            long timeout = Long.parseLong(args.get(1));
            probe.setTimeout(type, timeout);
        } catch (Exception e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
