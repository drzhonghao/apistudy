import org.apache.karaf.shell.impl.console.commands.help.*;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.ansi.SimpleAnsi;

public class CommandsHelpProvider implements HelpProvider {

    public String getHelp(Session session, String path) {
        if (path.indexOf('|') > 0) {
            if (path.startsWith("commands|")) {
                path = path.substring("commands|".length());
            } else {
                return null;
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        ps.println(SimpleAnsi.INTENSITY_BOLD + "COMMANDS" + SimpleAnsi.INTENSITY_NORMAL);
        ps.println("${command-list|" + path + "}");
        return baos.toString();
    }

    private Set<Command> getCommands(Session session, String path) {
        // TODO: this is not really clean

        List<Command> commands = session.getRegistry().getCommands();

        String subshell = (String) session.get(Session.SUBSHELL);
        String completionMode = (String) session.get(Session.COMPLETION_MODE);

        Set<Command> matchingCommands = new HashSet<>();
        for (Command command : commands) {

            String name = command.getScope() + ":" + command.getName();

            if (command != null && !name.startsWith(path)) {
                continue;
            }

            if (completionMode != null && completionMode.equalsIgnoreCase(Session.COMPLETION_MODE_SUBSHELL)) {
                // filter the help only for "global" commands
                if (subshell == null || subshell.trim().isEmpty()) {
                    if (!name.startsWith(Session.SCOPE_GLOBAL)) {
                        continue;
                    }
                }
            }

            if (completionMode != null && (completionMode.equalsIgnoreCase(Session.COMPLETION_MODE_SUBSHELL)
                    || completionMode.equalsIgnoreCase(Session.COMPLETION_MODE_FIRST))) {
                // filter the help only for commands local to the subshell
                if (!name.startsWith(subshell)) {
                    continue;
                }
            }

            matchingCommands.add(command);
        }
        return matchingCommands;
    }

}
