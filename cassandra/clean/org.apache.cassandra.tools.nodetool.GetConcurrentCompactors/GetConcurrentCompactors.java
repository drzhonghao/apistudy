import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;
import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "getconcurrentcompactors", description = "Get the number of concurrent compactors in the system.")
public class GetConcurrentCompactors extends NodeToolCmd
{
    protected void execute(NodeProbe probe)
    {
        System.out.println("Current concurrent compactors in the system is: \n" +
                           probe.getConcurrentCompactors());
    }
}
