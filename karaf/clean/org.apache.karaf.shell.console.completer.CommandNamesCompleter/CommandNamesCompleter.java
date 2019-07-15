import org.apache.karaf.shell.compat.CommandTracker;
import org.apache.karaf.shell.console.completer.*;


import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.console.CommandSessionHolder;
import org.apache.karaf.shell.console.Completer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceListener;

/**
 * Completes command names
 */
@Deprecated
public class CommandNamesCompleter implements Completer {

    public static final String COMMANDS = ".commands";

    private CommandSession session;
    private final Set<String> commands = new CopyOnWriteArraySet<>();

    public CommandNamesCompleter() {
        this(CommandSessionHolder.getSession());
    }

    public CommandNamesCompleter(CommandSession session) {
        this.session = session;

        try {
            new CommandTracker();
        } catch (Throwable t) {
            // Ignore in case we're not in OSGi
        }
    }


    public int complete(String buffer, int cursor, List<String> candidates) {
        if (session == null) {
            session = CommandSessionHolder.getSession();
        }
        checkData();
        int res = new StringsCompleter(commands).complete(buffer, cursor, candidates);
        Collections.sort(candidates);
        return res;
    }

    @SuppressWarnings("unchecked")
    protected void checkData() {
        if (commands.isEmpty()) {
            Set<String> names = new HashSet<>((Set<String>) session.get(COMMANDS));
            for (String name : names) {
                commands.add(name);
                if (name.indexOf(':') > 0) {
                    commands.add(name.substring(0, name.indexOf(':')));
                }
            }
        }
    }

    private class CommandTracker {
        public CommandTracker() throws Exception {
            BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
            if (context == null) {
                throw new IllegalStateException("Bundle is stopped");
            }
            ServiceListener listener = event -> commands.clear();
            context.addServiceListener(listener,
                    String.format("(&(%s=*)(%s=*))",
                            CommandProcessor.COMMAND_SCOPE,
                            CommandProcessor.COMMAND_FUNCTION));
        }
    }

}

