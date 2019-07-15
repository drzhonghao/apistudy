import org.apache.accumulo.shell.commands.*;


import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.metadata.MetadataTable;
import org.apache.accumulo.shell.Shell;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class OfflineCommand extends TableOperation {

  private boolean wait;
  private Option waitOpt;

  @Override
  public String description() {
    return "starts the process of taking table offline";
  }

  @Override
  protected void doTableOp(final Shell shellState, final String tableName)
      throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
    if (tableName.equals(MetadataTable.NAME)) {
      Shell.log.info("  You cannot take the " + MetadataTable.NAME + " offline.");
    } else {
      shellState.getConnector().tableOperations().offline(tableName, wait);
      Shell.log.info("Offline of table " + tableName + (wait ? " completed." : " initiated..."));
    }
  }

  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws Exception {
    wait = cl.hasOption(waitOpt.getLongOpt());
    return super.execute(fullCommand, cl, shellState);
  }

  @Override
  public Options getOptions() {
    final Options opts = super.getOptions();
    waitOpt = new Option("w", "wait", false, "wait for offline to finish");
    opts.addOption(waitOpt);
    return opts;
  }
}
