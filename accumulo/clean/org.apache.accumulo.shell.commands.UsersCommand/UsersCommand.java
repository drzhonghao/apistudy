import org.apache.accumulo.shell.commands.*;


import java.io.IOException;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.commons.cli.CommandLine;

public class UsersCommand extends Command {
  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws AccumuloException, AccumuloSecurityException, IOException {
    for (String user : shellState.getConnector().securityOperations().listLocalUsers()) {
      shellState.getReader().println(user);
    }
    return 0;
  }

  @Override
  public String description() {
    return "displays a list of existing users";
  }

  @Override
  public int numArgs() {
    return 0;
  }
}
