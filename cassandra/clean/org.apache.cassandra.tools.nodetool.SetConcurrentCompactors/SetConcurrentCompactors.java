import org.apache.cassandra.tools.nodetool.*;


import static com.google.common.base.Preconditions.checkArgument;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool;

@Command(name = "setconcurrentcompactors", description = "Set number of concurrent compactors in the system.")
public class SetConcurrentCompactors extends NodeTool.NodeToolCmd
{
    @Arguments(title = "concurrent_compactors", usage = "<value>", description = "Number of concurrent compactors, greater than 0.", required = true)
    private Integer concurrentCompactors = null;

    protected void execute(NodeProbe probe)
    {
        checkArgument(concurrentCompactors > 0, "concurrent_compactors should be great than 0.");
        probe.setConcurrentCompactors(concurrentCompactors);
    }
}
