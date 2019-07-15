import org.apache.karaf.shell.impl.console.commands.*;


import java.io.PrintStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.impl.console.commands.help.HelpCommand;

public class SubShellCommand extends TopLevelCommand {

    private final String name;
    private final AtomicInteger references = new AtomicInteger();

    public SubShellCommand(String name) {
        this.name = name;
    }

    public void increment() {
        references.incrementAndGet();
    }

    public int decrement() {
        return references.decrementAndGet();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return "Enter the subshell";
    }

    @Override
    protected void doExecute(Session session) throws Exception {
        session.put(Session.SUBSHELL, name);
        session.put(Session.SCOPE, name + ":" + session.get(Session.SCOPE));
    }

    @Override
    protected void printHelp(Session session, PrintStream out) {
        try {
            new HelpCommand(session.getFactory()).execute(session, Arrays.asList("shell|" + name));
        } catch (Exception e) {
            throw new RuntimeException("Unable to print subshell help", e);
        }
    }
}
