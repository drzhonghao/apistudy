import org.apache.karaf.shell.impl.console.*;


import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;

public class CommandNamesCompleter extends org.apache.karaf.shell.support.completers.CommandNamesCompleter {

    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        // TODO: optimize
        List<Command> list = session.getRegistry().getCommands();
        Set<String> names = new HashSet<>();
        for (Command command : list) {
            names.add(command.getScope() + ":" + command.getName());
            names.add(command.getName());
        }
        int res = new StringsCompleter(names).complete(session, commandLine, candidates);
        Collections.sort(candidates);
        return res;
    }

}
