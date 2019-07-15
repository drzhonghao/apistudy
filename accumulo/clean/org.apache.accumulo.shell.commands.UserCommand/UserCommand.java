import org.apache.accumulo.shell.commands.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.accumulo.shell.Token;
import org.apache.commons.cli.CommandLine;

public class UserCommand extends Command {
  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws AccumuloException, AccumuloSecurityException, IOException {
    // save old credentials and connection in case of failure
    String user = cl.getArgs()[0];
    byte[] pass;

    // We can't let the wrapping try around the execute method deal
    // with the exceptions because we have to do something if one
    // of these methods fails
    final String p = shellState.readMaskedLine("Enter password for user " + user + ": ", '*');
    if (p == null) {
      shellState.getReader().println();
      return 0;
    } // user canceled
    pass = p.getBytes(UTF_8);
    shellState.updateUser(user, new PasswordToken(pass));
    return 0;
  }

  @Override
  public String description() {
    return "switches to the specified user";
  }

  @Override
  public void registerCompletion(final Token root,
      final Map<Command.CompletionSet,Set<String>> special) {
    registerCompletionForUsers(root, special);
  }

  @Override
  public String usage() {
    return getName() + " <username>";
  }

  @Override
  public int numArgs() {
    return 1;
  }
}
