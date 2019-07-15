import org.apache.accumulo.shell.commands.TableOperation;
import org.apache.accumulo.shell.commands.OptUtil;
import org.apache.accumulo.shell.commands.*;


import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.shell.Shell;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.hadoop.io.Text;

public class FlushCommand extends TableOperation {
  private Text startRow;
  private Text endRow;

  private boolean wait;
  private Option waitOpt;

  @Override
  public String description() {
    return "flushes a tables data that is currently in memory to disk";
  }

  @Override
  protected void doTableOp(final Shell shellState, final String tableName)
      throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
    shellState.getConnector().tableOperations().flush(tableName, startRow, endRow, wait);
    Shell.log.info("Flush of table " + tableName + (wait ? " completed." : " initiated..."));
  }

  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws Exception {
    wait = cl.hasOption(waitOpt.getLongOpt());
    startRow = OptUtil.getStartRow(cl);
    endRow = OptUtil.getEndRow(cl);
    return super.execute(fullCommand, cl, shellState);
  }

  @Override
  public Options getOptions() {
    final Options opts = super.getOptions();
    waitOpt = new Option("w", "wait", false, "wait for flush to finish");
    opts.addOption(waitOpt);
    opts.addOption(OptUtil.startRowOpt());
    opts.addOption(OptUtil.endRowOpt());

    return opts;
  }
}
