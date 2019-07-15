import org.apache.cassandra.tools.nodetool.*;


import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

import io.airlift.command.Command;

@Command(name = "gcstats", description = "Print GC Statistics")
public class GcStats extends NodeToolCmd
{
    @Override
    public void execute(NodeProbe probe)
    {
        double[] stats = probe.getAndResetGCStats();
        double mean = stats[2] / stats[5];
        double stdev = Math.sqrt((stats[3] / stats[5]) - (mean * mean));
        System.out.printf("%20s%20s%20s%20s%20s%20s%25s%n", "Interval (ms)", "Max GC Elapsed (ms)", "Total GC Elapsed (ms)", "Stdev GC Elapsed (ms)", "GC Reclaimed (MB)", "Collections", "Direct Memory Bytes");
        System.out.printf("%20.0f%20.0f%20.0f%20.0f%20.0f%20.0f%25d%n", stats[0], stats[1], stats[2], stdev, stats[4], stats[5], (long)stats[6]);
    }
}
