import org.apache.cassandra.tools.nodetool.*;


import static com.google.common.base.Preconditions.checkArgument;
import io.airlift.command.Arguments;
import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "settraceprobability", description = "Sets the probability for tracing any given request to value. 0 disables, 1 enables for all requests, 0 is the default")
public class SetTraceProbability extends NodeToolCmd
{
    @Arguments(title = "trace_probability", usage = "<value>", description = "Trace probability between 0 and 1 (ex: 0.2)", required = true)
    private Double traceProbability = null;

    @Override
    public void execute(NodeProbe probe)
    {
        checkArgument(traceProbability >= 0 && traceProbability <= 1, "Trace probability must be between 0 and 1");
        probe.setTraceProbability(traceProbability);
    }
}
