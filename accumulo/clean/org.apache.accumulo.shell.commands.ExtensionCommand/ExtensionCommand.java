import org.apache.accumulo.shell.commands.*;


import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;

import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.accumulo.shell.ShellExtension;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class ExtensionCommand extends Command {

  protected Option enable, disable, list;

  private ServiceLoader<ShellExtension> extensions = null;

  private Set<String> loadedHeaders = new HashSet<>();
  private Set<String> loadedCommands = new HashSet<>();
  private Set<String> loadedExtensions = new TreeSet<>();

  @Override
  public int execute(String fullCommand, CommandLine cl, Shell shellState) throws Exception {
    if (cl.hasOption(enable.getOpt())) {
      extensions = ServiceLoader.load(ShellExtension.class);
      for (ShellExtension se : extensions) {

        loadedExtensions.add(se.getExtensionName());
        String header = "-- " + se.getExtensionName() + " Extension Commands ---------";
        loadedHeaders.add(header);
        shellState.commandGrouping.put(header, se.getCommands());

        for (Command cmd : se.getCommands()) {
          String name = se.getExtensionName() + "::" + cmd.getName();
          loadedCommands.add(name);
          shellState.commandFactory.put(name, cmd);
        }
      }
    } else if (cl.hasOption(disable.getOpt())) {
      // Remove the headers
      for (String header : loadedHeaders) {
        shellState.commandGrouping.remove(header);
      }
      // remove the commands
      for (String name : loadedCommands) {
        shellState.commandFactory.remove(name);
      }
      // Reset state
      loadedExtensions.clear();
      extensions.reload();
    } else if (cl.hasOption(list.getOpt())) {
      shellState.printLines(loadedExtensions.iterator(), true);
    } else {
      printHelp(shellState);
    }
    return 0;
  }

  @Override
  public String description() {
    return "Enable, disable, or list shell extensions";
  }

  @Override
  public int numArgs() {
    return 0;
  }

  @Override
  public String getName() {
    return "extensions";
  }

  @Override
  public Options getOptions() {
    final Options o = new Options();
    enable = new Option("e", "enable", false, "enable shell extensions");
    disable = new Option("d", "disable", false, "disable shell extensions");
    list = new Option("l", "list", false, "list shell extensions");
    o.addOption(enable);
    o.addOption(disable);
    o.addOption(list);
    return o;
  }

}
