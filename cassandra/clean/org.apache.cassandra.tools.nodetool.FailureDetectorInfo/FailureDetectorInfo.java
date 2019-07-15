import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Command;

import java.util.List;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "failuredetector", description = "Shows the failure detector information for the cluster")
public class FailureDetectorInfo extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        TabularData data = probe.getFailureDetectorPhilValues();
        System.out.printf("%10s,%16s%n", "Endpoint", "Phi");
        for (Object o : data.keySet())
        {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            CompositeData datum = data.get(((List) o).toArray(new Object[((List) o).size()]));
            System.out.printf("%10s,%16.8f%n",datum.get("Endpoint"), datum.get("PHI"));
        }
    }
}

