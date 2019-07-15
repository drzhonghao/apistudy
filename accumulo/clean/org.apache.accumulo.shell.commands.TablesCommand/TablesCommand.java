import org.apache.accumulo.shell.commands.OptUtil;
import org.apache.accumulo.shell.commands.*;


import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.NamespaceNotFoundException;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.collections.MapUtils;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;

public class TablesCommand extends Command {
  static final String NAME_AND_ID_FORMAT = "%-20s => %9s%n";

  private Option tableIdOption;
  private Option sortByTableIdOption;
  private Option disablePaginationOpt;

  @SuppressWarnings("unchecked")
  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws AccumuloException, AccumuloSecurityException, IOException, NamespaceNotFoundException {

    final String namespace = cl.hasOption(OptUtil.namespaceOpt().getOpt())
        ? OptUtil.getNamespaceOpt(cl, shellState)
        : null;
    Map<String,String> tables = shellState.getConnector().tableOperations().tableIdMap();

    // filter only specified namespace
    tables = Maps.filterKeys(tables, new Predicate<String>() {
      @Override
      public boolean apply(String tableName) {
        return namespace == null || Tables.qualify(tableName).getFirst().equals(namespace);
      }
    });

    final boolean sortByTableId = cl.hasOption(sortByTableIdOption.getOpt());
    tables = new TreeMap<>((sortByTableId ? MapUtils.invertMap(tables) : tables));

    Iterator<String> it = Iterators.transform(tables.entrySet().iterator(),
        new Function<Entry<String,String>,String>() {
          @Override
          public String apply(Map.Entry<String,String> entry) {
            String tableName = String.valueOf(sortByTableId ? entry.getValue() : entry.getKey());
            String tableId = String.valueOf(sortByTableId ? entry.getKey() : entry.getValue());
            if (namespace != null)
              tableName = Tables.qualify(tableName).getSecond();
            if (cl.hasOption(tableIdOption.getOpt()))
              return String.format(NAME_AND_ID_FORMAT, tableName, tableId);
            else
              return tableName;
          }
        });

    shellState.printLines(it, !cl.hasOption(disablePaginationOpt.getOpt()));
    return 0;
  }

  @Override
  public String description() {
    return "displays a list of all existing tables";
  }

  @Override
  public Options getOptions() {
    final Options o = new Options();
    tableIdOption = new Option("l", "list-ids", false,
        "display internal table ids along with the table name");
    o.addOption(tableIdOption);
    sortByTableIdOption = new Option("s", "sort-ids", false, "with -l: sort output by table ids");
    o.addOption(sortByTableIdOption);
    disablePaginationOpt = new Option("np", "no-pagination", false, "disable pagination of output");
    o.addOption(disablePaginationOpt);
    o.addOption(OptUtil.namespaceOpt("name of namespace to list only its tables"));
    return o;
  }

  @Override
  public int numArgs() {
    return 0;
  }
}
