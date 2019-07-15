import org.apache.accumulo.shell.commands.*;


import java.util.Map;
import java.util.Set;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.accumulo.shell.Token;
import org.apache.commons.cli.CommandLine;

public class RenameTableCommand extends Command {
  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws AccumuloException, AccumuloSecurityException, TableNotFoundException,
      TableExistsException {
    shellState.getConnector().tableOperations().rename(cl.getArgs()[0], cl.getArgs()[1]);
    if (shellState.getTableName().equals(Tables.qualified(cl.getArgs()[0]))) {
      shellState.setTableName(cl.getArgs()[1]);
    }
    return 0;
  }

  @Override
  public String usage() {
    return getName() + " <current table name> <new table name>";
  }

  @Override
  public String description() {
    return "renames a table";
  }

  @Override
  public void registerCompletion(final Token root,
      final Map<Command.CompletionSet,Set<String>> completionSet) {
    registerCompletionForTables(root, completionSet);
  }

  @Override
  public int numArgs() {
    return 2;
  }
}
