import org.apache.accumulo.shell.commands.*;


import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.accumulo.core.client.NamespaceNotFoundException;
import org.apache.accumulo.core.client.impl.Namespaces;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.accumulo.shell.Token;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class DeleteNamespaceCommand extends Command {
  private Option forceOpt;

  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws Exception {
    boolean force = false;
    boolean operate = true;
    if (cl.hasOption(forceOpt.getOpt())) {
      force = true;
    }
    String namespace = cl.getArgs()[0];

    if (!force) {
      shellState.getReader().flush();
      String line = shellState.getReader()
          .readLine(getName() + " { " + namespace + " } (yes|no)? ");
      operate = line != null && (line.equalsIgnoreCase("y") || line.equalsIgnoreCase("yes"));
    }
    if (operate) {
      doTableOp(shellState, namespace, force);
    }
    return 0;
  }

  @Override
  public String description() {
    return "deletes a namespace";
  }

  protected void doTableOp(final Shell shellState, final String namespace, boolean force)
      throws Exception {
    boolean resetContext = false;
    String currentTable = shellState.getTableName();
    if (!Namespaces.getNameToIdMap(shellState.getInstance()).containsKey(namespace)) {
      throw new NamespaceNotFoundException(null, namespace, null);
    }

    String namespaceId = Namespaces.getNamespaceId(shellState.getInstance(), namespace);
    List<String> tables = Namespaces.getTableNames(shellState.getInstance(), namespaceId);
    resetContext = tables.contains(currentTable);

    if (force)
      for (String table : shellState.getConnector().tableOperations().list())
        if (table.startsWith(namespace + "."))
          shellState.getConnector().tableOperations().delete(table);

    shellState.getConnector().namespaceOperations().delete(namespace);
    if (resetContext) {
      shellState.setTableName("");
    }
  }

  @Override
  public Options getOptions() {
    forceOpt = new Option("f", "force", false, "force deletion without prompting");
    final Options opts = super.getOptions();

    opts.addOption(forceOpt);
    return opts;
  }

  @Override
  public int numArgs() {
    return 1;
  }

  @Override
  public void registerCompletion(final Token root,
      final Map<Command.CompletionSet,Set<String>> special) {
    registerCompletionForNamespaces(root, special);
  }
}