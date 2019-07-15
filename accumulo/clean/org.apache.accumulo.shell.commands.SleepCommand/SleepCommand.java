import org.apache.accumulo.shell.commands.*;


import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.commons.cli.CommandLine;

public class SleepCommand extends Command {

  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws Exception {
    final double secs = Double.parseDouble(cl.getArgs()[0]);
    Thread.sleep((long) (secs * 1000));
    return 0;
  }

  @Override
  public String description() {
    return "sleeps for the given number of seconds";
  }

  @Override
  public int numArgs() {
    return 1;
  }

  @Override
  public String usage() {
    return getName() + " <seconds>";
  }
}
