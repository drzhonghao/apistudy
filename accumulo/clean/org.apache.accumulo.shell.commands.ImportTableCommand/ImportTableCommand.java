import org.apache.accumulo.shell.commands.*;


import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.commons.cli.CommandLine;

public class ImportTableCommand extends Command {

  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws AccumuloException, AccumuloSecurityException, TableNotFoundException,
      TableExistsException {

    shellState.getConnector().tableOperations().importTable(cl.getArgs()[0], cl.getArgs()[1]);
    return 0;
  }

  @Override
  public String usage() {
    return getName() + " <table name> <import dir>";
  }

  @Override
  public String description() {
    return "imports a table";
  }

  @Override
  public int numArgs() {
    return 2;
  }
}
