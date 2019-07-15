import org.apache.accumulo.shell.commands.*;


import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.accumulo.core.util.LocalityGroupUtil;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.hadoop.io.Text;

public class GetGroupsCommand extends Command {

  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws Exception {
    final String tableName = OptUtil.getTableOpt(cl, shellState);

    final Map<String,Set<Text>> groups = shellState.getConnector().tableOperations()
        .getLocalityGroups(tableName);

    for (Entry<String,Set<Text>> entry : groups.entrySet()) {
      shellState.getReader()
          .println(entry.getKey() + "=" + LocalityGroupUtil.encodeColumnFamilies(entry.getValue()));
    }
    return 0;
  }

  @Override
  public String description() {
    return "gets the locality groups for a given table";
  }

  @Override
  public int numArgs() {
    return 0;
  }

  @Override
  public Options getOptions() {
    final Options opts = new Options();
    opts.addOption(OptUtil.tableOpt("table to fetch locality groups from"));
    return opts;
  }
}
