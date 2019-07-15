import org.apache.accumulo.shell.commands.*;


import java.io.IOException;

import org.apache.accumulo.core.security.NamespacePermission;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.commons.cli.CommandLine;

public class NamespacePermissionsCommand extends Command {
  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws IOException {
    for (String p : NamespacePermission.printableValues()) {
      shellState.getReader().println(p);
    }
    return 0;
  }

  @Override
  public String description() {
    return "displays a list of valid namespace permissions";
  }

  @Override
  public int numArgs() {
    return 0;
  }
}
