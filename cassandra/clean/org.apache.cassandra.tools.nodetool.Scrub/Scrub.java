import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;

import java.util.ArrayList;
import java.util.List;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;
import org.apache.cassandra.tools.StandaloneScrubber;

@Command(name = "scrub", description = "Scrub (rebuild sstables for) one or more tables")
public class Scrub extends NodeToolCmd
{
    @Arguments(usage = "[<keyspace> <tables>...]", description = "The keyspace followed by one or many tables")
    private List<String> args = new ArrayList<>();

    @Option(title = "disable_snapshot",
            name = {"-ns", "--no-snapshot"},
            description = "Scrubbed CFs will be snapshotted first, if disableSnapshot is false. (default false)")
    private boolean disableSnapshot = false;

    @Option(title = "skip_corrupted",
            name = {"-s", "--skip-corrupted"},
            description = "Skip corrupted partitions even when scrubbing counter tables. (default false)")
    private boolean skipCorrupted = false;

    @Option(title = "no_validate",
                   name = {"-n", "--no-validate"},
                   description = "Do not validate columns using column validator")
    private boolean noValidation = false;

    @Option(title = "reinsert_overflowed_ttl",
    name = {"r", "--reinsert-overflowed-ttl"},
    description = StandaloneScrubber.REINSERT_OVERFLOWED_TTL_OPTION_DESCRIPTION)
    private boolean reinsertOverflowedTTL = false;

    @Option(title = "jobs",
            name = {"-j", "--jobs"},
            description = "Number of sstables to scrub simultanously, set to 0 to use all available compaction threads")
    private int jobs = 2;

    @Override
    public void execute(NodeProbe probe)
    {
        List<String> keyspaces = parseOptionalKeyspace(args, probe);
        String[] tableNames = parseOptionalTables(args);

        for (String keyspace : keyspaces)
        {
            try
            {
                probe.scrub(System.out, disableSnapshot, skipCorrupted, !noValidation, reinsertOverflowedTTL, jobs, keyspace, tableNames);
            }
            catch (IllegalArgumentException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error occurred during scrubbing", e);
            }
        }
    }
}
