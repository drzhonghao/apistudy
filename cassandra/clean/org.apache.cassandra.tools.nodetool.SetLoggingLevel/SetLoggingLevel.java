import org.apache.cassandra.tools.nodetool.*;


import static org.apache.commons.lang3.StringUtils.EMPTY;
import io.airlift.command.Arguments;
import io.airlift.command.Command;

import java.util.ArrayList;
import java.util.List;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "setlogginglevel", description = "Set the log level threshold for a given class. If both class and level are empty/null, it will reset to the initial configuration")
public class SetLoggingLevel extends NodeToolCmd
{
    @Arguments(usage = "<class> <level>", description = "The class to change the level for and the log level threshold to set (can be empty)")
    private List<String> args = new ArrayList<>();

    @Override
    public void execute(NodeProbe probe)
    {
        String classQualifier = args.size() >= 1 ? args.get(0) : EMPTY;
        String level = args.size() == 2 ? args.get(1) : EMPTY;
        probe.setLoggingLevel(classQualifier, level);
    }
}
