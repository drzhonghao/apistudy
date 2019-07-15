import org.apache.cassandra.tools.nodetool.*;


import static com.google.common.base.Preconditions.checkArgument;
import io.airlift.command.Arguments;
import io.airlift.command.Command;

import java.util.ArrayList;
import java.util.List;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "setcachecapacity", description = "Set global key, row, and counter cache capacities (in MB units)")
public class SetCacheCapacity extends NodeToolCmd
{
    @Arguments(title = "<key-cache-capacity> <row-cache-capacity> <counter-cache-capacity>",
               usage = "<key-cache-capacity> <row-cache-capacity> <counter-cache-capacity>",
               description = "Key cache, row cache, and counter cache (in MB)",
               required = true)
    private List<Integer> args = new ArrayList<>();

    @Override
    public void execute(NodeProbe probe)
    {
        checkArgument(args.size() == 3, "setcachecapacity requires key-cache-capacity, row-cache-capacity, and counter-cache-capacity args.");
        probe.setCacheCapacities(args.get(0), args.get(1), args.get(2));
    }
}
