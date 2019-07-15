import org.apache.accumulo.shell.commands.*;


import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.commons.cli.CommandLine;

public class NoTableCommand extends Command {
  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws Exception {
    shellState.setTableName("");

    return 0;
  }

  @Override
  public String description() {
    return "returns to a tableless shell state";
  }

  @Override
  public int numArgs() {
    return 0;
  }
}
