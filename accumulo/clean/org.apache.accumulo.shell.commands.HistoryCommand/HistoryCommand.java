import org.apache.accumulo.shell.commands.*;


import java.io.IOException;
import java.util.Iterator;

import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;

import jline.console.history.History.Entry;

public class HistoryCommand extends Command {
  private Option clearHist;
  private Option disablePaginationOpt;

  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws IOException {
    if (cl.hasOption(clearHist.getOpt())) {
      shellState.getReader().getHistory().clear();
    } else {
      Iterator<Entry> source = shellState.getReader().getHistory().entries();
      Iterator<String> historyIterator = Iterators.transform(source, new Function<Entry,String>() {
        @Override
        public String apply(Entry input) {
          return String.format("%d: %s", input.index() + 1, input.value());
        }
      });

      shellState.printLines(historyIterator, !cl.hasOption(disablePaginationOpt.getOpt()));
    }

    return 0;
  }

  @Override
  public String description() {
    return ("generates a list of commands previously executed");
  }

  @Override
  public int numArgs() {
    return 0;
  }

  @Override
  public Options getOptions() {
    final Options o = new Options();
    clearHist = new Option("c", "clear", false, "clear history file");
    o.addOption(clearHist);
    disablePaginationOpt = new Option("np", "no-pagination", false, "disable pagination of output");
    o.addOption(disablePaginationOpt);
    return o;
  }
}
