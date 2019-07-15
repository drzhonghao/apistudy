import org.apache.cassandra.tools.nodetool.*;


import static com.google.common.base.Preconditions.checkArgument;
import io.airlift.command.Arguments;
import io.airlift.command.Command;

import java.util.ArrayList;
import java.util.List;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "refresh", description = "Load newly placed SSTables to the system without restart")
public class Refresh extends NodeToolCmd
{
    @Arguments(usage = "<keyspace> <table>", description = "The keyspace and table name")
    private List<String> args = new ArrayList<>();

    @Override
    public void execute(NodeProbe probe)
    {
        checkArgument(args.size() == 2, "refresh requires ks and cf args");
        probe.loadNewSSTables(args.get(0), args.get(1));
    }
}
