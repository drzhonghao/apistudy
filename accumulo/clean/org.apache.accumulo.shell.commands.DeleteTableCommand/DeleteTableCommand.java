import org.apache.accumulo.shell.commands.*;


import java.util.Iterator;
import java.util.Set;

import org.apache.accumulo.core.client.impl.Namespaces;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.util.Pair;
import org.apache.accumulo.shell.Shell;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteTableCommand extends TableOperation {
  private static final Logger log = LoggerFactory.getLogger(DeleteTableCommand.class);

  private Option forceOpt;

  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws Exception {
    if (cl.hasOption(forceOpt.getOpt())) {
      super.force();
    } else {
      super.noForce();
    }
    return super.execute(fullCommand, cl, shellState);
  }

  @Override
  public String description() {
    return "deletes a table";
  }

  @Override
  protected void doTableOp(final Shell shellState, final String tableName) throws Exception {
    shellState.getConnector().tableOperations().delete(tableName);
    shellState.getReader().println("Table: [" + tableName + "] has been deleted.");

    if (shellState.getTableName().equals(tableName)) {
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
  protected void pruneTables(String pattern, Set<String> tables) {
    Iterator<String> tableNames = tables.iterator();
    while (tableNames.hasNext()) {
      String table = tableNames.next();
      Pair<String,String> qualifiedName = Tables.qualify(table);
      if (Namespaces.ACCUMULO_NAMESPACE.equals(qualifiedName.getFirst())) {
        log.trace("Removing table from deletion set: {}", table);
        tableNames.remove();
      }
    }
  }
}
