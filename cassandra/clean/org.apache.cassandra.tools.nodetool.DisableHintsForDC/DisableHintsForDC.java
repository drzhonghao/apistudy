import org.apache.cassandra.tools.nodetool.*;


import java.util.ArrayList;
import java.util.List;

import io.airlift.command.Arguments;
import io.airlift.command.Command;
import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool;

import static com.google.common.base.Preconditions.checkArgument;

@Command(name = "disablehintsfordc", description = "Disable hints for a data center")
public class DisableHintsForDC extends NodeTool.NodeToolCmd
{
    @Arguments(usage = "<datacenter>", description = "The data center to disable")
    private List<String> args = new ArrayList<>();

    public void execute(NodeProbe probe)
    {
        checkArgument(args.size() == 1, "disablehintsfordc requires exactly one data center");

        probe.disableHintsForDC(args.get(0));
    }
}
