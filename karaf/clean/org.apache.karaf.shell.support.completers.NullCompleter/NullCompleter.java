import org.apache.karaf.shell.support.completers.*;


import java.util.List;

import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;

public class NullCompleter implements Completer {

    public static final NullCompleter INSTANCE = new NullCompleter();

    public int complete(final Session session, CommandLine commandLine, List<String> candidates) {
        return -1;
    }
}
