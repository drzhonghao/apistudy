import org.apache.accumulo.shell.commands.*;


import java.io.IOException;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.KerberosToken;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public class CreateUserCommand extends Command {
  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws AccumuloException, TableNotFoundException, AccumuloSecurityException,
      TableExistsException, IOException {
    final String user = cl.getArgs()[0];

    AuthenticationToken userToken = shellState.getToken();
    PasswordToken passwordToken;
    if (userToken instanceof KerberosToken) {
      passwordToken = new PasswordToken();
    } else {
      final String password = shellState.readMaskedLine("Enter new password for '" + user + "': ",
          '*');
      if (password == null) {
        shellState.getReader().println();
        return 0;
      } // user canceled
      String passwordConfirm = shellState
          .readMaskedLine("Please confirm new password for '" + user + "': ", '*');
      if (passwordConfirm == null) {
        shellState.getReader().println();
        return 0;
      } // user canceled

      if (!password.equals(passwordConfirm)) {
        throw new IllegalArgumentException("Passwords do not match");
      }
      passwordToken = new PasswordToken(password);
    }

    shellState.getConnector().securityOperations().createLocalUser(user, passwordToken);
    Shell.log.debug("Created user " + user);
    return 0;
  }

  @Override
  public String usage() {
    return getName() + " <username>";
  }

  @Override
  public String description() {
    return "creates a new user";
  }

  @Override
  public Options getOptions() {
    final Options o = new Options();
    return o;
  }

  @Override
  public int numArgs() {
    return 1;
  }
}
