import org.apache.cassandra.tools.nodetool.*;


import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;

import java.util.ArrayList;
import java.util.List;

import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.tools.NodeTool.NodeToolCmd;

@Command(name = "verify", description = "Verify (check data checksum for) one or more tables")
public class Verify extends NodeToolCmd
{
    @Arguments(usage = "[<keyspace> <tables>...]", description = "The keyspace followed by one or many tables")
    private List<String> args = new ArrayList<>();

    @Option(title = "extended_verify",
        name = {"-e", "--extended-verify"},
        description = "Verify each cell data, beyond simply checking sstable checksums")
    private boolean extendedVerify = false;

    @Override
    public void execute(NodeProbe probe)
    {
        List<String> keyspaces = parseOptionalKeyspace(args, probe);
        String[] tableNames = parseOptionalTables(args);

        for (String keyspace : keyspaces)
        {
            try
            {
                probe.verify(System.out, extendedVerify, keyspace, tableNames);
            } catch (Exception e)
            {
                throw new RuntimeException("Error occurred during verifying", e);
            }
        }
    }
}
