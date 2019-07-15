import org.apache.accumulo.shell.commands.OptUtil;
import org.apache.accumulo.shell.commands.*;


import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.hadoop.io.Text;

public class DeleteRowsCommand extends Command {
  private Option forceOpt;

  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws Exception {
    final String tableName = OptUtil.getTableOpt(cl, shellState);
    final Text startRow = OptUtil.getStartRow(cl);
    final Text endRow = OptUtil.getEndRow(cl);
    if (!cl.hasOption(forceOpt.getOpt()) && (startRow == null || endRow == null)) {
      shellState.getReader()
          .println("Not deleting unbounded range. Specify both ends, or use --force");
      return 1;
    }
    shellState.getConnector().tableOperations().deleteRows(tableName, startRow, endRow);
    return 0;
  }

  @Override
  public String description() {
    return "deletes a range of rows in a table. Note that rows matching the"
        + " start row ARE NOT deleted, but rows matching the end row ARE deleted.";
  }

  @Override
  public int numArgs() {
    return 0;
  }

  @Override
  public Options getOptions() {
    final Options o = new Options();
    forceOpt = new Option("f", "force", false,
        "delete data even if start or end are not specified");
    o.addOption(OptUtil.startRowOpt());
    o.addOption(OptUtil.endRowOpt());
    o.addOption(OptUtil.tableOpt("table to delete a row range from"));
    o.addOption(forceOpt);
    return o;
  }
}
