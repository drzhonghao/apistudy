import org.apache.accumulo.shell.commands.*;


import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.apache.accumulo.core.util.BadArgumentException;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.accumulo.shell.Token;
import org.apache.commons.cli.CommandLine;

public class DebugCommand extends Command {
  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws IOException {
    if (cl.getArgs().length == 1) {
      if (cl.getArgs()[0].equalsIgnoreCase("on")) {
        Shell.setDebugging(true);
      } else if (cl.getArgs()[0].equalsIgnoreCase("off")) {
        Shell.setDebugging(false);
      } else {
        throw new BadArgumentException("Argument must be 'on' or 'off'", fullCommand,
            fullCommand.indexOf(cl.getArgs()[0]));
      }
    } else if (cl.getArgs().length == 0) {
      shellState.getReader().println(Shell.isDebuggingEnabled() ? "on" : "off");
    } else {
      shellState.printException(new IllegalArgumentException(
          "Expected 0 or 1 argument. There were " + cl.getArgs().length + "."));
      printHelp(shellState);
      return 1;
    }
    return 0;
  }

  @Override
  public String description() {
    return "turns debug logging on or off";
  }

  @Override
  public void registerCompletion(final Token root,
      final Map<Command.CompletionSet,Set<String>> special) {
    final Token debug_command = new Token(getName());
    debug_command.addSubcommand(Arrays.asList(new String[] {"on", "off"}));
    root.addSubcommand(debug_command);
  }

  @Override
  public String usage() {
    return getName() + " [ on | off ]";
  }

  @Override
  public int numArgs() {
    return Shell.NO_FIXED_ARG_LENGTH_CHECK;
  }
}
