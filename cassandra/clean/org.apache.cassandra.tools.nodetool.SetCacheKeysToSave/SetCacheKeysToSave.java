import org.apache.cassandra.tools.nodetool.*;


import static com.google.common.base.Preconditions.checkArgument;
import io.airlift.command.Arguments;
import io.airlift.command.Command;

import java.util.ArrayList;
import java.util.List;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "setcachekeystosave", description = "Set number of keys saved by each cache for faster post-restart warmup. 0 to disable")
public class SetCacheKeysToSave extends NodeToolCmd
{
    @Arguments(title = "<key-cache-keys-to-save> <row-cache-keys-to-save> <counter-cache-keys-to-save>",
               usage = "<key-cache-keys-to-save> <row-cache-keys-to-save> <counter-cache-keys-to-save>",
               description = "The number of keys saved by each cache. 0 to disable",
               required = true)
    private List<Integer> args = new ArrayList<>();

    @Override
    public void execute(NodeProbe probe)
    {
        checkArgument(args.size() == 3, "setcachekeystosave requires key-cache-keys-to-save, row-cache-keys-to-save, and counter-cache-keys-to-save args.");
        probe.setCacheKeysToSave(args.get(0), args.get(1), args.get(2));
    }
}
