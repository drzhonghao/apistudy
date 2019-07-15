import org.apache.accumulo.shell.commands.*;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.accumulo.core.client.impl.MasterClient;
import org.apache.accumulo.core.master.thrift.MasterClientService;
import org.apache.accumulo.core.master.thrift.MasterMonitorInfo;
import org.apache.accumulo.core.trace.Tracer;
import org.apache.accumulo.server.AccumuloServerContext;
import org.apache.accumulo.server.conf.ServerConfigurationFactory;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class ListBulkCommand extends Command {

  private Option tserverOption, disablePaginationOpt;

  @Override
  public String description() {
    return "lists what bulk imports are currently running in accumulo.";
  }

  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws Exception {

    List<String> tservers;

    MasterMonitorInfo stats;
    MasterClientService.Iface client = null;
    try {
      AccumuloServerContext context = new AccumuloServerContext(
          new ServerConfigurationFactory(shellState.getInstance()));
      client = MasterClient.getConnectionWithRetry(context);
      stats = client.getMasterStats(Tracer.traceInfo(), context.rpcCreds());
    } finally {
      if (client != null)
        MasterClient.close(client);
    }

    final boolean paginate = !cl.hasOption(disablePaginationOpt.getOpt());

    if (cl.hasOption(tserverOption.getOpt())) {
      tservers = new ArrayList<>();
      tservers.add(cl.getOptionValue(tserverOption.getOpt()));
    } else {
      tservers = Collections.emptyList();
    }

    shellState.printLines(new BulkImportListIterator(tservers, stats), paginate);
    return 0;
  }

  @Override
  public int numArgs() {
    return 0;
  }

  @Override
  public Options getOptions() {
    final Options opts = new Options();

    tserverOption = new Option("ts", "tabletServer", true, "tablet server to list bulk imports");
    tserverOption.setArgName("tablet server");
    opts.addOption(tserverOption);

    disablePaginationOpt = new Option("np", "no-pagination", false, "disable pagination of output");
    opts.addOption(disablePaginationOpt);

    return opts;
  }

}
