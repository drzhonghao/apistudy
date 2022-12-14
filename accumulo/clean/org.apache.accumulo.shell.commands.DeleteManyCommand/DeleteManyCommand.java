import org.apache.accumulo.shell.commands.*;


import java.util.concurrent.TimeUnit;

import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.iterators.SortedKeyIterator;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.util.format.FormatterConfig;
import org.apache.accumulo.core.util.interpret.ScanInterpreter;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.format.DeleterFormatter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class DeleteManyCommand extends ScanCommand {
  private Option forceOpt;

  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws Exception {
    final String tableName = OptUtil.getTableOpt(cl, shellState);

    final ScanInterpreter interpeter = getInterpreter(cl, tableName, shellState);

    // handle first argument, if present, the authorizations list to
    // scan with
    final Authorizations auths = getAuths(cl, shellState);
    final Scanner scanner = shellState.getConnector().createScanner(tableName, auths);

    scanner.addScanIterator(
        new IteratorSetting(Integer.MAX_VALUE, "NOVALUE", SortedKeyIterator.class));

    // handle session-specific scan iterators
    addScanIterators(shellState, cl, scanner, tableName);

    // handle remaining optional arguments
    scanner.setRange(getRange(cl, interpeter));

    scanner.setTimeout(getTimeout(cl), TimeUnit.MILLISECONDS);

    // handle columns
    fetchColumns(cl, scanner, interpeter);

    // output / delete the records
    final BatchWriter writer = shellState.getConnector().createBatchWriter(tableName,
        new BatchWriterConfig().setTimeout(getTimeout(cl), TimeUnit.MILLISECONDS));
    FormatterConfig config = new FormatterConfig();
    config.setPrintTimestamps(cl.hasOption(timestampOpt.getOpt()));
    shellState.printLines(
        new DeleterFormatter(writer, scanner, config, shellState, cl.hasOption(forceOpt.getOpt())),
        false);

    return 0;
  }

  @Override
  public String description() {
    return "scans a table and deletes the resulting records";
  }

  @Override
  public Options getOptions() {
    forceOpt = new Option("f", "force", false, "force deletion without prompting");
    final Options opts = super.getOptions();
    opts.addOption(forceOpt);
    opts.addOption(OptUtil.tableOpt("table to delete entries from"));
    return opts;
  }

}
