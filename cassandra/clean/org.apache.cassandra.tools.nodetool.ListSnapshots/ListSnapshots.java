import org.apache.cassandra.tools.nodetool.*;


import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.openmbean.TabularData;

import io.airlift.command.Command;

import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;
import org.apache.cassandra.tools.nodetool.formatter.TableBuilder;

@Command(name = "listsnapshots", description = "Lists all the snapshots along with the size on disk and true size.")
public class ListSnapshots extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        try
        {
            System.out.println("Snapshot Details: ");

            final Map<String,TabularData> snapshotDetails = probe.getSnapshotDetails();
            if (snapshotDetails.isEmpty())
            {
                System.out.println("There are no snapshots");
                return;
            }

            final long trueSnapshotsSize = probe.trueSnapshotsSize();
            TableBuilder table = new TableBuilder();
            // display column names only once
            final List<String> indexNames = snapshotDetails.entrySet().iterator().next().getValue().getTabularType().getIndexNames();
            table.add(indexNames.toArray(new String[indexNames.size()]));

            for (final Map.Entry<String, TabularData> snapshotDetail : snapshotDetails.entrySet())
            {
                Set<?> values = snapshotDetail.getValue().keySet();
                for (Object eachValue : values)
                {
                    final List<?> value = (List<?>) eachValue;
                    table.add(value.toArray(new String[value.size()]));
                }
            }
            table.printTo(System.out);

            System.out.println("\nTotal TrueDiskSpaceUsed: " + FileUtils.stringifyFileSize(trueSnapshotsSize) + "\n");
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error during list snapshot", e);
        }
    }
}
