import org.apache.cassandra.tools.nodetool.*;


import static com.google.common.base.Preconditions.checkArgument;
import io.airlift.command.Arguments;
import io.airlift.command.Command;

import java.util.ArrayList;
import java.util.List;

import org.apache.cassandra.db.ColumnFamilyStoreMBean;
import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "getcompactionthreshold", description = "Print min and max compaction thresholds for a given table")
public class GetCompactionThreshold extends NodeToolCmd
{
    @Arguments(usage = "<keyspace> <table>", description = "The keyspace with a table")
    private List<String> args = new ArrayList<>();

    @Override
    public void execute(NodeProbe probe)
    {
        checkArgument(args.size() == 2, "getcompactionthreshold requires ks and cf args");
        String ks = args.get(0);
        String cf = args.get(1);

        ColumnFamilyStoreMBean cfsProxy = probe.getCfsProxy(ks, cf);
        System.out.println("Current compaction thresholds for " + ks + "/" + cf + ": \n" +
                " min = " + cfsProxy.getMinimumCompactionThreshold() + ", " +
                " max = " + cfsProxy.getMaximumCompactionThreshold());
    }
}
