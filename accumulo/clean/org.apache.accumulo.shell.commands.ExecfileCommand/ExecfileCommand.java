import org.apache.accumulo.shell.commands.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.util.Scanner;

import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class ExecfileCommand extends Command {
  private Option verboseOption;

  @Override
  public String description() {
    return "specifies a file containing accumulo commands to execute";
  }

  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws Exception {
    Scanner scanner = new Scanner(new File(cl.getArgs()[0]), UTF_8.name());
    try {
      while (scanner.hasNextLine()) {
        shellState.execCommand(scanner.nextLine(), true, cl.hasOption(verboseOption.getOpt()));
      }
    } finally {
      scanner.close();
    }
    return 0;
  }

  @Override
  public String usage() {
    return getName() + " <fileName>";
  }

  @Override
  public int numArgs() {
    return 1;
  }

  @Override
  public Options getOptions() {
    final Options opts = new Options();
    verboseOption = new Option("v", "verbose", false,
        "display command prompt as commands are executed");
    opts.addOption(verboseOption);
    return opts;
  }
}
