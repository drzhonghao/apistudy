import org.apache.cassandra.tools.nodetool.*;


import static java.lang.String.format;
import io.airlift.command.Command;

import java.util.List;
import java.util.Map;

import org.apache.cassandra.locator.DynamicEndpointSnitch;
import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "describecluster", description = "Print the name, snitch, partitioner and schema version of a cluster")
public class DescribeCluster extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        // display cluster name, snitch and partitioner
        System.out.println("Cluster Information:");
        System.out.println("\tName: " + probe.getClusterName());
        String snitch = probe.getEndpointSnitchInfoProxy().getSnitchName();
        boolean dynamicSnitchEnabled = false;
        if (snitch.equals(DynamicEndpointSnitch.class.getName()))
        {
            snitch = probe.getDynamicEndpointSnitchInfoProxy().getSubsnitchClassName();
            dynamicSnitchEnabled = true;
        }
        System.out.println("\tSnitch: " + snitch);
        System.out.println("\tDynamicEndPointSnitch: " + (dynamicSnitchEnabled ? "enabled" : "disabled"));
        System.out.println("\tPartitioner: " + probe.getPartitioner());

        // display schema version for each node
        System.out.println("\tSchema versions:");
        Map<String, List<String>> schemaVersions = probe.getSpProxy().getSchemaVersions();
        for (String version : schemaVersions.keySet())
        {
            System.out.println(format("\t\t%s: %s%n", version, schemaVersions.get(version)));
        }
    }
}