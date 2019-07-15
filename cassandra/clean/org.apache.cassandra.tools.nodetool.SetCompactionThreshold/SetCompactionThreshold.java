import org.apache.cassandra.tools.nodetool.*;


import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Integer.parseInt;
import io.airlift.command.Arguments;
import io.airlift.command.Command;

import java.util.ArrayList;
import java.util.List;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "setcompactionthreshold", description = "Set min and max compaction thresholds for a given table")
public class SetCompactionThreshold extends NodeToolCmd
{
    @Arguments(title = "<keyspace> <table> <minthreshold> <maxthreshold>", usage = "<keyspace> <table> <minthreshold> <maxthreshold>", description = "The keyspace, the table, min and max threshold", required = true)
    private List<String> args = new ArrayList<>();

    @Override
    public void execute(NodeProbe probe)
    {
        checkArgument(args.size() == 4, "setcompactionthreshold requires ks, cf, min, and max threshold args.");

        int minthreshold = parseInt(args.get(2));
        int maxthreshold = parseInt(args.get(3));
        checkArgument(minthreshold >= 0 && maxthreshold >= 0, "Thresholds must be positive integers");
        checkArgument(minthreshold <= maxthreshold, "Min threshold cannot be greater than max.");
        checkArgument(minthreshold >= 2 || maxthreshold == 0, "Min threshold must be at least 2");

        probe.setCompactionThreshold(args.get(0), args.get(1), minthreshold, maxthreshold);
    }
}
