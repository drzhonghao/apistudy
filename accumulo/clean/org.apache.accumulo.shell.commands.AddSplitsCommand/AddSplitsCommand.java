import org.apache.accumulo.shell.commands.OptUtil;
import org.apache.accumulo.shell.commands.*;


import java.util.TreeSet;

import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.accumulo.shell.ShellUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.hadoop.io.Text;

public class AddSplitsCommand extends Command {
  private Option optSplitsFile, base64Opt;

  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws Exception {
    final String tableName = OptUtil.getTableOpt(cl, shellState);
    final boolean decode = cl.hasOption(base64Opt.getOpt());

    final TreeSet<Text> splits = new TreeSet<>();

    if (cl.hasOption(optSplitsFile.getOpt())) {
      splits.addAll(ShellUtil.scanFile(cl.getOptionValue(optSplitsFile.getOpt()), decode));
    } else {
      if (cl.getArgList().isEmpty()) {
        throw new MissingArgumentException("No split points specified");
      }
      for (String s : cl.getArgs()) {
        splits.add(new Text(s.getBytes(Shell.CHARSET)));
      }
    }

    if (!shellState.getConnector().tableOperations().exists(tableName)) {
      throw new TableNotFoundException(null, tableName, null);
    }
    shellState.getConnector().tableOperations().addSplits(tableName, splits);

    return 0;
  }

  @Override
  public String description() {
    return "adds split points to an existing table";
  }

  @Override
  public Options getOptions() {
    final Options o = new Options();

    optSplitsFile = new Option("sf", "splits-file", true,
        "file with a newline-separated list of rows to split the table with");
    optSplitsFile.setArgName("filename");

    base64Opt = new Option("b64", "base64encoded", false,
        "decode encoded split points (splits file only)");

    o.addOption(OptUtil.tableOpt("name of the table to add split points to"));
    o.addOption(optSplitsFile);
    o.addOption(base64Opt);
    return o;
  }

  @Override
  public String usage() {
    return getName() + " [<split>{ <split>} ]";
  }

  @Override
  public int numArgs() {
    return Shell.NO_FIXED_ARG_LENGTH_CHECK;
  }
}
