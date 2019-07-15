import org.apache.accumulo.shell.commands.*;


import java.io.IOException;

import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.accumulo.start.classloader.vfs.AccumuloVFSClassLoader;
import org.apache.accumulo.start.classloader.vfs.AccumuloVFSClassLoader.Printer;
import org.apache.commons.cli.CommandLine;

import jline.console.ConsoleReader;

public class ClasspathCommand extends Command {
  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState) {
    final ConsoleReader reader = shellState.getReader();
    AccumuloVFSClassLoader.printClassPath(new Printer() {
      @Override
      public void print(String s) {
        try {
          reader.println(s);
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      }
    });
    return 0;
  }

  @Override
  public String description() {
    return "lists the current files on the classpath";
  }

  @Override
  public int numArgs() {
    return 0;
  }
}
