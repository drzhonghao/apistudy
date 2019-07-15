import org.apache.accumulo.shell.commands.*;


import java.io.IOException;

import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class AboutCommand extends Command {
  private Option verboseOption;

  @Override
  public String description() {
    return "displays information about this program";
  }

  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws IOException {
    shellState.printInfo();
    if (cl.hasOption(verboseOption.getOpt())) {
      shellState.printVerboseInfo();
    }
    return 0;
  }

  @Override
  public int numArgs() {
    return 0;
  }

  @Override
  public Options getOptions() {
    final Options opts = new Options();
    verboseOption = new Option("v", "verbose", false, "display detailed session information");
    opts.addOption(verboseOption);
    return opts;
  }
}
