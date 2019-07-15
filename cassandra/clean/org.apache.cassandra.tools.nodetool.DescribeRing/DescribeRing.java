import org.apache.cassandra.tools.nodetool.*;


import static org.apache.commons.lang3.StringUtils.EMPTY;
import io.airlift.command.Arguments;
import io.airlift.command.Command;

import java.io.IOException;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "describering", description = "Shows the token ranges info of a given keyspace")
public class DescribeRing extends NodeToolCmd
{
    @Arguments(description = "The keyspace name", required = true)
    String keyspace = EMPTY;

    @Override
    public void execute(NodeProbe probe)
    {
        System.out.println("Schema Version:" + probe.getSchemaVersion());
        System.out.println("TokenRange: ");
        try
        {
            for (String tokenRangeString : probe.describeRing(keyspace))
            {
                System.out.println("\t" + tokenRangeString);
            }
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
