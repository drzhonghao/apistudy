import org.apache.accumulo.shell.commands.*;


import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.commons.cli.CommandLine;

public class ExitCommand extends Command {
  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState) {
    shellState.setExit(true);
    return 0;
  }

  @Override
  public String description() {
    return "exits the shell";
  }

  @Override
  public int numArgs() {
    return 0;
  }
}
