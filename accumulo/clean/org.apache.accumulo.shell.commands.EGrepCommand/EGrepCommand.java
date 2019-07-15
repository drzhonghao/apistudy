import org.apache.accumulo.shell.commands.*;


import java.io.IOException;

import org.apache.accumulo.core.client.BatchScanner;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.iterators.user.RegExFilter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class EGrepCommand extends GrepCommand {

  private Option matchSubstringOption;

  @Override
  protected void setUpIterator(final int prio, final String name, final String term,
      final BatchScanner scanner, CommandLine cl) throws IOException {
    if (prio < 0) {
      throw new IllegalArgumentException("Priority < 0 " + prio);
    }
    final IteratorSetting si = new IteratorSetting(prio, name, RegExFilter.class);
    RegExFilter.setRegexs(si, term, term, term, term, true,
        cl.hasOption(matchSubstringOption.getOpt()));
    scanner.addScanIterator(si);
  }

  @Override
  public String description() {
    return "searches each row, column family, column qualifier and value, in"
        + " parallel, on the server side (using a java Matcher, so put .* before"
        + " and after your term if you're not matching the whole element)";
  }

  @Override
  public String usage() {
    return getName() + " <regex>{ <regex>}";
  }

  @Override
  public Options getOptions() {
    final Options opts = super.getOptions();
    matchSubstringOption = new Option("g", "global", false,
        "forces the use of the find() expression matcher, causing substring"
            + " matches to return true");
    opts.addOption(matchSubstringOption);
    return opts;
  }
}
