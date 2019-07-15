import org.apache.karaf.shell.commands.impl.*;


import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.StringsCompleter;

/**
 * Command to change the completion mode while using the shell console.
 */
@Command(scope = "shell", name = "completion", description = "Display or change the completion mode on the current console session.")
@Service
public class CompletionAction implements Action {

    @Argument(index = 0, name = "mode", description = "The completion mode to set. The valid completion modes are: global, first, subshell.", required = false, multiValued = false)
    @Completion(value = StringsCompleter.class, values = { "global", "first", "subshell" })
    String mode;

    @Reference
    Session session;

    @Override
    public Object execute() throws Exception {
        if (mode == null) {
            System.out.println(session.get(Session.COMPLETION_MODE));
        } else if (!mode.equalsIgnoreCase("global") && !mode.equalsIgnoreCase("first") && !mode.equalsIgnoreCase("subshell")) {
            System.err.println("The completion mode is not correct. The valid modes are: global, first, subshell. See documentation for details.");
        } else {
            session.put(Session.COMPLETION_MODE, mode.toLowerCase());
        }
        return null;
    }

}
