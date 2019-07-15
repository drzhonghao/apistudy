import org.apache.cassandra.tools.nodetool.*;


import static com.google.common.base.Preconditions.checkArgument;
import io.airlift.command.Arguments;
import io.airlift.command.Command;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "getendpoints", description = "Print the end points that owns the key")
public class GetEndpoints extends NodeToolCmd
{
    @Arguments(usage = "<keyspace> <table> <key>", description = "The keyspace, the table, and the partition key for which we need to find the endpoint")
    private List<String> args = new ArrayList<>();

    @Override
    public void execute(NodeProbe probe)
    {
        checkArgument(args.size() == 3, "getendpoints requires keyspace, table and partition key arguments");
        String ks = args.get(0);
        String table = args.get(1);
        String key = args.get(2);

        List<InetAddress> endpoints = probe.getEndpoints(ks, table, key);
        for (InetAddress endpoint : endpoints)
        {
            System.out.println(endpoint.getHostAddress());
        }
    }
}
