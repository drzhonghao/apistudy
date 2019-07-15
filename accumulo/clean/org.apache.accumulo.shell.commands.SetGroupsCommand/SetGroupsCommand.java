import org.apache.accumulo.shell.commands.OptUtil;
import org.apache.accumulo.shell.commands.*;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.hadoop.io.Text;

public class SetGroupsCommand extends Command {
  @Override
  public String description() {
    return "sets the locality groups for a given table (for binary or commas, use Java API)";
  }

  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws Exception {
    final String tableName = OptUtil.getTableOpt(cl, shellState);

    final HashMap<String,Set<Text>> groups = new HashMap<>();

    for (String arg : cl.getArgs()) {
      final String sa[] = arg.split("=", 2);
      if (sa.length < 2) {
        throw new IllegalArgumentException("Missing '='");
      }
      final String group = sa[0];
      final HashSet<Text> colFams = new HashSet<>();

      for (String family : sa[1].split(",")) {
        colFams.add(new Text(family.getBytes(Shell.CHARSET)));
      }

      groups.put(group, colFams);
    }

    shellState.getConnector().tableOperations().setLocalityGroups(tableName, groups);

    return 0;
  }

  @Override
  public int numArgs() {
    return Shell.NO_FIXED_ARG_LENGTH_CHECK;
  }

  @Override
  public String usage() {
    return getName() + " <group>=<col fam>{,<col fam>}{ <group>=<col fam>{,<col fam>}}";
  }

  @Override
  public Options getOptions() {
    final Options opts = new Options();
    opts.addOption(OptUtil.tableOpt("table to fetch locality groups for"));
    return opts;
  }

}
