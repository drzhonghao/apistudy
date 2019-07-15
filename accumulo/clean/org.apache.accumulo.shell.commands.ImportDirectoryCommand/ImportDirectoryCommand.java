import org.apache.accumulo.shell.commands.*;


import java.io.IOException;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.commons.cli.CommandLine;

public class ImportDirectoryCommand extends Command {

  @Override
  public String description() {
    return "bulk imports an entire directory of data files to the current"
        + " table. The boolean argument determines if accumulo sets the time.";
  }

  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws IOException, AccumuloException, AccumuloSecurityException, TableNotFoundException {
    shellState.checkTableState();

    String dir = cl.getArgs()[0];
    String failureDir = cl.getArgs()[1];
    final boolean setTime = Boolean.parseBoolean(cl.getArgs()[2]);

    shellState.getConnector().tableOperations().importDirectory(shellState.getTableName(), dir,
        failureDir, setTime);
    return 0;
  }

  @Override
  public int numArgs() {
    return 3;
  }

  @Override
  public String usage() {
    return getName() + " <directory> <failureDirectory> true|false";
  }

}
