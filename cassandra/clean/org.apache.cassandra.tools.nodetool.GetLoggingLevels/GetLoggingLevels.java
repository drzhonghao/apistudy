import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import java.util.Map;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "getlogginglevels", description = "Get the runtime logging levels")
public class GetLoggingLevels extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        // what if some one set a very long logger name? 50 space may not be enough...
        System.out.printf("%n%-50s%10s%n", "Logger Name", "Log Level");
        for (Map.Entry<String, String> entry : probe.getLoggingLevels().entrySet())
            System.out.printf("%-50s%10s%n", entry.getKey(), entry.getValue());
    }
}
