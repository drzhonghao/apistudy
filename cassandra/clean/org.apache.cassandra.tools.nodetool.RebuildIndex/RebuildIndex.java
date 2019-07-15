import org.apache.cassandra.tools.nodetool.*;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.toArray;
import io.airlift.command.Arguments;
import io.airlift.command.Command;

import java.util.ArrayList;
import java.util.List;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "rebuild_index", description = "A full rebuild of native secondary indexes for a given table")
public class RebuildIndex extends NodeToolCmd
{
    @Arguments(usage = "<keyspace> <table> <indexName...>", description = "The keyspace and table name followed by a list of index names")
    List<String> args = new ArrayList<>();

    @Override
    public void execute(NodeProbe probe)
    {
        checkArgument(args.size() >= 3, "rebuild_index requires ks, cf and idx args");
        probe.rebuildIndex(args.get(0), args.get(1), toArray(args.subList(2, args.size()), String.class));
    }
}
