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

public class DeleteShellIterCommand extends Command {
  private Option nameOpt, allOpt, profileOpt;

  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws Exception {

    String profile = cl.getOptionValue(profileOpt.getOpt());
    if (shellState.iteratorProfiles.containsKey(profile)) {
      if (cl.hasOption(allOpt.getOpt())) {
        shellState.iteratorProfiles.remove(profile);
        Shell.log.info("Removed profile " + profile);
      } else {
        List<IteratorSetting> iterSettings = shellState.iteratorProfiles.get(profile);
        String name = cl.getOptionValue(nameOpt.getOpt());
        boolean found = false;
        for (Iterator<IteratorSetting> iter = iterSettings.iterator(); iter.hasNext();) {
          if (iter.next().getName().equals(name)) {
            iter.remove();
            found = true;
            break;
          }
        }
        if (!found) {
          Shell.log.info("No iterator named " + name + " found");
        } else {
          Shell.log.info("Removed iterator " + name + " from profile " + profile + " ("
              + iterSettings.size() + " left)");
        }
      }

    } else {
      Shell.log.info("No profile named " + profile);
    }

    return 0;
  }

  @Override
  public String description() {
    return "deletes iterators profiles configured in this shell session";
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

    profileOpt = new Option("pn", "profile", true, "iterator profile name");
    profileOpt.setRequired(true);
    profileOpt.setArgName("profile");
    o.addOption(profileOpt);

    return o;
  }

  @Override
  public int numArgs() {
    return 0;
  }
}
