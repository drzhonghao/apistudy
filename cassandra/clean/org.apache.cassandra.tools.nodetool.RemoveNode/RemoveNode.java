import org.apache.cassandra.tools.nodetool.*;


import static org.apache.commons.lang3.StringUtils.EMPTY;
import io.airlift.command.Arguments;
import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "removenode", description = "Show status of current node removal, force completion of pending removal or remove provided ID")
public class RemoveNode extends NodeToolCmd
{
    @Arguments(title = "remove_operation", usage = "<status>|<force>|<ID>", description = "Show status of current node removal, force completion of pending removal, or remove provided ID", required = true)
    private String removeOperation = EMPTY;

    @Override
    public void execute(NodeProbe probe)
    {
        switch (removeOperation)
        {
            case "status":
                System.out.println("RemovalStatus: " + probe.getRemovalStatus());
                break;
            case "force":
                System.out.println("RemovalStatus: " + probe.getRemovalStatus());
                probe.forceRemoveCompletion();
                break;
            default:
                probe.removeNode(removeOperation);
                break;
        }
    }
}
