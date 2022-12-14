import org.apache.accumulo.shell.commands.*;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.accumulo.shell.ShellOptions;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.StringUtils;

public class GetAuthsCommand extends Command {
  private Option userOpt;

  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws AccumuloException, AccumuloSecurityException, IOException {
    final String user = cl.getOptionValue(userOpt.getOpt(), shellState.getConnector().whoami());
    // Sort authorizations
    Authorizations auths = shellState.getConnector().securityOperations()
        .getUserAuthorizations(user);
    List<String> set = sortAuthorizations(auths);
    shellState.getReader().println(StringUtils.join((Iterator) set, ','));
    return 0;
  }

  protected List<String> sortAuthorizations(Authorizations auths) {
    List<String> list = new ArrayList<>();
    for (byte[] auth : auths) {
      list.add(new String(auth));
    }
    Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
    return list;
  }

  @Override
  public String description() {
    return "displays the maximum scan authorizations for a user";
  }

  @Override
  public Options getOptions() {
    final Options o = new Options();
    userOpt = new Option(ShellOptions.userOption, "user", true, "user to operate on");
    userOpt.setArgName("user");
    o.addOption(userOpt);
    return o;
  }

  @Override
  public int numArgs() {
    return 0;
  }
}
