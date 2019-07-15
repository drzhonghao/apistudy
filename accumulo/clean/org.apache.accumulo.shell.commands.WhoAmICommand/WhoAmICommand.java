import org.apache.accumulo.shell.commands.*;


import java.io.IOException;

import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.commons.cli.CommandLine;

public class WhoAmICommand extends Command {
  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws IOException {
    shellState.getReader().println(shellState.getConnector().whoami());
    return 0;
  }

  @Override
  public String description() {
    return "reports the current user name";
  }

  @Override
  public int numArgs() {
    return 0;
  }
}
