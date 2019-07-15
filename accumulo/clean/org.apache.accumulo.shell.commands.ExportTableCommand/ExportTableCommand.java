import org.apache.accumulo.shell.commands.OptUtil;
import org.apache.accumulo.shell.commands.*;


import java.util.Map;
import java.util.Set;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.accumulo.shell.ShellOptions;
import org.apache.accumulo.shell.Token;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class ExportTableCommand extends Command {

  private Option tableOpt;

  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws AccumuloException, AccumuloSecurityException, TableNotFoundException,
      TableExistsException {

    final String tableName = OptUtil.getTableOpt(cl, shellState);

    shellState.getConnector().tableOperations().exportTable(tableName, cl.getArgs()[0]);
    return 0;
  }

  @Override
  public String usage() {
    return getName() + " <export dir>";
  }

  @Override
  public Options getOptions() {
    final Options o = new Options();

    tableOpt = new Option(ShellOptions.tableOption, "table", true, "table to export");

    tableOpt.setArgName("table");

    o.addOption(tableOpt);

    return o;
  }

  @Override
  public String description() {
    return "exports a table";
  }

  @Override
  public void registerCompletion(final Token root,
      final Map<Command.CompletionSet,Set<String>> completionSet) {
    registerCompletionForTables(root, completionSet);
  }

  @Override
  public int numArgs() {
    return 1;
  }
}
