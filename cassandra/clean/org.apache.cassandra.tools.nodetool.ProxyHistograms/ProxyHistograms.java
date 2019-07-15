import org.apache.cassandra.tools.nodetool.*;


import static java.lang.String.format;
import io.airlift.command.Command;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "proxyhistograms", description = "Print statistic histograms for network operations")
public class ProxyHistograms extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        String[] percentiles = {"50%", "75%", "95%", "98%", "99%", "Min", "Max"};
        double[] readLatency = probe.metricPercentilesAsArray(probe.getProxyMetric("Read"));
        double[] writeLatency = probe.metricPercentilesAsArray(probe.getProxyMetric("Write"));
        double[] rangeLatency = probe.metricPercentilesAsArray(probe.getProxyMetric("RangeSlice"));
        double[] casReadLatency = probe.metricPercentilesAsArray(probe.getProxyMetric("CASRead"));
        double[] casWriteLatency = probe.metricPercentilesAsArray(probe.getProxyMetric("CASWrite"));
        double[] viewWriteLatency = probe.metricPercentilesAsArray(probe.getProxyMetric("ViewWrite"));

        System.out.println("proxy histograms");
        System.out.println(format("%-10s%19s%19s%19s%19s%19s%19s",
                "Percentile", "Read Latency", "Write Latency", "Range Latency", "CAS Read Latency", "CAS Write Latency", "View Write Latency"));
        System.out.println(format("%-10s%19s%19s%19s%19s%19s%19s",
                "", "(micros)", "(micros)", "(micros)", "(micros)", "(micros)", "(micros)"));
        for (int i = 0; i < percentiles.length; i++)
        {
            System.out.println(format("%-10s%19.2f%19.2f%19.2f%19.2f%19.2f%19.2f",
                    percentiles[i],
                    readLatency[i],
                    writeLatency[i],
                    rangeLatency[i],
                    casReadLatency[i],
                    casWriteLatency[i],
                    viewWriteLatency[i]));
        }
        System.out.println();
    }
}
