import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Arguments;
import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "sethintedhandoffthrottlekb", description =  "Set hinted handoff throttle in kb per second, per delivery thread.")
public class SetHintedHandoffThrottleInKB extends NodeToolCmd
{
    @Arguments(title = "throttle_in_kb", usage = "<value_in_kb_per_sec>", description = "Value in KB per second", required = true)
    private Integer throttleInKB = null;

    @Override
    public void execute(NodeProbe probe)
    {
        probe.setHintedHandoffThrottleInKB(throttleInKB);
    }
}
