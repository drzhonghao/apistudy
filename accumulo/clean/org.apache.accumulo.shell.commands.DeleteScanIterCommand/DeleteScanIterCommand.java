import org.apache.accumulo.shell.commands.*;


import java.util.Iterator;
import java.util.List;

import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;

public class DeleteScanIterCommand extends Command {
  private Option nameOpt, allOpt;

  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws Exception {
    Shell.log.warn("Deprecated, use " + new DeleteShellIterCommand().getName());
    final String tableName = OptUtil.getTableOpt(cl, shellState);

    if (cl.hasOption(allOpt.getOpt())) {
      final List<IteratorSetting> tableScanIterators = shellState.scanIteratorOptions
          .remove(tableName);
      if (tableScanIterators == null) {
        Shell.log.info("No scan iterators set on table " + tableName);
      } else {
        Shell.log.info("Removed the following scan iterators from table " + tableName + ":"
            + tableScanIterators);
      }
    } else if (cl.hasOption(nameOpt.getOpt())) {
      final String name = cl.getOptionValue(nameOpt.getOpt());
      final List<IteratorSetting> tableScanIterators = shellState.scanIteratorOptions
          .get(tableName);
      if (tableScanIterators != null) {
        boolean found = false;
        for (Iterator<IteratorSetting> iter = tableScanIterators.iterator(); iter.hasNext();) {
          if (iter.next().getName().equals(name)) {
            iter.remove();
            found = true;
            break;
          }
        }
        if (!found) {
          Shell.log.info("No iterator named " + name + " found for table " + tableName);
        } else {
          Shell.log.info("Removed scan iterator " + name + " from table " + tableName + " ("
              + shellState.scanIteratorOptions.get(tableName).size() + " left)");
          if (shellState.scanIteratorOptions.get(tableName).size() == 0) {
            shellState.scanIteratorOptions.remove(tableName);
          }
        }
      } else {
        Shell.log.info("No iterator named " + name + " found for table " + tableName);
      }
    }

    return 0;
  }

  @Override
  public String description() {
    return "deletes a table-specific scan iterator so it is no longer used"
        + " during this shell session";
  }

  @Override
  public Options getOptions() {
    final Options o = new Options();

    OptionGroup nameGroup = new OptionGroup();

    nameOpt = new Option("n", "name", true, "iterator to delete");
    nameOpt.setArgName("itername");

    allOpt = new Option("a", "all", false, "delete all scan iterators");
    allOpt.setArgName("all");

    nameGroup.addOption(nameOpt);
    nameGroup.addOption(allOpt);
    nameGroup.setRequired(true);
    o.addOptionGroup(nameGroup);
    o.addOption(OptUtil.tableOpt("table to delete scan iterators from"));

    return o;
  }

  @Override
  public int numArgs() {
    return 0;
  }
}
