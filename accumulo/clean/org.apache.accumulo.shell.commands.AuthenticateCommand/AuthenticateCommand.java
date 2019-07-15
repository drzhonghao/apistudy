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

public class AuthenticateCommand extends Command {
  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws AccumuloException, AccumuloSecurityException, IOException {
    final String user = cl.getArgs()[0];
    final String p = shellState.readMaskedLine("Enter current password for '" + user + "': ", '*');
    if (p == null) {
      shellState.getReader().println();
      return 0;
    } // user canceled
    final byte[] password = p.getBytes(UTF_8);
    final boolean valid = shellState.getConnector().securityOperations().authenticateUser(user,
        new PasswordToken(password));
    shellState.getReader().println((valid ? "V" : "Not v") + "alid");
    return 0;
  }

  @Override
  public String description() {
    return "verifies a user's credentials";
  }

  @Override
  public String usage() {
    return getName() + " <username>";
  }

  @Override
  public void registerCompletion(final Token root,
      final Map<Command.CompletionSet,Set<String>> completionSet) {
    registerCompletionForUsers(root, completionSet);
  }

  @Override
  public int numArgs() {
    return 1;
  }
}
