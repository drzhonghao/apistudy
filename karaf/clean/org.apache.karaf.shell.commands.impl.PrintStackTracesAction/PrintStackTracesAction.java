import org.apache.karaf.shell.commands.impl.*;


import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

/**
 * Command for showing the full tree of bundles that have been used to resolve
 * a given bundle.
 */
@Command(scope = "shell", name = "stack-traces-print", description = "Prints the full stack trace in the console when the execution of a command throws an exception.")
@Service
public class PrintStackTracesAction implements Action {

    @Argument(name = "print", description="Print stack traces or not", required = false, multiValued = false)
    boolean print = true;

    @Reference
    Session session;

    @Override
    public Object execute() throws Exception {
        System.out.println("Printing of stacktraces set to " + print);
        session.put(Session.PRINT_STACK_TRACES, print);
        return null;
    }

}
