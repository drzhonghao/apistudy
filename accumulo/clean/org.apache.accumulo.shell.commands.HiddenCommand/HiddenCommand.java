import org.apache.accumulo.shell.commands.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.SecureRandom;
import java.util.Random;

import org.apache.accumulo.core.util.Base64;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.accumulo.shell.ShellCommandException;
import org.apache.accumulo.shell.ShellCommandException.ErrorCode;
import org.apache.commons.cli.CommandLine;

public class HiddenCommand extends Command {
  private static Random rand = new SecureRandom();

  @Override
  public String description() {
    return "The first rule of Accumulo is: \"You don't talk about Accumulo.\"";
  }

  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws Exception {
    if (rand.nextInt(10) == 0) {
      shellState.getReader().beep();
      shellState.getReader().println();
      shellState.getReader()
          .println(new String(Base64
              .decodeBase64(("ICAgICAgIC4tLS4KICAgICAgLyAvXCBcCiAgICAgKCAvLS1cICkKICAgICAuPl8gIF88"
                  + "LgogICAgLyB8ICd8ICcgXAogICAvICB8Xy58Xy4gIFwKICAvIC98ICAgICAgfFwgXAog"
                  + "fCB8IHwgfFwvfCB8IHwgfAogfF98IHwgfCAgfCB8IHxffAogICAgIC8gIF9fICBcCiAg"
                  + "ICAvICAvICBcICBcCiAgIC8gIC8gICAgXCAgXF8KIHwvICAvICAgICAgXCB8IHwKIHxf"
                  + "Xy8gICAgICAgIFx8X3wK").getBytes(UTF_8)),
              UTF_8));
    } else {
      throw new ShellCommandException(ErrorCode.UNRECOGNIZED_COMMAND, getName());
    }
    return 0;
  }

  @Override
  public int numArgs() {
    return Shell.NO_FIXED_ARG_LENGTH_CHECK;
  }

  @Override
  public String getName() {
    return "accvmvlo";
  }
}
