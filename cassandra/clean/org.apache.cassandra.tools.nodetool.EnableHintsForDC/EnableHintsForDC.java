import org.apache.cassandra.tools.nodetool.*;


import java.util.ArrayList;
import java.util.List;

import io.airlift.command.Arguments;
import io.airlift.command.Command;
import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool;

import static com.google.common.base.Preconditions.checkArgument;

@Command(name = "enablehintsfordc", description = "Enable hints for a data center that was previsouly disabled")
public class EnableHintsForDC extends NodeTool.NodeToolCmd
{
    @Arguments(usage = "<datacenter>", description = "The data center to enable")
    private List<String> args = new ArrayList<>();

    public void execute(NodeProbe probe)
    {
        checkArgument(args.size() == 1, "enablehintsfordc requires exactly one data center");

        probe.enableHintsForDC(args.get(0));
    }
}
